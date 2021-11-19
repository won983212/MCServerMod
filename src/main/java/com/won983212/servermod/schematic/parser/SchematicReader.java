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

import com.won983212.servermod.LegacyMapper;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.*;
import net.minecraftforge.common.util.Constants;

import java.io.IOException;

/**
 * Base class for NBT schematic readers.
 */
public abstract class SchematicReader {

    protected static <T extends INBT> T checkTag(CompoundNBT nbt, String key, Class<T> expected) throws IOException {
        byte typeId = getNBTTypeFromClass(expected);
        if (!nbt.contains(key, typeId)) {
            throw new IOException(key + " tag is not found or is not of tag type " + expected);
        }
        return (T) nbt.get(key);
    }

    protected static <T extends INBT> T getTag(CompoundNBT nbt, String key, Class<T> expected) throws IOException {
        byte typeId = getNBTTypeFromClass(expected);
        if (!nbt.contains(key, typeId)) {
            return null;
        }
        return (T) nbt.get(key);
    }

    private static byte getNBTTypeFromClass(Class<?> cls) throws IOException {
        if (cls == ByteNBT.class)
            return Constants.NBT.TAG_BYTE;
        if (cls == ShortNBT.class)
            return Constants.NBT.TAG_SHORT;
        if (cls == IntNBT.class)
            return Constants.NBT.TAG_INT;
        if (cls == LongNBT.class)
            return Constants.NBT.TAG_LONG;
        if (cls == FloatNBT.class)
            return Constants.NBT.TAG_FLOAT;
        if (cls == DoubleNBT.class)
            return Constants.NBT.TAG_DOUBLE;
        if (cls == ByteArrayNBT.class)
            return Constants.NBT.TAG_BYTE_ARRAY;
        if (cls == StringNBT.class)
            return Constants.NBT.TAG_STRING;
        if (cls == ListNBT.class)
            return Constants.NBT.TAG_LIST;
        if (cls == CompoundNBT.class)
            return Constants.NBT.TAG_COMPOUND;
        if (cls == IntArrayNBT.class)
            return Constants.NBT.TAG_INT_ARRAY;
        if (cls == LongArrayNBT.class)
            return Constants.NBT.TAG_LONG_ARRAY;
        if (cls == NumberNBT.class)
            return Constants.NBT.TAG_ANY_NUMERIC;
        throw new IOException("Invaild type: " + cls);
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

    protected BlockState getBlockState(int id, int data) {
        return LegacyMapper.getInstance().getBlockFromLegacy(id, data);
    }
}
