package com.won983212.servermod.schematic.parser;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.parser.legacycompat.EntityNBTCompatibilityHandler;
import com.won983212.servermod.utility.RegistryHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.nbt.*;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.util.Constants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SpongeSchematicReader extends SchematicReader {

    private ForgeDataFixer fixer = null;
    private int schematicVersion = -1;
    private int dataVersion = -1;

    @Override
    protected Template parseSchematic(CompoundNBT schematic) throws IOException {
        int liveDataVersion = SharedConstants.getCurrentVersion().getWorldVersion();
        schematicVersion = checkTag(schematic, "Version", IntNBT.class).getAsInt();
        if (schematicVersion == 1) {
            dataVersion = 1631; // data version of 1.13.2. this is a relatively safe assumption unless someone imports a schematic from 1.12, e.g. sponge 7.1-
            fixer = new ForgeDataFixer(dataVersion);
            return readVersion1(schematic);
        } else if (schematicVersion == 2) {
            dataVersion = checkTag(schematic, "DataVersion", IntNBT.class).getAsInt();
            if (dataVersion < 0) {
                Logger.warn("Schematic has an unknown data version (" + dataVersion + "). Data may be incompatible.");
                // Do not DFU unknown data
                dataVersion = liveDataVersion;
            }
            if (dataVersion > liveDataVersion) {
                Logger.warn("Schematic was made in a newer Minecraft version ("
                        + dataVersion + " > " + liveDataVersion + "). Data may be incompatible.");
            } else if (dataVersion < liveDataVersion) {
                fixer = new ForgeDataFixer(dataVersion);
                Logger.debug("Schematic was made in an older Minecraft version ("
                        + dataVersion + " < " + liveDataVersion + "), will attempt DFU.");
            }

            Template template = readVersion1(schematic);
            return readVersion2(template, schematic);
        }
        throw new IOException("This schematic version is currently not supported");
    }

    private Template readVersion1(CompoundNBT schematicTag) throws IOException {
        int width = checkTag(schematicTag, "Width", ShortNBT.class).getAsInt();
        int height = checkTag(schematicTag, "Height", ShortNBT.class).getAsInt();
        int length = checkTag(schematicTag, "Length", ShortNBT.class).getAsInt();

        IntArrayNBT offsetTag = getTag(schematicTag, "Offset", IntArrayNBT.class);
        int[] offsetParts;
        if (offsetTag != null) {
            offsetParts = offsetTag.getAsIntArray();
            if (offsetParts.length != 3) {
                throw new IOException("Invalid offset specified in schematic.");
            }
        } else {
            offsetParts = new int[]{0, 0, 0};
        }

        IntNBT paletteMaxTag = getTag(schematicTag, "PaletteMax", IntNBT.class);
        CompoundNBT paletteObject = checkTag(schematicTag, "Palette", CompoundNBT.class);
        if (paletteMaxTag != null && paletteObject.size() != paletteMaxTag.getAsInt()) {
            throw new IOException("Block palette size does not match expected size.");
        }

        Map<Integer, BlockState> palette = new HashMap<>();
        for (String palettePart : paletteObject.getAllKeys()) {
            int id = checkTag(paletteObject, palettePart, IntNBT.class).getAsInt();
            if (fixer != null) {
                palettePart = fixer.fixUp(DataFixer.FixTypes.BLOCK_STATE, palettePart, dataVersion);
            }
            BlockState state;
            try {
                state = BlockStateArgument.block().parse(new StringReader(palettePart)).getState();
            } catch (CommandSyntaxException ignored) {
                Logger.warn("Invalid BlockState in palette: " + palettePart + ". Block will be replaced with air.");
                state = Blocks.AIR.defaultBlockState();
            }
            palette.put(id, state);
        }

        byte[] blocks = checkTag(schematicTag, "BlockData", ByteArrayNBT.class).getAsByteArray();

        Map<BlockPos, CompoundNBT> tileEntitiesMap = new HashMap<>();
        ListNBT tileEntities = getTag(schematicTag, "BlockEntities", ListNBT.class);
        if (tileEntities == null) {
            tileEntities = getTag(schematicTag, "TileEntities", ListNBT.class);
        }
        if (tileEntities != null) {
            for (INBT tileEntity : tileEntities) {
                CompoundNBT tag = (CompoundNBT) tileEntity;
                int[] pos = checkTag(tag, "Pos", IntArrayNBT.class).getAsIntArray();
                final BlockPos pt = new BlockPos(pos[0], pos[1], pos[2]);
                tag.put("x", IntNBT.valueOf(pt.getX()));
                tag.put("y", IntNBT.valueOf(pt.getY()));
                tag.put("z", IntNBT.valueOf(pt.getZ()));
                tag.put("id", tag.get("Id"));
                tag.remove("Id");
                tag.remove("Pos");
                if (fixer != null) {
                    // TODO fixit
                    fixer.fixUp(DataFixer.FixTypes.BLOCK_ENTITY, tag.copy(), dataVersion));
                    tag = ((CompoundNBT) AdventureNBTConverter.fromAdventure().getValue();
                }
                tileEntitiesMap.put(pt, tag);
            }
        }

        Template template = new Template();
        PriorityBlockList blockList = new PriorityBlockList();
        int index = 0;
        int i = 0;
        int value;
        int varintLength;
        while (i < blocks.length) {
            value = 0;
            varintLength = 0;

            while (true) {
                value |= (blocks[i] & 127) << (varintLength++ * 7);
                if (varintLength > 5) {
                    throw new IOException("VarInt too big (probably corrupted data)");
                }
                if ((blocks[i] & 128) != 128) {
                    i++;
                    break;
                }
                i++;
            }

            // index = (y * length * width) + (z * width) + x
            int y = index / (width * length);
            int z = (index % (width * length)) / width;
            int x = (index % (width * length)) % width;
            BlockState state = palette.get(value);
            BlockPos pt = new BlockPos(x, y, z);
            if (tileEntitiesMap.containsKey(pt)) {
                blockList.addBlock(pt, state, tileEntitiesMap.get(pt).copy());
            } else {
                blockList.addBlock(pt, state, null);
            }
            index++;
        }

        blockList.addNewPaletteTo(template);
        return template;
    }

    private Template readVersion2(Template version1, CompoundNBT schematicTag) throws IOException {
        if (schematicTag.contains("Entities")) {
            readEntities(version1, schematicTag);
        }
        return version1;
    }

    private void readEntities(Template template, CompoundNBT schematic) throws IOException {
        template.entityInfoList.clear();
        ListNBT entList = checkTag(schematic, "Entities", ListNBT.class);
        if (entList.isEmpty()) {
            return;
        }
        for (INBT et : entList) {
            if (!(et instanceof CompoundNBT)) {
                continue;
            }
            CompoundNBT entityTag = (CompoundNBT) et;
            String id = checkTag(entityTag, "Id", StringNBT.class).getAsString();
            entityTag.putString("id", id);
            entityTag.remove("Id");

            if (fixer != null) {
                // TODO fixit
                fixer.fixUp(DataFixer.FixTypes.ENTITY, entityTag, dataVersion);
                entityTag = (CompoundNBT) AdventureNBTConverter.fromAdventure();
            }

            ListNBT pos = entityTag.getList("Pos", Constants.NBT.TAG_DOUBLE);
            Vector3d posVector = new Vector3d(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2));
            BlockPos blockPos = new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
            Template.EntityInfo entityInfo = new Template.EntityInfo(posVector, blockPos, entityTag);
            template.entityInfoList.add(entityInfo);
        }
    }
}