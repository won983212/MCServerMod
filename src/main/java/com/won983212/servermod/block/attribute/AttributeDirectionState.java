package com.won983212.servermod.block.attribute;

import net.minecraft.block.BlockState;
import net.minecraft.state.Property;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;

public class AttributeDirectionState implements IBlockAttribute<Direction> {
    @Override
    public void has(BlockState state) {
        state.hasProperty(BlockStateProperties.FACING);
    }

    @Override
    public Direction get(BlockState state) {
        return state.getValue(BlockStateProperties.FACING);
    }

    @Override
    public BlockState set(BlockState state, Direction value) {
        return state.setValue(BlockStateProperties.FACING, value);
    }

    @Override
    public Property<Direction> getProperty() {
        return BlockStateProperties.FACING;
    }

    @Override
    public BlockState getDefaultState(BlockState state) {
        return state.setValue(BlockStateProperties.FACING, Direction.NORTH);
    }
}
