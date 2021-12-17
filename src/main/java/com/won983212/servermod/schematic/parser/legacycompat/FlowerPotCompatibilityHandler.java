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

package com.won983212.servermod.schematic.parser.legacycompat;

import com.won983212.servermod.LegacyMapper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.StringNBT;

import java.util.HashSet;

public class FlowerPotCompatibilityHandler implements NBTCompatibilityHandler {
    @Override
    public boolean isAffectedBlock(BlockState block) {
        return block.getBlock() == Blocks.FLOWER_POT;
    }

    @Override
    public BlockState updateNBT(BlockState block, CompoundNBT values) {
        INBT item = values.get("Item");
        if (item instanceof StringNBT) {
            String id = item.getAsString();
            if (id.isEmpty()) {
                return Blocks.FLOWER_POT.defaultBlockState();
            }
            int data = 0;
            INBT dataTag = values.get("Data");
            if (dataTag instanceof IntNBT) {
                data = ((IntNBT) dataTag).getAsInt();
            }
            BlockState newState = convertLegacyBlockType(id, data);
            if (newState != null) {
                for (String key : new HashSet<>(values.getAllKeys())) {
                    values.remove(key);
                }
                return newState;
            }
        }
        return block;
    }

    private BlockState convertLegacyBlockType(String id, int data) {
        int newId = 0;
        switch (id) {
            case "minecraft:red_flower":
                newId = 38; // now poppy
                break;
            case "minecraft:yellow_flower":
                newId = 37; // now dandelion
                break;
            case "minecraft:sapling":
                newId = 6; // oak_sapling
                break;
            case "minecraft:deadbush":
            case "minecraft:tallgrass":
                newId = 31; // dead_bush with fern and grass (not 32!)
                break;
            default:
                break;
        }
        String plantedName = null;
        if (newId == 0 && id.startsWith("minecraft:")) {
            plantedName = id.substring(10);
        } else {
            BlockState plantedWithData = LegacyMapper.getBlockFromLegacy(newId, data);
            if (plantedWithData != null) {
                plantedName = plantedWithData.getBlock().getRegistryName().getPath();
            }
        }
        if (plantedName != null) {
            Block potAndPlanted = NBTCompatibilityHandler.getBlockFromId("minecraft:potted_" + plantedName, null);
            if (potAndPlanted != null) {
                return potAndPlanted.defaultBlockState();
            }
        }
        return null;
    }
}
