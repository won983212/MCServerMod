package com.won983212.servermod;

import com.won983212.servermod.client.ClientMod;
import com.won983212.servermod.network.NetworkDispatcher;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(com.won983212.servermod.ServerMod.MODID)
public class ServerMod {
    public static final String MODID = "servermod";
    public static final String HOST = "http://web.won983212.synology.me/skin/";
    private final CommonMod proxy;

    public ServerMod() {
        proxy = DistExecutor.safeRunForDist(() -> ClientMod::new, () -> com.won983212.servermod.server.ServerMod::new);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        NetworkDispatcher.initDispatcher();
        proxy.onCommonSetup(event);
    }
}
