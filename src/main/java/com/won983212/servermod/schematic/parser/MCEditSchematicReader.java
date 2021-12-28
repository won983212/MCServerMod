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

package com.won983212.servermod.schematic.parser;

import com.google.common.collect.ImmutableList;
import com.won983212.servermod.LegacyMapper;
import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.container.SchematicContainer;
import com.won983212.servermod.schematic.parser.legacycompat.*;
import com.won983212.servermod.task.StagedTaskProcessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import java.io.File;
import java.util.*;

class MCEditSchematicReader extends AbstractSchematicReader {

    private enum ParseStage {
        METADATA, BLOCKS, TILES, STORE_BLOCKS, STORE_ENTITIES
    }

    private static final ImmutableList<NBTCompatibilityHandler> COMPATIBILITY_HANDLERS
            = ImmutableList.of(
            new SignCompatibilityHandler(),
            new FlowerPotCompatibilityHandler(),
            new NoteBlockCompatibilityHandler(),
            new SkullBlockCompatibilityHandler(),
            new BannerBlockCompatibilityHandler(),
            new BedBlockCompatibilityHandler(),
            new DoorBlockCompatibilityHandler(),
            new CommandBlockCompatibilityHandler()
    );

    private static final ImmutableList<EntityNBTCompatibilityHandler> ENTITY_COMPATIBILITY_HANDLERS
            = ImmutableList.of(
            new Pre13HangingCompatibilityHandler()
    );

    private byte[] blockId;
    private byte[] addId;
    private List<INBT> tileEntities;
    private Set<Integer> unknownBlocks;

    private BlockState[] palette;
    private short[] blocks;
    private byte[] blockData;
    private int width, height, length;
    private Map<BlockPos, CompoundNBT> tileEntitiesMap;
    private Map<BlockPos, BlockState> blockStates;

    private final StagedTaskProcessor<ParseStage> stageProcessor;
    private int current = 0;


    public MCEditSchematicReader(File file) {
        super(file);
        this.stageProcessor = new StagedTaskProcessor<>(ParseStage.class)
                .stage(ParseStage.METADATA)
                .nextStageEvent(() -> current = 0)
                .completeEvent(() -> notifyProgress("읽는 중...", 1));
        this.stageProcessor.addStageHandler(ParseStage.METADATA, this::parseMetadata);
        this.stageProcessor.addStageHandler(ParseStage.BLOCKS, this::parseBlocks);
        this.stageProcessor.addStageHandler(ParseStage.TILES, this::parseTileEntities);
        this.stageProcessor.addStageHandler(ParseStage.STORE_BLOCKS, this::addBlocks);
        this.stageProcessor.addStageHandler(ParseStage.STORE_ENTITIES, this::addEntities);
    }

    protected BlockPos parseSize() {
        short width = checkTag(schematic, "Width", ShortNBT.class).getAsShort();
        short height = checkTag(schematic, "Height", ShortNBT.class).getAsShort();
        short length = checkTag(schematic, "Length", ShortNBT.class).getAsShort();
        return new BlockPos(width, height, length);
    }

    private static void addBlocksToPalette(int id, String key, BlockState[] palette) {
        int keyId = LegacyMapper.getOldNameToBlockId(key);
        if (keyId == -1) {
            keyId = LegacyMapper.getNameToBlockId(key);
        }
        if (keyId != -1) {
            keyId = keyId >> 4;
            for (int meta = 0; meta < 16; ++meta) {
                BlockState state1 = LegacyMapper.getBlockFromLegacy(keyId, meta);
                if (state1 != null) {
                    palette[(id << 4) | meta] = state1;
                }
            }
        } else {
            Logger.warn("Can't find legacy id : " + key);
        }
    }

    private void parsePalette() {
        palette = new BlockState[1 << 16];
        if (schematic.contains("SchematicMapping", Constants.NBT.TAG_COMPOUND)) {
            Logger.warn("This file is not supported file. It will be not loaded successfully.");
        } else if (schematic.contains("SchematicaMapping", Constants.NBT.TAG_COMPOUND)) {
            CompoundNBT tag = schematic.getCompound("SchematicaMapping");
            Set<String> keys = tag.getAllKeys();

            for (String key : keys) {
                int id = tag.getShort(key);
                if (id < 0 || id >= 4096) {
                    throw new IllegalArgumentException("Invalid ID '" + id + "' in SchematicaMapping for block '" + key + "', range: 0 - 4095");
                }
                addBlocksToPalette(id, key, palette);
            }
        } else if (schematic.contains("BlockIDs", Constants.NBT.TAG_COMPOUND)) {
            CompoundNBT tag = schematic.getCompound("BlockIDs");
            Set<String> keys = tag.getAllKeys();

            for (String idStr : keys) {
                String key = tag.getString(idStr);
                int id;
                try {
                    id = Integer.parseInt(idStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid ID '" + idStr + "' (not a number) in MCEdit2 palette for block '" + key + "'");
                }
                if (id < 0 || id >= 4096) {
                    throw new IllegalArgumentException("Invalid ID '" + id + "' in BlockIDs for block '" + key + "', range: 0 - 4095");
                }
                addBlocksToPalette(id, key, palette);
            }
        } else {
            for (int id = 0; id < 4096; ++id) {
                for (int meta = 0; meta < 16; ++meta) {
                    BlockState state = LegacyMapper.getBlockFromLegacy(id, meta);
                    if (state != null) {
                        palette[(id << 4) | meta] = state;
                    }
                }
            }
        }
    }

    private boolean parseMetadata() {
        notifyProgress("NBT 데이터 읽는 중...", 0);

        // Check
        if (!schematic.contains("Blocks")) {
            throw new IllegalArgumentException("Schematic file is missing a 'Blocks' tag");
        }

        // Check type of Schematic
        String materials = schematic.getString("Materials");
        if (!materials.equals("Alpha")) {
            throw new IllegalArgumentException("Schematic file is not an Alpha schematic");
        }

        BlockPos size = parseSize();
        this.width = size.getX();
        this.height = size.getY();
        this.length = size.getZ();
        this.result = new SchematicContainer(size);

        parsePalette();
        return false;
    }

    private boolean parseBlocks() {
        if (current == 0) {
            this.blockId = checkTag(schematic, "Blocks", ByteArrayNBT.class).getAsByteArray();
            this.blockData = checkTag(schematic, "Data", ByteArrayNBT.class).getAsByteArray();
            this.blocks = new short[blockId.length]; // Have to later combine IDs

            // We support 4096 block IDs using the same method as vanilla Minecraft, where
            // the highest 4 bits are stored in a separate byte array.
            this.addId = new byte[0];
            if (schematic.contains("AddBlocks")) {
                addId = checkTag(schematic, "AddBlocks", ByteArrayNBT.class).getAsByteArray();
            }
        }

        // Combine the AddBlocks data with the first 8-bit block ID
        for (int i = 0; i < batchCount && current < blockId.length; ++i, ++current) {
            if ((current >> 1) >= addId.length) { // No corresponding AddBlocks index
                blocks[current] = (short) (blockId[current] & 0xFF);
            } else {
                if ((current & 1) == 0) {
                    blocks[current] = (short) (((addId[current >> 1] & 0x0F) << 8) + (blockId[current] & 0xFF));
                } else {
                    blocks[current] = (short) (((addId[current >> 1] & 0xF0) << 4) + (blockId[current] & 0xFF));
                }
            }
        }

        notifyProgress("Block 읽는 중...", 0.1 + 0.2 * current / blockId.length);
        return current < blockId.length;
    }

    private boolean parseTileEntities() {
        if (current == 0) {
            final ListNBT tileEntityTag = getTag(schematic, "TileEntities", ListNBT.class);
            this.tileEntities = tileEntityTag == null ? new ArrayList<>() : tileEntityTag;
            this.tileEntitiesMap = new HashMap<>();
            this.blockStates = new HashMap<>();
        }

        for (int i = 0; i < batchCount && current < tileEntities.size(); ++i, ++current) {
            INBT tag = tileEntities.get(i);
            if (!(tag instanceof CompoundNBT)) {
                continue;
            }
            CompoundNBT t = (CompoundNBT) tag;
            String id = t.getString("id");
            t.put("id", StringNBT.valueOf(convertBlockEntityId(id)));
            int x = t.getInt("x");
            int y = t.getInt("y");
            int z = t.getInt("z");
            int index = y * width * length + z * width + x;

            BlockState block = palette[(blocks[index] << 4) | blockData[index]];
            if (block == null) {
                for (int meta = 0; meta < 16; ++meta) {
                    block = palette[(blocks[index] << 4) | meta];
                    if (block != null) {
                        palette[(blocks[index] << 4) | blockData[index]] = block;
                        Logger.warn(blocks[index] + ":" + blockData[index] + " is replaced to " + block +
                                ". Because this block id is not in palette.");
                        break;
                    }
                }
            }

            BlockState newBlock = block;
            if (newBlock != null) {
                for (NBTCompatibilityHandler handler : COMPATIBILITY_HANDLERS) {
                    if (handler.isAffectedBlock(newBlock)) {
                        newBlock = handler.updateNBT(block, t);
                        if (newBlock == null || t.isEmpty()) {
                            break;
                        }
                    }
                }
            } else {
                Logger.warn("No block is in palette: " + blocks[index] + ":" + blockData[index]);
                continue;
            }

            if (t.isEmpty()) {
                t = null;
            }

            BlockPos vec = new BlockPos(x, y, z);
            if (t != null) {
                tileEntitiesMap.put(vec, t);
            }
            blockStates.put(vec, newBlock);
        }

        if (tileEntities.size() > 0) {
            notifyProgress("TileEntity 데이터 읽는 중...", 0.3 + 0.2 * current / tileEntities.size());
        }
        return current < tileEntities.size();
    }

    private boolean addBlocks() {
        if (current == 0) {
            this.unknownBlocks = new HashSet<>();
        }

        int total = width * height * length;
        for (int i = 0; i < batchCount && current < total; ++i, ++current) {
            int y = current / (width * length);
            int z = (current % (width * length)) / width;
            int x = (current % (width * length)) % width;

            BlockPos pt = new BlockPos(x, y, z);
            BlockState state = blockStates.get(pt);
            int blockIdMeta = (blocks[current] << 4) | blockData[current];

            if (state == null) {
                state = palette[blockIdMeta];
                blockStates.put(pt, state);
            }

            if (state == SchematicContainer.AIR_BLOCK_STATE) {
                continue;
            }

            if (state == null) {
                if (unknownBlocks.add(blockIdMeta)) {
                    Logger.warn("Unknown block when loading schematic: "
                            + blocks[current] + ":" + blockData[current] + ". This is most likely a bad schematic.");
                }
                continue;
            }

            CompoundNBT blockNBT = null;
            if (tileEntitiesMap.containsKey(pt)) {
                blockNBT = tileEntitiesMap.get(pt).copy();
            }

            result.setBlock(pt, state, blockNBT);
        }

        notifyProgress("Block 읽는 중...", 0.5 + 0.45 * current / total);
        return current < total;
    }

    private boolean addEntities() {
        ListNBT entityList = getTag(schematic, "Entities", ListNBT.class);
        if (entityList != null) {
            long current = 0;
            for (INBT tag : entityList) {
                if (tag instanceof CompoundNBT) {
                    CompoundNBT compound = (CompoundNBT) tag;
                    String id = convertEntityId(compound.getString("id"));
                    if (!id.isEmpty()) {
                        CompoundNBT entityNBTTag = compound.copy();
                        for (EntityNBTCompatibilityHandler compatibilityHandler : ENTITY_COMPATIBILITY_HANDLERS) {
                            if (compatibilityHandler.isAffectedEntity(id, entityNBTTag)) {
                                entityNBTTag = compatibilityHandler.updateNBT(id, entityNBTTag);
                            }
                        }
                        result.addEntity(entityNBTTag);
                    }
                }
                notifyProgress("Entity 읽는 중...", 0.95 + 0.04 * (current++) / entityList.size());
            }
        }
        return false;
    }

    @Override
    public boolean parsePartial() {
        return stageProcessor.tick();
    }

    protected String convertEntityId(String id) {
        switch (id) {
            case "AreaEffectCloud":
                return "area_effect_cloud";
            case "ArmorStand":
                return "armor_stand";
            case "CaveSpider":
                return "cave_spider";
            case "MinecartChest":
                return "chest_minecart";
            case "DragonFireball":
                return "dragon_fireball";
            case "ThrownEgg":
                return "egg";
            case "EnderDragon":
                return "ender_dragon";
            case "ThrownEnderpearl":
                return "ender_pearl";
            case "FallingSand":
                return "falling_block";
            case "FireworksRocketEntity":
                return "fireworks_rocket";
            case "MinecartFurnace":
                return "furnace_minecart";
            case "MinecartHopper":
                return "hopper_minecart";
            case "EntityHorse":
                return "horse";
            case "ItemFrame":
                return "item_frame";
            case "LeashKnot":
                return "leash_knot";
            case "LightningBolt":
                return "lightning_bolt";
            case "LavaSlime":
                return "magma_cube";
            case "MinecartRideable":
                return "minecart";
            case "MushroomCow":
                return "mooshroom";
            case "Ozelot":
                return "ocelot";
            case "PolarBear":
                return "polar_bear";
            case "ThrownPotion":
                return "potion";
            case "ShulkerBullet":
                return "shulker_bullet";
            case "SmallFireball":
                return "small_fireball";
            case "MinecartSpawner":
                return "spawner_minecart";
            case "SpectralArrow":
                return "spectral_arrow";
            case "PrimedTnt":
                return "tnt";
            case "MinecartTNT":
                return "tnt_minecart";
            case "VillagerGolem":
                return "villager_golem";
            case "WitherBoss":
                return "wither";
            case "WitherSkull":
                return "wither_skull";
            case "PigZombie":
                return "zombie_pigman";
            case "XPOrb":
            case "xp_orb":
                return "experience_orb";
            case "ThrownExpBottle":
            case "xp_bottle":
                return "experience_bottle";
            case "EyeOfEnderSignal":
            case "eye_of_ender_signal":
                return "eye_of_ender";
            case "EnderCrystal":
            case "ender_crystal":
                return "end_crystal";
            case "fireworks_rocket":
                return "firework_rocket";
            case "MinecartCommandBlock":
            case "commandblock_minecart":
                return "command_block_minecart";
            case "snowman":
                return "snow_golem";
            case "villager_golem":
                return "iron_golem";
            case "evocation_fangs":
                return "evoker_fangs";
            case "evocation_illager":
                return "evoker";
            case "vindication_illager":
                return "vindicator";
            case "illusion_illager":
                return "illusioner";
            default:
                return id;
        }
    }

    protected String convertBlockEntityId(String id) {
        switch (id) {
            case "Cauldron":
                return "brewing_stand";
            case "Control":
                return "command_block";
            case "DLDetector":
                return "daylight_detector";
            case "Trap":
                return "dispenser";
            case "EnchantTable":
                return "enchanting_table";
            case "EndGateway":
                return "end_gateway";
            case "AirPortal":
                return "end_portal";
            case "EnderChest":
                return "ender_chest";
            case "FlowerPot":
                return "flower_pot";
            case "RecordPlayer":
                return "jukebox";
            case "MobSpawner":
                return "mob_spawner";
            case "Music":
            case "noteblock":
                return "note_block";
            case "Structure":
                return "structure_block";
            case "Chest":
                return "chest";
            case "Sign":
                return "sign";
            case "Banner":
                return "banner";
            case "Beacon":
                return "beacon";
            case "Comparator":
                return "comparator";
            case "Dropper":
                return "dropper";
            case "Furnace":
                return "furnace";
            case "Hopper":
                return "hopper";
            case "Skull":
                return "skull";
            default:
                return id;
        }
    }
}
