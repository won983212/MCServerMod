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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.won983212.servermod.client.ResourceUtil;
import com.won983212.servermod.utility.RegistryHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class LegacyMapper {
    private static final ResourceLocation LEGACY_MAP_FILE = ResourceUtil.getResource("legacy.json");
    private static LegacyMapper INSTANCE;

    private final Map<String, BlockState> stringToBlockMap = new HashMap<>();
    private final Multimap<BlockState, String> blockToStringMap = HashMultimap.create();
    private final Map<String, Item> stringToItemMap = new HashMap<>();
    private final Multimap<Item, String> itemToStringMap = HashMultimap.create();

    /**
     * Create a new instance.
     */
    private LegacyMapper() {
        try {
            loadFromResource();
        } catch (Throwable e) {
            Logger.warn("Failed to load the built-in legacy id registry. ");
            e.printStackTrace();
        }
    }

    /**
     * Attempt to load the data from file.
     *
     * @throws IOException thrown on I/O error
     */
    private void loadFromResource() throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Vector3d.class, new VectorAdapter());
        Gson gson = gsonBuilder.disableHtmlEscaping().create();

        InputStream is = Minecraft.getInstance().getResourceManager().getResource(LEGACY_MAP_FILE).getInputStream();
        String data = IOUtils.toString(is, StandardCharsets.UTF_8);
        LegacyDataFile dataFile = gson.fromJson(data, new TypeToken<LegacyDataFile>() {}.getType());

        for (Map.Entry<String, String> blockEntry : dataFile.blocks.entrySet()) {
            String id = blockEntry.getKey();
            String value = blockEntry.getValue();
            BlockState state = null;

            // if it's still null, the fixer was unavailable or failed
            try {
                state = BlockStateArgument.block().parse(new StringReader(value)).getState();
            } catch (CommandSyntaxException ignored) {
            }

            // if it's still null, both fixer and default failed
            if (state == null) {
                Logger.warn("Unknown block: " + value);
            } else {
                // it's not null so one of them succeeded, now use it
                blockToStringMap.put(state, id);
                stringToBlockMap.put(id, state);
            }
        }

        for (Map.Entry<String, String> itemEntry : dataFile.items.entrySet()) {
            String id = itemEntry.getKey();
            String value = itemEntry.getValue();
            Item type = RegistryHelper.getItemFromId(value, null);
            if (type != null) {
                itemToStringMap.put(type, id);
                stringToItemMap.put(id, type);
            } else {
                Logger.warn("Unknown item ID: " + value);
            }
        }
    }

    @Nullable
    public Item getItemFromLegacy(int legacyId) {
        return getItemFromLegacy(legacyId, 0);
    }

    @Nullable
    public Item getItemFromLegacy(int legacyId, int data) {
        return stringToItemMap.get(legacyId + ":" + data);
    }

    @Nullable
    public int[] getLegacyFromItem(Item itemType) {
        if (itemToStringMap.containsKey(itemType)) {
            String value = itemToStringMap.get(itemType).stream().findFirst().get();
            return Arrays.stream(value.split(":")).mapToInt(Integer::parseInt).toArray();
        } else {
            return null;
        }
    }

    @Nullable
    public BlockState getBlockFromLegacy(int legacyId) {
        return getBlockFromLegacy(legacyId, 0);
    }

    @Nullable
    public BlockState getBlockFromLegacy(int legacyId, int data) {
        return stringToBlockMap.get(legacyId + ":" + data);
    }

    @Nullable
    public int[] getLegacyFromBlock(BlockState blockState) {
        if (blockToStringMap.containsKey(blockState)) {
            String value = blockToStringMap.get(blockState).stream().findFirst().get();
            return Arrays.stream(value.split(":")).mapToInt(Integer::parseInt).toArray();
        } else {
            return null;
        }
    }

    public static LegacyMapper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LegacyMapper();
        }
        return INSTANCE;
    }

    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
    private static class LegacyDataFile {
        private Map<String, String> blocks;
        private Map<String, String> items;
    }

    public static class VectorAdapter implements JsonDeserializer<Vector3d> {
        @Override
        public Vector3d deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonArray jsonArray = json.getAsJsonArray();
            if (jsonArray.size() != 3) {
                throw new JsonParseException("Expected array of 3 length for Vector3");
            }

            double x = jsonArray.get(0).getAsDouble();
            double y = jsonArray.get(1).getAsDouble();
            double z = jsonArray.get(2).getAsDouble();

            return new Vector3d(x, y, z);
        }
    }

}
