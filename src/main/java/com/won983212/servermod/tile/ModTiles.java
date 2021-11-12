package com.won983212.servermod.tile;

import com.won983212.servermod.ServerMod;
import com.won983212.servermod.block.ModBlocks;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ServerMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModTiles {
    public static final TileEntityType<TileEntityIndustrialAlarm> tileEntityIndustrialAlarm =
            TileEntityType.Builder.create(TileEntityIndustrialAlarm::new, ModBlocks.blockIndustrialAlarm).build(null);

    @SubscribeEvent
    public static void onTileEntityTypeRegistration(final RegistryEvent.Register<TileEntityType<?>> event) {
        tileEntityIndustrialAlarm.setRegistryName(ServerMod.MODID + ":tile_entity_type_industrial_alarm");
        event.getRegistry().register(tileEntityIndustrialAlarm);
    }
}
