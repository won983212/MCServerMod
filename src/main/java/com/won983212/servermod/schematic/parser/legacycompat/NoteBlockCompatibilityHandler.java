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

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.state.IntegerProperty;

import java.util.HashSet;

public class NoteBlockCompatibilityHandler implements NBTCompatibilityHandler {
    private static final IntegerProperty NoteProperty;

    static {
        IntegerProperty temp;
        try {
            temp = NoteBlock.NOTE;
        } catch (NullPointerException | IllegalArgumentException | ClassCastException e) {
            temp = null;
        }
        NoteProperty = temp;
    }

    @Override
    public boolean isAffectedBlock(BlockState block) {
        return NoteProperty != null && block.getBlock() == Blocks.NOTE_BLOCK;
    }

    @Override
    public BlockState updateNBT(BlockState block, CompoundNBT values) {
        // note that instrument was not stored (in state or nbt) previously.
        // it will be updated to the block below when it gets set into the world for the first time
        INBT noteTag = values.get("note");
        if (noteTag instanceof ByteNBT) {
            byte note = ((ByteNBT) noteTag).getAsByte();
            for (String key : new HashSet<>(values.getAllKeys())) {
                values.remove(key);
            }
            return block.setValue(NoteProperty, (int) note);
        }
        return block;
    }
}
