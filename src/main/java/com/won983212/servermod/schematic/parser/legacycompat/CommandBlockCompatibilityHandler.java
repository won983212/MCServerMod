package com.won983212.servermod.schematic.parser.legacycompat;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class CommandBlockCompatibilityHandler implements NBTCompatibilityHandler {
    @Override
    public boolean isAffectedBlock(BlockState block) {
        return block.getBlock() == Blocks.COMMAND_BLOCK || block.getBlock() == Blocks.CHAIN_COMMAND_BLOCK
                || block.getBlock() == Blocks.REPEATING_COMMAND_BLOCK;
    }

    @Override
    public BlockState updateNBT(BlockState block, CompoundNBT values) {
        if (!values.contains("CustomName")) {
            return block;
        }
        INBT value = values.get("CustomName");
        if (value instanceof StringNBT) {
            String s = value.getAsString();
            if (s.isEmpty()) {
                values.remove("CustomName");
            } else {
                String json = ITextComponent.Serializer.toJson(new StringTextComponent(s));
                values.put("CustomName", StringNBT.valueOf(json));
            }
        }
        return block;
    }
}
