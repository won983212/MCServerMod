package com.won983212.servermod.schematic.parser;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.container.SchematicContainer;
import com.won983212.servermod.task.StagedTaskProcessor;
import net.minecraft.block.BlockState;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.nbt.*;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class SpongeSchematicReader extends AbstractSchematicReader {

    private enum ParseStage {
        METADATA, TILES, BLOCKS, ENTITIES
    }

    private ForgeDataFixer fixer = null;
    private int dataVersion = -1;

    private int width;
    private int length;
    private Map<Integer, BlockState> palette;
    private Map<BlockPos, CompoundNBT> tileEntitiesMap;

    private ListNBT tileEntities;
    private int blockByteOffset;

    private final StagedTaskProcessor<ParseStage> stageProcessor;
    private int current = 0;


    public SpongeSchematicReader(File file) {
        super(file);
        this.stageProcessor = new StagedTaskProcessor<>(ParseStage.class)
                .stage(ParseStage.METADATA)
                .onNextStage(() -> current = 0)
                .onComplete(() -> notifyProgress("읽는 중...", 1));
        this.stageProcessor.addStageHandler(ParseStage.METADATA, this::readMetadata);
        this.stageProcessor.addStageHandler(ParseStage.TILES, this::readTileEntities);
        this.stageProcessor.addStageHandler(ParseStage.BLOCKS, this::readBlocks);
        this.stageProcessor.addStageHandler(ParseStage.ENTITIES, this::readEntities);
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
        return stageProcessor.tick();
    }

    private void readPalette() {
        IntNBT paletteMaxTag = getTag(schematic, "PaletteMax", IntNBT.class);
        CompoundNBT paletteObject = checkTag(schematic, "Palette", CompoundNBT.class);
        if (paletteMaxTag != null && paletteObject.size() != paletteMaxTag.getAsInt()) {
            throw new IllegalArgumentException("Block palette size does not match expected size.");
        }

        palette = new HashMap<>();
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
    }

    private boolean readMetadata() {
        int liveDataVersion = SharedConstants.getCurrentVersion().getWorldVersion();
        int schematicVersion = checkTag(schematic, "Version", IntNBT.class).getAsInt();
        if (schematicVersion == 1) {
            dataVersion = 1631; // data version of 1.13.2. this is a relatively safe assumption unless someone imports a schematic from 1.12, e.g. sponge 7.1-
            fixer = new ForgeDataFixer(dataVersion);
            stageProcessor.finalStage(ParseStage.BLOCKS);
        } else if (schematicVersion == 2) {
            dataVersion = checkTag(schematic, "DataVersion", IntNBT.class).getAsInt();
            if (dataVersion < 0) {
                Logger.warn("Schematic has an unknown data version (" + dataVersion + "). Data may be incompatible.");
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
            stageProcessor.finalStage(ParseStage.ENTITIES);
        } else {
            throw new IllegalArgumentException("This schematic version is currently not supported");
        }

        notifyProgress("사이즈 읽는 중...", 0);
        BlockPos size = parseSize();
        this.width = size.getX();
        this.length = size.getZ();
        this.result = new SchematicContainer(size);

        readPalette();
        return false;
    }

    private boolean readTileEntities() {
        if (current == 0) {
            tileEntitiesMap = new HashMap<>();
            tileEntities = getTag(schematic, "BlockEntities", ListNBT.class);
            if (tileEntities == null) {
                tileEntities = getTag(schematic, "TileEntities", ListNBT.class);
            }
            if (tileEntities == null) {
                return false;
            }
        }

        int tileSize = tileEntities.size();
        for (int i = 0; i < batchCount && current < tileSize; ++i, ++current) {
            CompoundNBT tag = (CompoundNBT) tileEntities.get(current);
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
        }

        notifyProgress("TileEntity 읽는 중...", 0.1 + 0.3 * current / tileSize);
        return current < tileSize;
    }

    private boolean readBlocks() {
        if (current == 0) {
            blockByteOffset = 0;
        }

        int value;
        int varIntLength;

        byte[] blocks = checkTag(schematic, "BlockData", ByteArrayNBT.class).getAsByteArray();
        for (int i = 0; i < batchCount && blockByteOffset < blocks.length; ++i, ++current) {
            value = 0;
            varIntLength = 0;

            while (true) {
                value |= (blocks[blockByteOffset] & 127) << (varIntLength++ * 7);
                if (varIntLength > 5) {
                    throw new IllegalArgumentException("VarInt too big (probably corrupted data)");
                }
                if ((blocks[blockByteOffset] & 128) != 128) {
                    blockByteOffset++;
                    break;
                }
                blockByteOffset++;
            }

            int y = current / (width * length);
            int z = (current % (width * length)) / width;
            int x = (current % (width * length)) % width;
            BlockState state = palette.get(value);
            BlockPos pt = new BlockPos(x, y, z);
            if (tileEntitiesMap.containsKey(pt)) {
                result.setBlock(pt, state, tileEntitiesMap.get(pt).copy());
            } else {
                result.setBlock(pt, state, null);
            }
        }

        notifyProgress("Block 읽는 중...", 0.4 + 0.59 * current / blocks.length);
        return blockByteOffset < blocks.length;
    }

    private boolean readEntities() {
        if (!schematic.contains("Entities")) {
            return false;
        }

        ListNBT entList = checkTag(schematic, "Entities", ListNBT.class);
        notifyProgress("Entity 읽는 중...", 0.99);
        if (entList.isEmpty()) {
            return false;
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

            result.addEntity(entityTag);
        }
        return false;
    }
}