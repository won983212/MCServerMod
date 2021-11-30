package com.won983212.servermod.schematic.parser;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.Template;

import java.io.IOException;

public class VanillaSchematicReader extends AbstractSchematicReader {

    @Override
    protected Template parse(CompoundNBT schematic) {
        Template t = new Template();
        notifyProgress("Schematic 읽는 중...", 0);
        t.load(schematic);
        notifyProgress("Schematic 읽는 중...", 0.99);
        return t;
    }

    @Override
    protected BlockPos parseSize(CompoundNBT schematic) {
        ListNBT listnbt = schematic.getList("size", 3);
        return new BlockPos(listnbt.getInt(0), listnbt.getInt(1), listnbt.getInt(2));
    }
}
