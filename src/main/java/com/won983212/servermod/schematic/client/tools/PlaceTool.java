package com.won983212.servermod.schematic.client.tools;

public class PlaceTool extends SchematicToolBase {

    @Override
    public boolean handleRightClick() {
        schematicHandler.printInstantly();
        return true;
    }

    @Override
    public boolean handleMouseWheel(double delta) {
        return false;
    }

}
