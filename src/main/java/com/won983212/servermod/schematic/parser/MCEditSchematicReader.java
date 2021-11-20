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
import com.google.common.collect.Lists;
import com.won983212.servermod.LegacyMapper;
import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.parser.legacycompat.*;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.*;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.EmptyBlockReader;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.common.util.Constants;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

    private Template template;
    private CompoundNBT nbt;

    public Template parseSchematic(File file) throws IOException {
        template = new Template();
        read(file);
        parseSchematicToTemplate();
        return template;
    }

    private void read(File file) throws IOException {
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(file))))) {
            nbt = CompressedStreamTools.read(stream, new NBTSizeTracker(0x20000000L));
        } catch (IOException e) {
            throw new IOException("Failed to read schematic", e);
        }
    }

    private void parseSchematicToTemplate() throws IOException {
        // Check
        if (!nbt.contains("Blocks")) {
            throw new IOException("Schematic file is missing a 'Blocks' tag");
        }

        // Check type of Schematic
        String materials = nbt.getString("Materials");
        if (!materials.equals("Alpha")) {
            throw new IOException("Schematic file is not an Alpha schematic");
        }

        // ====================================================================
        // Metadata
        // ====================================================================

        // Get information
        short width = checkTag(nbt, "Width", ShortNBT.class).getAsShort();
        short height = checkTag(nbt, "Height", ShortNBT.class).getAsShort();
        short length = checkTag(nbt, "Length", ShortNBT.class).getAsShort();
        template.size = new BlockPos(width, height, length);

        // ====================================================================
        // Blocks
        // ====================================================================

        // Get blocks
        byte[] blockId = checkTag(nbt, "Blocks", ByteArrayNBT.class).getAsByteArray();
        byte[] blockData = checkTag(nbt, "Data", ByteArrayNBT.class).getAsByteArray();
        byte[] addId = new byte[0];
        short[] blocks = new short[blockId.length]; // Have to later combine IDs

        // We support 4096 block IDs using the same method as vanilla Minecraft, where
        // the highest 4 bits are stored in a separate byte array.
        if (nbt.contains("AddBlocks")) {
            addId = checkTag(nbt, "AddBlocks", ByteArrayNBT.class).getAsByteArray();
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
        final ListNBT tileEntityTag = getTag(nbt, "TileEntities", ListNBT.class);
        List<INBT> tileEntities = tileEntityTag == null ? new ArrayList<>() : tileEntityTag;
        Map<BlockPos, CompoundNBT> tileEntitiesMap = new HashMap<>();
        Map<BlockPos, BlockState> blockStates = new HashMap<>();
        LegacyMapper legacyMapper = LegacyMapper.getInstance();

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

            BlockState block = legacyMapper.getBlockFromLegacy(blocks[index], blockData[index]);
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

        List<Template.BlockInfo> plainBlocks = Lists.newArrayList();
        List<Template.BlockInfo> tileBlocks = Lists.newArrayList();
        List<Template.BlockInfo> specialBlocks = Lists.newArrayList();
        Set<Integer> unknownBlocks = new HashSet<>();

        template.palettes.clear();
        long total = (long) width * height * length;
        long current = 0;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                for (int z = 0; z < length; ++z) {
                    int index = y * width * length + z * width + x;
                    BlockPos pt = new BlockPos(x, y, z);
                    BlockState state = blockStates.get(pt);

                    if (state == null) {
                        state = legacyMapper.getBlockFromLegacy(blocks[index], blockData[index]);
                        blockStates.put(pt, state);
                    }

                    if (state == null){
                        short block = blocks[index];
                        byte data = blockData[index];
                        int combined = block << 8 | data;
                        if (unknownBlocks.add(combined)) {
                            Logger.warn("Unknown block when loading schematic: "
                                    + block + ":" + data + ". This is most likely a bad schematic.");
                        }
                        continue;
                    }

                    CompoundNBT blockNBT = null;
                    if (tileEntitiesMap.containsKey(pt)) {
                        blockNBT = tileEntitiesMap.get(pt).copy();
                    }

                    Template.BlockInfo tempBlock = new Template.BlockInfo(pt, state, blockNBT);
                    if (tempBlock.nbt != null) {
                        tileBlocks.add(tempBlock);
                    } else if (!tempBlock.state.getBlock().hasDynamicShape() && tempBlock.state.isCollisionShapeFullBlock(EmptyBlockReader.INSTANCE, BlockPos.ZERO)) {
                        plainBlocks.add(tempBlock);
                    } else {
                        specialBlocks.add(tempBlock);
                    }

                    // TODO Progress System
                    if (current++ % (total / 20) == 0){
                        System.out.println("Current Progress: " + (current * 100.0d / total) + "%");
                    }
                }
            }
        }

        List<Template.BlockInfo> list = Lists.newArrayList();
        list.addAll(plainBlocks);
        list.addAll(specialBlocks);
        list.addAll(tileBlocks);

        Template.Palette palette;
        try {
            Constructor<Template.Palette> con = Template.Palette.class.getDeclaredConstructor(List.class);
            con.setAccessible(true);
            palette = con.newInstance(list);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IOException("Can't create palette", e);
        }
        template.palettes.add(palette);

        // ====================================================================
        // Entities
        // ====================================================================

        template.entityInfoList.clear();
        ListNBT entityList = getTag(nbt, "Entities", ListNBT.class);
        if (entityList != null) {
            for (INBT tag : entityList) {
                if (tag instanceof CompoundNBT) {
                    CompoundNBT compound = (CompoundNBT) tag;
                    String id = convertEntityId(compound.getString("id"));
                    ListNBT pos = compound.getList("Pos", Constants.NBT.TAG_DOUBLE);
                    if (!id.isEmpty()) {
                        CompoundNBT entityNBTTag = compound.copy();
                        for (EntityNBTCompatibilityHandler compatibilityHandler : ENTITY_COMPATIBILITY_HANDLERS) {
                            if (compatibilityHandler.isAffectedEntity(id, entityNBTTag)) {
                                entityNBTTag = compatibilityHandler.updateNBT(id, entityNBTTag);
                            }
                        }
                        Vector3d posVector = new Vector3d(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2));
                        BlockPos blockPos = new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
                        Template.EntityInfo entityInfo = new Template.EntityInfo(posVector, blockPos, entityNBTTag);
                        template.entityInfoList.add(entityInfo);
                    }
                }
            }
        }
    }
}
