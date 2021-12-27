package com.won983212.servermod.schematic.parser;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.container.SchematicContainer;
import net.minecraft.block.BlockState;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.nbt.*;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class SpongeSchematicReader extends AbstractSchematicReader {

    private ForgeDataFixer fixer = null;
    private int dataVersion = -1;


    public SpongeSchematicReader(File file) {
        super(file);
    }

    @Override
    protected BlockPos parseSize() {
        int width = checkTag(schematic, "Width", ShortNBT.class).getAsInt();
        int height = checkTag(schematic, "Height", ShortNBT.class).getAsInt();
        int length = checkTag(schematic, "Length", ShortNBT.class).getAsInt();
        return new BlockPos(width, height, length);
    }

    @Override
    public boolean parsePartial() {
        int liveDataVersion = SharedConstants.getCurrentVersion().getWorldVersion();
        int schematicVersion = checkTag(schematic, "Version", IntNBT.class).getAsInt();
        if (schematicVersion == 1) {
            dataVersion = 1631; // data version of 1.13.2. this is a relatively safe assumption unless someone imports a schematic from 1.12, e.g. sponge 7.1-
            fixer = new ForgeDataFixer(dataVersion);
            result = readVersion1();
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
            result = readVersion2(readVersion1());
        } else {
            throw new IllegalArgumentException("This schematic version is currently not supported");
        }
        notifyProgress("읽는 중...", 1);
        return false;
    }

    private SchematicContainer readVersion1() {
        notifyProgress("Metadata 읽는 중...", 0);

        BlockPos size = parseSize();
        int width = size.getX();
        int height = size.getY();
        int length = size.getZ();

        IntArrayNBT offsetTag = getTag(schematic, "Offset", IntArrayNBT.class);
        int[] offsetParts;
        if (offsetTag != null) {
            offsetParts = offsetTag.getAsIntArray();
            if (offsetParts.length != 3) {
                throw new IllegalArgumentException("Invalid offset specified in schematic.");
            }
        }

        IntNBT paletteMaxTag = getTag(schematic, "PaletteMax", IntNBT.class);
        CompoundNBT paletteObject = checkTag(schematic, "Palette", CompoundNBT.class);
        if (paletteMaxTag != null && paletteObject.size() != paletteMaxTag.getAsInt()) {
            throw new IllegalArgumentException("Block palette size does not match expected size.");
        }

        long current = 0;
        Map<Integer, BlockState> palette = new HashMap<>();
        Set<String> palettes = paletteObject.getAllKeys();
        for (String palettePart : palettes) {
            int id = checkTag(paletteObject, palettePart, IntNBT.class).getAsInt();
            if (fixer != null) {
                palettePart = fixer.fixUp(ForgeDataFixer.FixTypes.BLOCK_STATE, palettePart, dataVersion);
            }
            BlockState state;
            try {
                state = BlockStateArgument.block().parse(new StringReader(palettePart)).getState();
            } catch (CommandSyntaxException ignored) {
                Logger.warn("Invalid BlockState in palette: " + palettePart + ". Block will be replaced with air.");
                state = SchematicContainer.AIR_BLOCK_STATE;
            }
            notifyProgress("Palette 읽는 중...", 0.1 * (current++) / palettes.size());
            palette.put(id, state);
        }

        byte[] blocks = checkTag(schematic, "BlockData", ByteArrayNBT.class).getAsByteArray();

        Map<BlockPos, CompoundNBT> tileEntitiesMap = new HashMap<>();
        ListNBT tileEntities = getTag(schematic, "BlockEntities", ListNBT.class);
        if (tileEntities == null) {
            tileEntities = getTag(schematic, "TileEntities", ListNBT.class);
        }

        current = 0;
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
                    tag = fixer.fixUp(ForgeDataFixer.FixTypes.BLOCK_ENTITY, tag.copy(), dataVersion);
                }
                tileEntitiesMap.put(pt, tag);
                notifyProgress("TileEntity 읽는 중...", 0.1 + 0.3 * (current++) / tileEntities.size());
            }
        }

        SchematicContainer schem = new SchematicContainer(new BlockPos(width, height, length));

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
                    throw new IllegalArgumentException("VarInt too big (probably corrupted data)");
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
                schem.setBlock(pt, state, tileEntitiesMap.get(pt).copy());
            } else {
                schem.setBlock(pt, state, null);
            }
            notifyProgress("Block 읽는 중...", 0.4 + 0.59 * (index++) / blocks.length);
        }

        return schem;
    }

    private SchematicContainer readVersion2(SchematicContainer version1) {
        if (schematic.contains("Entities")) {
            notifyProgress("Entity 읽는 중...", 0.99);
            readEntities(version1);
        }
        return version1;
    }

    private void readEntities(SchematicContainer schem) {
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
                entityTag = fixer.fixUp(ForgeDataFixer.FixTypes.ENTITY, entityTag, dataVersion);
            }

            schem.addEntity(entityTag);
        }
    }
}