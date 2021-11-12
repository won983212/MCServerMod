package com.won983212.servermod.block.attribute;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;

public class Attributes {
    public static final AttributeActiveState ACTIVE = new AttributeActiveState();
    public static final AttributeDirectionState FACING = new AttributeDirectionState();

    public static void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE.getProperty());
        builder.add(FACING.getProperty());
    }
}
