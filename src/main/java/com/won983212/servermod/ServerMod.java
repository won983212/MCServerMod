package com.won983212.servermod;

import com.won983212.servermod.client.ClientDist;
import com.won983212.servermod.network.NetworkDispatcher;
import com.won983212.servermod.server.ServerDist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ServerMod.MODID)
public class ServerMod {
    public static final String MODID = "servermod";
    public static final String HOST = "http://web.won983212.synology.me/skin/";
    private final ModDist proxy;

    public ServerMod() {
        proxy = DistExecutor.safeRunForDist(() -> ClientDist::new, () -> ServerDist::new);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        NetworkDispatcher.initDispatcher();
        proxy.onCommonSetup(event);
    }
}
