package com.won983212.servermod.client;

import com.won983212.servermod.ForgeEventHandler;
import com.won983212.servermod.Key;
import com.won983212.servermod.ModDist;
import com.won983212.servermod.client.gui.ConfigScreen;
import com.won983212.servermod.client.render.RenderIndustrialAlarm;
import com.won983212.servermod.tile.ModTiles;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class ClientDist implements ModDist {
    public ClientDist() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
                () -> (mc, screen) -> new ConfigScreen()
        );
        ClientRegistry.bindTileEntityRenderer(ModTiles.tileEntityIndustrialAlarm, RenderIndustrialAlarm::new);
        Key.registerKeys();
        ForgeEventHandler.prepareLegacyIdMap();
    }
}
