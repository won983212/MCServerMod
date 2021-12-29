package com.won983212.servermod.schematic.client.tools;

import com.won983212.servermod.client.ClientMod;
import net.minecraft.item.ItemStack;

public class ToggleAirTool extends SchematicToolBase {
    @Override
    public boolean handleMouseWheel(double delta) {
        return false;
    }

    @Override
    public boolean handleRightClick() {
        ItemStack item = schematicHandler.getActiveSchematicItem();
        if (item != null) {
            schematicHandler.toggleIncludeAir();
        }
        return item != null;
    }

    @Override
    public int getHighlightColor() {
        if (ClientMod.SCHEMATIC_HANDLER.isIncludeAir()) {
            return 0xff4AA96C;
        } else {
            return 0xffF55C47;
        }
    }
}
