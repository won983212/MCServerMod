package com.won983212.servermod.schematic.parser;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.gen.feature.template.Template;

public class VanillaSchematicReader extends AbstractSchematicReader {
    @Override
    protected Template parse(CompoundNBT schematic) {
        Template t = new Template();
        t.load(schematic);
        return t;
    }
}
