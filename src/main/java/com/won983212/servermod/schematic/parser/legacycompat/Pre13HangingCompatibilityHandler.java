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

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;

public class Pre13HangingCompatibilityHandler implements EntityNBTCompatibilityHandler {

    @Override
    public boolean isAffectedEntity(String id, CompoundNBT tag) {
        if (!id.startsWith("minecraft:")) {
            return false;
        }
        boolean hasLegacyDirection = tag.contains("Dir") || tag.contains("Direction");
        boolean hasFacing = tag.contains("Facing");
        return hasLegacyDirection || hasFacing;
    }

    @Override
    public CompoundNBT updateNBT(String id, CompoundNBT tag) {
        boolean hasLegacyDir = tag.contains("Dir");
        boolean hasLegacyDirection = tag.contains("Direction");
        boolean hasPre113Facing = tag.contains("Facing");
        Direction newDirection;
        if (hasLegacyDir) {
            newDirection = fromPre13Hanging(fromLegacyHanging((byte) tag.getInt("Dir")));
        } else if (hasLegacyDirection) {
            newDirection = fromPre13Hanging(tag.getInt("Direction"));
        } else if (hasPre113Facing) {
            newDirection = fromPre13Hanging(tag.getInt("Facing"));
        } else {
            return tag;
        }
        byte hangingByte = (byte) newDirection.ordinal();
        CompoundNBT result = new CompoundNBT();
        result.putByte("Facing", hangingByte);
        return result;
    }

    private static Direction fromPre13Hanging(int i) {
        switch (i) {
            case 0:
                return Direction.SOUTH;
            case 1:
                return Direction.WEST;
            case 2:
                return Direction.NORTH;
            case 3:
                return Direction.EAST;
            default:
                return Direction.NORTH;
        }
    }

    private static int fromLegacyHanging(byte i) {
        switch (i) {
            case 0:
                return 2;
            case 1:
                return 1;
            case 2:
                return 0;
            default:
                return 3;
        }
    }
}
