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
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.ListNBT;

public class BannerBlockCompatibilityHandler implements NBTCompatibilityHandler {
    @Override
    public boolean isAffectedBlock(BlockState block) {
        return block.getBlock() == Blocks.WHITE_BANNER
                || block.getBlock() == Blocks.WHITE_WALL_BANNER;
    }

    @Override
    public BlockState updateNBT(BlockState block, CompoundNBT values) {
        INBT typeTag = values.get("Base");
        if (typeTag instanceof IntNBT) {
            boolean isWall = block.getBlock() == Blocks.WHITE_WALL_BANNER;
            String bannerType = convertBannerType(((IntNBT) typeTag).getAsInt(), isWall);
            if (bannerType != null) {
                Block type = NBTCompatibilityHandler.getBlockFromId("minecraft:" + bannerType, null);
                if (type != null) {
                    BlockState state = type.defaultBlockState();
                    if (isWall) {
                        state = state.setValue(WallBannerBlock.FACING, block.getValue(WallBannerBlock.FACING));
                    } else {
                        state = state.setValue(BannerBlock.ROTATION, block.getValue(BannerBlock.ROTATION));
                    }
                    values.remove("Base");

                    INBT patternsTag = values.get("Patterns");
                    if (patternsTag instanceof ListNBT) {
                        ListNBT tempList = new ListNBT();
                        for (INBT pattern : (ListNBT) patternsTag) {
                            if (pattern instanceof CompoundNBT) {
                                CompoundNBT patternMap = (CompoundNBT) pattern;
                                INBT colorTag = patternMap.get("Color");
                                CompoundNBT patternTag = patternMap.copy();
                                if (colorTag instanceof IntNBT) {
                                    patternTag.putInt("Color", 15 - ((IntNBT) colorTag).getAsInt());
                                }
                                tempList.add(patternTag);
                            } else {
                                tempList.add(pattern);
                            }
                        }
                        values.put("Patterns", tempList);
                    }
                    return state;
                }
            }
        }
        return block;
    }

    private static String convertBannerType(int oldType, boolean isWall) {
        String color;
        switch (oldType) {
            case 0:
                color = "black";
                break;
            case 1:
                color = "red";
                break;
            case 2:
                color = "green";
                break;
            case 3:
                color = "brown";
                break;
            case 4:
                color = "blue";
                break;
            case 5:
                color = "purple";
                break;
            case 6:
                color = "cyan";
                break;
            case 7:
                color = "light_gray";
                break;
            case 8:
                color = "gray";
                break;
            case 9:
                color = "pink";
                break;
            case 10:
                color = "lime";
                break;
            case 11:
                color = "yellow";
                break;
            case 12:
                color = "light_blue";
                break;
            case 13:
                color = "magenta";
                break;
            case 14:
                color = "orange";
                break;
            case 15:
                color = "white";
                break;
            default:
                return null;
        }
        return color + (isWall ? "_wall_banner" : "_banner");
    }
}
