package com.won983212.servermod.block;

import com.won983212.servermod.ServerMod;
import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ServerMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlocks {
    public static final BlockIndustrialAlarm blockIndustrialAlarm =
            (BlockIndustrialAlarm) (new BlockIndustrialAlarm().setRegistryName(ServerMod.MODID, "block_industrial_alarm"));

    @SubscribeEvent
    public static void onBlocksRegistration(final RegistryEvent.Register<Block> blockRegisterEvent) {
        blockRegisterEvent.getRegistry().register(blockIndustrialAlarm);
    }
}
