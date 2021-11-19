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

import com.google.gson.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.StringNBT;

public class SignCompatibilityHandler implements NBTCompatibilityHandler {

    @Override
    public boolean isAffectedBlock(BlockState block) {
        return block.getBlock() == Blocks.OAK_SIGN || block.getBlock() == Blocks.OAK_WALL_SIGN;
    }

    @Override
    public BlockState updateNBT(BlockState block, CompoundNBT values) {
        for (int i = 0; i < 4; ++i) {
            String key = "Text" + (i + 1);
            INBT value = values.get(key);
            if (value instanceof StringNBT) {
                String storedString = value.getAsString();
                JsonElement jsonElement = null;
                if (storedString.startsWith("{")) {
                    try {
                        jsonElement = new JsonParser().parse(storedString);
                    } catch (JsonSyntaxException ex) {
                        // ignore: jsonElement will be null in the next check
                    }
                }
                if (jsonElement == null) {
                    jsonElement = new JsonPrimitive(storedString);
                }
                if (jsonElement.isJsonObject()) {
                    continue;
                }

                if (jsonElement.isJsonNull()) {
                    jsonElement = new JsonPrimitive("");
                }

                JsonObject jsonTextObject = new JsonObject();
                jsonTextObject.add("text", jsonElement);
                values.put("Text" + (i + 1), StringNBT.valueOf(jsonTextObject.toString()));
            }
        }
        return block;
    }
}
