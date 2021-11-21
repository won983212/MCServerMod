package com.won983212.servermod.client;

import com.won983212.servermod.LegacyMapper;
import com.won983212.servermod.ModDist;
import com.won983212.servermod.ModKeys;
import com.won983212.servermod.client.gui.ConfigScreen;
import com.won983212.servermod.client.render.tile.RenderIndustrialAlarm;
import com.won983212.servermod.schematic.client.SchematicHandler;
import com.won983212.servermod.schematic.parser.MCEditSchematicReader;
import com.won983212.servermod.tile.ModTiles;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.io.File;
import java.io.IOException;

public class ClientDist extends ModDist {
    public static final SchematicHandler SCHEMATIC_HANDLER = new SchematicHandler();
    public static Template t;

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        super.onCommonSetup(event);
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
                () -> (mc, screen) -> new ConfigScreen()
        );
        ClientRegistry.bindTileEntityRenderer(ModTiles.tileEntityIndustrialAlarm, RenderIndustrialAlarm::new);
        ModKeys.registerKeys();
        LegacyMapper.getInstance();

        // TODO Test load
        String[] schems = new String[]{"270"};
        for(String schem : schems) {
            try {
                t = new MCEditSchematicReader().parseSchematic(new File("C:\\Users\\psvm\\IdeaProjects\\MCServerMod\\run\\schematics\\" + schem + ".schematic"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
