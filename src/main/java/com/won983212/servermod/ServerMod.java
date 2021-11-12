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

    public ServerMod() {
        DistExecutor.safeRunForDist(() -> ClientDist::new, () -> ServerDist::new);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        NetworkDispatcher.initDispatcher();
    }
}
