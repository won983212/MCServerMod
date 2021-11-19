/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.won983212.servermod.schematic.parser;

import com.google.common.collect.ImmutableList;
import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.parser.legacycompat.*;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.*;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.common.util.Constants;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Reads schematic files that are compatible with MCEdit and other editors.
 */
public class MCEditSchematicReader extends SchematicReader {

    private static final ImmutableList<NBTCompatibilityHandler> COMPATIBILITY_HANDLERS
            = ImmutableList.of(
            new SignCompatibilityHandler(),
            new FlowerPotCompatibilityHandler(),
            new NoteBlockCompatibilityHandler(),
            new SkullBlockCompatibilityHandler(),
            new BannerBlockCompatibilityHandler(),
            new BedBlockCompatibilityHandler()
    );
    private static final ImmutableList<EntityNBTCompatibilityHandler> ENTITY_COMPATIBILITY_HANDLERS
            = ImmutableList.of(
            new Pre13HangingCompatibilityHandler()
    );

    private CompoundNBT read(File file) throws IOException {
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(file))))) {
            return CompressedStreamTools.read(stream, new NBTSizeTracker(0x20000000L));
        } catch (IOException e) {
            throw new IOException("Failed to read schematic", e);
        }
    }

    public Template parseSchematic(File file) throws IOException {
        Template t = new Template();
        CompoundNBT nbt = read(file);
        nbt = convertToVanillaNBT(nbt);
        t.load(nbt);
        return t;
    }

    private CompoundNBT convertToVanillaNBT(CompoundNBT nbt) throws IOException {
        // Schematic tag
        if (!nbt.contains("Schematic")) {
            throw new IOException("Tag 'Schematic' does not exist or is not first");
        }

        CompoundNBT vanillaTag = new CompoundNBT();
        CompoundNBT schematicTag = nbt.getCompound("Schematic");

        // Check
        if (!schematicTag.contains("Blocks")) {
            throw new IOException("Schematic file is missing a 'Blocks' tag");
        }

        // Check type of Schematic
        String materials = schematicTag.getString("Materials");
        if (!materials.equals("Alpha")) {
            throw new IOException("Schematic file is not an Alpha schematic");
        }

        // ====================================================================
        // Metadata
        // ====================================================================

        // Get information
        short width = checkTag(schematicTag, "Width", ShortNBT.class).getAsShort();
        short height = checkTag(schematicTag, "Height", ShortNBT.class).getAsShort();
        short length = checkTag(schematicTag, "Length", ShortNBT.class).getAsShort();
        vanillaTag.putIntArray("size", new int[]{width, height, length});

        // ====================================================================
        // Blocks
        // ====================================================================

        // Get blocks
        byte[] blockId = checkTag(schematicTag, "Blocks", ByteArrayNBT.class).getAsByteArray();
        byte[] blockData = checkTag(schematicTag, "Data", ByteArrayNBT.class).getAsByteArray();
        byte[] addId = new byte[0];
        short[] blocks = new short[blockId.length]; // Have to later combine IDs

        // We support 4096 block IDs using the same method as vanilla Minecraft, where
        // the highest 4 bits are stored in a separate byte array.
        if (schematicTag.contains("AddBlocks")) {
            addId = checkTag(schematicTag, "AddBlocks", ByteArrayNBT.class).getAsByteArray();
        }

        // Combine the AddBlocks data with the first 8-bit block ID
        for (int index = 0; index < blockId.length; index++) {
            if ((index >> 1) >= addId.length) { // No corresponding AddBlocks index
                blocks[index] = (short) (blockId[index] & 0xFF);
            } else {
                if ((index & 1) == 0) {
                    blocks[index] = (short) (((addId[index >> 1] & 0x0F) << 8) + (blockId[index] & 0xFF));
                } else {
                    blocks[index] = (short) (((addId[index >> 1] & 0xF0) << 4) + (blockId[index] & 0xFF));
                }
            }
        }

        // Need to pull out tile entities
        final ListNBT tileEntityTag = getTag(schematicTag, "TileEntities", ListNBT.class);
        List<INBT> tileEntities = tileEntityTag == null ? new ArrayList<>() : tileEntityTag;
        Map<BlockPos, CompoundNBT> tileEntitiesMap = new HashMap<>();
        Map<BlockPos, BlockState> blockStates = new HashMap<>();

        for (INBT tag : tileEntities) {
            if (!(tag instanceof CompoundNBT)) {
                continue;
            }
            CompoundNBT t = (CompoundNBT) tag;
            String id = t.getString("id");
            t.put("id", StringNBT.valueOf(convertBlockEntityId(id)));
            int x = t.getInt("x");
            int y = t.getInt("y");
            int z = t.getInt("z");
            int index = y * width * length + z * width + x;

            BlockState block = getBlockState(blocks[index], blockData[index]);
            BlockState newBlock = block;
            if (newBlock != null) {
                for (NBTCompatibilityHandler handler : COMPATIBILITY_HANDLERS) {
                    if (handler.isAffectedBlock(newBlock)) {
                        newBlock = handler.updateNBT(block, t);
                        if (newBlock == null || t.isEmpty()) {
                            break;
                        }
                    }
                }
            }
            if (t.isEmpty()) {
                t = null;
            }

            BlockPos vec = new BlockPos(x, y, z);
            if (t != null) {
                tileEntitiesMap.put(vec, t);
            }
            blockStates.put(vec, newBlock);
        }

        VanillaPalette palette = new VanillaPalette();
        ListNBT blocksTag = new ListNBT();
        Set<Integer> unknownBlocks = new HashSet<>();
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                for (int z = 0; z < length; ++z) {
                    int index = y * width * length + z * width + x;
                    BlockPos pt = new BlockPos(x, y, z);
                    BlockState state = blockStates.computeIfAbsent(pt, p -> getBlockState(blocks[index], blockData[index]));

                    if (state != null) {
                        CompoundNBT blockTag = new CompoundNBT();
                        blockTag.putIntArray("pos", new int[]{x, y, z});
                        blockTag.putInt("state", palette.idFor(state));
                        if (tileEntitiesMap.containsKey(pt)) {
                            blockTag.put("nbt", tileEntitiesMap.get(pt).copy());
                        }
                        blocksTag.add(blockTag);
                    } else {
                        short block = blocks[index];
                        byte data = blockData[index];
                        int combined = block << 8 | data;
                        if (unknownBlocks.add(combined)) {
                            Logger.warn("Unknown block when loading schematic: "
                                    + block + ":" + data + ". This is most likely a bad schematic.");
                        }
                    }
                }
            }
        }
        vanillaTag.put("palette", palette.asListNBT());
        vanillaTag.put("blocks", blocksTag);

        // ====================================================================
        // Entities
        // ====================================================================

        ListNBT entityList = getTag(schematicTag, "Entities", ListNBT.class);
        ListNBT vanillaEntities = new ListNBT();
        if (entityList != null) {
            for (INBT tag : entityList) {
                if (tag instanceof CompoundNBT) {
                    CompoundNBT compound = (CompoundNBT) tag;
                    String id = convertEntityId(compound.getString("id"));
                    ListNBT pos = compound.getList("Pos", Constants.NBT.TAG_DOUBLE);
                    int[] blockPos = new int[]{pos.getInt(0), pos.getInt(1), pos.getInt(2)};
                    if (!id.isEmpty()) {
                        CompoundNBT entityTag = new CompoundNBT();
                        CompoundNBT entityNBTTag = compound.copy();
                        entityTag.put("pos", pos);
                        entityTag.putIntArray("blockPos", blockPos);
                        for (EntityNBTCompatibilityHandler compatibilityHandler : ENTITY_COMPATIBILITY_HANDLERS) {
                            if (compatibilityHandler.isAffectedEntity(id, entityNBTTag)) {
                                entityNBTTag = compatibilityHandler.updateNBT(id, entityNBTTag);
                            }
                        }
                        entityTag.put("nbt", entityNBTTag);
                        vanillaEntities.add(entityTag);
                    }
                }
            }
        }
        vanillaTag.put("entities", vanillaEntities);
        vanillaTag.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());

        return vanillaTag;
    }
}
