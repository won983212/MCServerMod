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
        return state.get(BlockStateProperties.FACING);
    }

    @Override
    public BlockState set(BlockState state, Direction value) {
        return state.with(BlockStateProperties.FACING, value);
    }

    @Override
    public Property<Direction> getProperty() {
        return BlockStateProperties.FACING;
    }

    @Override
    public BlockState getDefaultState(BlockState state) {
        return state.with(BlockStateProperties.FACING, Direction.NORTH);
    }
}
