package com.won983212.servermod.item;

import com.won983212.servermod.ServerMod;
import com.won983212.servermod.block.ModBlocks;
import com.won983212.servermod.client.render.item.block.RenderIndustrialAlarmItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ServerMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {
    public static final BlockItem itemIndustrialAlarm = (BlockItem) new BlockItem(ModBlocks.blockIndustrialAlarm, new Item.Properties()
            .setISTER(() -> RenderIndustrialAlarmItem::new)
            .stacksTo(64)
            .tab(ItemGroup.TAB_DECORATIONS))
            .setRegistryName("block_industrial_alarm");

    @SubscribeEvent
    public static void onItemsRegistration(final RegistryEvent.Register<Item> itemRegisterEvent) {
        itemRegisterEvent.getRegistry().register(itemIndustrialAlarm);
    }
}
