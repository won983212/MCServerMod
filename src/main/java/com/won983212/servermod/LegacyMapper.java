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

package com.won983212.servermod;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.won983212.servermod.schematic.container.SchematicContainer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Direction;

import javax.annotation.Nullable;

public final class LegacyMapper {
    private static final Object2IntOpenHashMap<String> NAME_TO_ID_MAP = new Object2IntOpenHashMap<>();
    private static final Object2IntOpenHashMap<String> OLD_NAME_TO_ID_MAP = new Object2IntOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<String> IDMETA_TO_OVERRIDDEN_MAP = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<BlockState> IDMETA_TO_STATE_MAP = new Int2ObjectOpenHashMap<>();
    private static final Object2IntOpenHashMap<BlockState> STATE_TO_IDMETA_MAP = new Object2IntOpenHashMap<>();


    public static void registerBlockState(int idMeta, String newState, String... oldState) {
        try {
            CompoundNBT newStateTag = parseTag(newState);
            String name = newStateTag.getString("Name");

            String overridden = IDMETA_TO_OVERRIDDEN_MAP.get(idMeta);
            if (overridden != null) {
                name = overridden;
                newStateTag.putString("Name", overridden);
            }

            if (!IDMETA_TO_STATE_MAP.containsKey(idMeta)) {
                BlockState state = NBTUtil.readBlockState(newStateTag);
                if (!name.equals("minecraft:air") && state == SchematicContainer.AIR_BLOCK_STATE) {
                    Logger.warn("Failed to read blockstate: " + idMeta + " = " + newState);
                    return;
                }

                IDMETA_TO_STATE_MAP.putIfAbsent(idMeta, state);
                STATE_TO_IDMETA_MAP.putIfAbsent(state, idMeta);
            }

            NAME_TO_ID_MAP.putIfAbsent(name, idMeta);
            // Logger.debug("Appended to Legacy map: " + (idMeta >> 4) + ":" + (idMeta & 15) + " = " + name);

            if (oldState.length > 0) {
                name = parseTag(oldState[0]).getString("Name");
                OLD_NAME_TO_ID_MAP.putIfAbsent(name, idMeta);
            }
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private static CompoundNBT parseTag(String nbtJson) throws CommandSyntaxException {
        return JsonToNBT.parseTag(nbtJson.replace('\'', '"'));
    }

    public static int getOldNameToBlockId(String oldBlockname) {
        return OLD_NAME_TO_ID_MAP.getOrDefault(oldBlockname, -1);
    }

    public static int getNameToBlockId(String blockname) {
        return NAME_TO_ID_MAP.getOrDefault(blockname, -1);
    }

    @Nullable
    public static BlockState getBlockFromLegacy(int legacyId, int data) {
        return IDMETA_TO_STATE_MAP.getOrDefault((legacyId << 4) | data, null);
    }

    public static int getLegacyFromBlock(BlockState blockState) {
        return STATE_TO_IDMETA_MAP.getOrDefault(blockState, -1);
    }

    static {
        BlockState air = SchematicContainer.AIR_BLOCK_STATE;
        STATE_TO_IDMETA_MAP.put(air, 0);
        IDMETA_TO_STATE_MAP.put(0, air);

        int idOldLog = (17 << 4) | 12;
        int idNewLog = (162 << 4) | 12;
        IDMETA_TO_STATE_MAP.put(idOldLog, Blocks.OAK_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        IDMETA_TO_STATE_MAP.put(idOldLog | 1, Blocks.SPRUCE_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        IDMETA_TO_STATE_MAP.put(idOldLog | 2, Blocks.BIRCH_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        IDMETA_TO_STATE_MAP.put(idOldLog | 3, Blocks.JUNGLE_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        IDMETA_TO_STATE_MAP.put(idNewLog, Blocks.ACACIA_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        IDMETA_TO_STATE_MAP.put(idNewLog | 1, Blocks.DARK_OAK_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));

        IDMETA_TO_STATE_MAP.put(2224, Blocks.COBBLESTONE_WALL.defaultBlockState());
        IDMETA_TO_STATE_MAP.put(2225, Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState());

        IDMETA_TO_OVERRIDDEN_MAP.put(832, "minecraft:spawner");

        for (int i = 0; i < 16; i++) {
            IDMETA_TO_OVERRIDDEN_MAP.put(1008 | i, "minecraft:oak_sign");
        }

        for (int i = 0; i < 4; i++) {
            IDMETA_TO_OVERRIDDEN_MAP.put(1090 + i, "minecraft:oak_wall_sign");
        }

        IDMETA_TO_OVERRIDDEN_MAP.put(1441, "minecraft:nether_portal");
        IDMETA_TO_OVERRIDDEN_MAP.put(1442, "minecraft:nether_portal");

        IDMETA_TO_OVERRIDDEN_MAP.put(1648, "minecraft:melon");

        IDMETA_TO_OVERRIDDEN_MAP.put(2304, "minecraft:skeleton_skull");
        IDMETA_TO_OVERRIDDEN_MAP.put(2305, "minecraft:skeleton_skull");
        IDMETA_TO_OVERRIDDEN_MAP.put(2306, "minecraft:skeleton_wall_skull");
        IDMETA_TO_OVERRIDDEN_MAP.put(2307, "minecraft:skeleton_wall_skull");
        IDMETA_TO_OVERRIDDEN_MAP.put(2308, "minecraft:skeleton_wall_skull");
        IDMETA_TO_OVERRIDDEN_MAP.put(2309, "minecraft:skeleton_wall_skull");
        IDMETA_TO_OVERRIDDEN_MAP.put(2312, "minecraft:skeleton_skull");
        IDMETA_TO_OVERRIDDEN_MAP.put(2313, "minecraft:skeleton_skull");
        IDMETA_TO_OVERRIDDEN_MAP.put(2314, "minecraft:skeleton_wall_skull");
        IDMETA_TO_OVERRIDDEN_MAP.put(2315, "minecraft:skeleton_wall_skull");
        IDMETA_TO_OVERRIDDEN_MAP.put(2316, "minecraft:skeleton_wall_skull");
        IDMETA_TO_OVERRIDDEN_MAP.put(2317, "minecraft:skeleton_wall_skull");

        IDMETA_TO_OVERRIDDEN_MAP.put(3664, "minecraft:shulker_box");
        IDMETA_TO_OVERRIDDEN_MAP.put(3665, "minecraft:shulker_box");
        IDMETA_TO_OVERRIDDEN_MAP.put(3666, "minecraft:shulker_box");
        IDMETA_TO_OVERRIDDEN_MAP.put(3667, "minecraft:shulker_box");
        IDMETA_TO_OVERRIDDEN_MAP.put(3668, "minecraft:shulker_box");
        IDMETA_TO_OVERRIDDEN_MAP.put(3669, "minecraft:shulker_box");
    }
}
