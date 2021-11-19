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

import com.won983212.servermod.utility.RegistryHelper;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;

@SuppressWarnings("")
public class BedBlockCompatibilityHandler implements NBTCompatibilityHandler {

    @Override
    public boolean isAffectedBlock(BlockState block) {
        return block.getBlock() == Blocks.RED_BED;
    }

    @Override
    public BlockState updateNBT(BlockState block, CompoundNBT values) {
        INBT typeTag = values.get("color");
        if (typeTag instanceof IntNBT) {
            String bedType = convertBedType(((IntNBT) typeTag).getAsInt());
            if (bedType != null) {
                Block type = RegistryHelper.getBlockFromId("minecraft:" + bedType, null);
                if (type != null) {
                    BlockState state = type.defaultBlockState();
                    state = state.setValue(BedBlock.FACING, block.getValue(BedBlock.FACING));
                    state = state.setValue(BedBlock.OCCUPIED, false);
                    state = state.setValue(BedBlock.PART, block.getValue(BedBlock.PART));
                    values.remove("color");
                    return state;
                }
            }
        }
        return block;
    }

    private String convertBedType(int oldType) {
        String color;
        switch (oldType) {
            case 0:
                color = "white";
                break;
            case 1:
                color = "orange";
                break;
            case 2:
                color = "magenta";
                break;
            case 3:
                color = "light_blue";
                break;
            case 4:
                color = "yellow";
                break;
            case 5:
                color = "lime";
                break;
            case 6:
                color = "pink";
                break;
            case 7:
                color = "gray";
                break;
            case 8:
                color = "light_gray";
                break;
            case 9:
                color = "cyan";
                break;
            case 10:
                color = "purple";
                break;
            case 11:
                color = "blue";
                break;
            case 12:
                color = "brown";
                break;
            case 13:
                color = "green";
                break;
            case 14:
                color = "red";
                break;
            case 15:
                color = "black";
                break;
            default:
                return null;
        }
        return color + "_bed";
    }
}
