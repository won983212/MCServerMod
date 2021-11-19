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

package com.won983212.servermod.legacy;

/**
 * Parses block input strings.
 */
public class DefaultBlockParser {
/*
    public BlockState parseFromInput(String input)
            throws InputParseException {
        String originalInput = input;
        input = input.replace(";", "|");
        Exception suppressed = null;
        try {
            BlockState modified = parseLogic(input);
            if (modified != null) {
                return modified;
            }
        } catch (Exception e) {
            suppressed = e;
        }
        try {
            return parseLogic(originalInput);
        } catch (Exception e) {
            if (suppressed != null) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }

    private static final String[] EMPTY_STRING_ARRAY = {};

    @SuppressWarnings("ConstantConditions")
    private String woolMapper(String string) {
        Block block = woolBlockMapper(string);
        return block != null ? block.getRegistryName().getPath() : string;
    }

    private Block woolBlockMapper(String string) {
        switch (string.toLowerCase(Locale.ROOT)) {
            case "white":
                return Blocks.WHITE_WOOL;
            case "black":
                return Blocks.BLACK_WOOL;
            case "blue":
                return Blocks.BLUE_WOOL;
            case "brown":
                return Blocks.BROWN_WOOL;
            case "cyan":
                return Blocks.CYAN_WOOL;
            case "gray":
            case "grey":
                return Blocks.GRAY_WOOL;
            case "green":
                return Blocks.GREEN_WOOL;
            case "light_blue":
            case "lightblue":
                return Blocks.LIGHT_BLUE_WOOL;
            case "light_gray":
            case "light_grey":
            case "lightgray":
            case "lightgrey":
                return Blocks.LIGHT_GRAY_WOOL;
            case "lime":
                return Blocks.LIME_WOOL;
            case "magenta":
                return Blocks.MAGENTA_WOOL;
            case "orange":
                return Blocks.ORANGE_WOOL;
            case "pink":
                return Blocks.PINK_WOOL;
            case "purple":
                return Blocks.PURPLE_WOOL;
            case "yellow":
                return Blocks.YELLOW_WOOL;
            case "red":
                return Blocks.RED_WOOL;
            default:
                return null;
        }
    }

    private static Map<Property<?>, Object> parseProperties(Block type, String[] stateProperties) throws InputParseException {
        Map<Property<?>, Object> blockStates = new HashMap<>();

        if (stateProperties.length > 0) { // Block data not yet detected
            // Parse the block data (optional)
            for (String parseableData : stateProperties) {
                try {
                    String[] parts = parseableData.split("=");
                    if (parts.length != 2) {
                        throw new InputParseException("Bad state format: " + parseableData);
                    }

                    Property<?> propertyKey = type.getStateDefinition().getProperty(parts[0]);
                    if (propertyKey == null) {
                        Logger.debug("Unknown property " + parts[0] + " for block " + type);
                        return Maps.newHashMap();
                    }
                    if (blockStates.containsKey(propertyKey)) {
                        throw new InputParseException("Duplicate property: " + parts[0]);
                    }

                    Object value;
                    try {
                        value = propertyKey.getValue(parts[1]);
                    } catch (IllegalArgumentException e) {
                        throw new InputParseException("Unknown value: " + parts[1] + ", " + propertyKey.getName());
                    }

                    blockStates.put(propertyKey, value);
                } catch (InputParseException e) {
                    throw e; // Pass-through
                } catch (Exception e) {
                    throw new InputParseException("Bad state format: " + parseableData);
                }
            }
        }

        return blockStates;
    }

    private BlockState parseLogic(String input) throws InputParseException {
        String[] blockAndExtraData = input.trim().split("\\|");
        if (blockAndExtraData.length == 0) {
            throw new InputParseException("Unknown block: " + input);
        }
        blockAndExtraData[0] = woolMapper(blockAndExtraData[0]);

        String typeString;
        String stateString = null;
        int stateStart = blockAndExtraData[0].indexOf('[');
        if (stateStart == -1) {
            typeString = blockAndExtraData[0];
        } else {
            typeString = blockAndExtraData[0].substring(0, stateStart);
            if (stateStart + 1 >= blockAndExtraData[0].length()) {
                throw new InputParseException("Hanging left bracket: " + stateStart);
            }
            int stateEnd = blockAndExtraData[0].lastIndexOf(']');
            if (stateEnd < 0) {
                throw new InputParseException("Missing right bracket: " + input);
            }
            stateString = blockAndExtraData[0].substring(stateStart + 1, blockAndExtraData[0].length() - 1);
        }

        if (typeString.isEmpty()) {
            throw new InputParseException("Bad state format: " + blockAndExtraData[0]);
        }

        String[] stateProperties = EMPTY_STRING_ARRAY;
        if (stateString != null) {
            stateProperties = stateString.split(",");
        }

        Block blockType = Registry.BLOCK.get(new ResourceLocation(typeString.toLowerCase(Locale.ROOT)));
        if (blockType == Blocks.AIR) {
            throw new InputParseException("Unknown block: " + input);
        }

        Map<Property<?>, Object> blockStates = new HashMap<>(parseProperties(blockType, stateProperties));
        BlockState state = blockType.defaultBlockState();
        for (Map.Entry<Property<?>, Object> blockState : blockStates.entrySet()) {
            Property<?> objProp = blockState.getKey();
            state = state.setValue(objProp, objProp.getValueClass().cast(blockState.getValue()));
        }

        return state;
    }*/
}
