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

import net.minecraft.block.*;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;

public class SkullBlockCompatibilityHandler implements NBTCompatibilityHandler {
    @Override
    public boolean isAffectedBlock(BlockState block) {
        return block.getBlock() == Blocks.SKELETON_SKULL
                || block.getBlock() == Blocks.SKELETON_WALL_SKULL;
    }

    @Override
    public BlockState updateNBT(BlockState block, CompoundNBT values) {
        boolean isWall = block.getBlock() == Blocks.SKELETON_WALL_SKULL;
        INBT typeTag = values.get("SkullType");
        if (typeTag instanceof ByteNBT) {
            String skullType = convertSkullType(((ByteNBT) typeTag).getAsByte(), isWall);
            if (skullType != null) {
                Block type = NBTCompatibilityHandler.getBlockFromId("minecraft:" + skullType, null);
                if (type != null) {
                    BlockState state = type.defaultBlockState();
                    if (isWall) {
                        state = state.setValue(WallSkullBlock.FACING, block.getValue(WallSkullBlock.FACING));
                    } else {
                        INBT rotTag = values.get("Rot");
                        if (rotTag instanceof ByteNBT) {
                            state = state.setValue(SkullBlock.ROTATION, ((ByteNBT) rotTag).getAsInt());
                        }
                    }
                    values.remove("SkullType");
                    values.remove("Rot");
                    return state;
                }
            }
        }
        return block;
    }

    private String convertSkullType(Byte oldType, boolean isWall) {
        switch (oldType) {
            case 0:
                return isWall ? "skeleton_wall_skull" : "skeleton_skull";
            case 1:
                return isWall ? "wither_skeleton_wall_skull" : "wither_skeleton_skull";
            case 2:
                return isWall ? "zombie_wall_head" : "zombie_head";
            case 3:
                return isWall ? "player_wall_head" : "player_head";
            case 4:
                return isWall ? "creeper_wall_head" : "creeper_head";
            case 5:
                return isWall ? "dragon_wall_head" : "dragon_head";
            default:
                return null;
        }
    }
}
