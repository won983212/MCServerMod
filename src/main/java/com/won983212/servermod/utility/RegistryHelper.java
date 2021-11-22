package com.won983212.servermod.utility;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nonnull;

public class RegistryHelper {
    public static Block getBlockFromId(String id, Block defaultValue) {
        if (id.equals("minecraft:air"))
            return Blocks.AIR;
        Block block = Registry.BLOCK.get(new ResourceLocation(id));
        return block == Blocks.AIR ? defaultValue : block;
    }

    @Nonnull
    public static Block getBlockFromId(String id) {
        return getBlockFromId(id, Blocks.AIR);
    }

    public static Item getItemFromId(String id, Item defaultValue) {
        if (id.equals("minecraft:air"))
            return Items.AIR;
        Item item = Registry.ITEM.get(new ResourceLocation(id));
        return item == Items.AIR ? defaultValue : item;
    }

    @Nonnull
    public static Item getItemFromId(String id) {
        return getItemFromId(id, Items.AIR);
    }
}
