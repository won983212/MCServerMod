package com.won983212.servermod.block.attribute;

import net.minecraft.block.BlockState;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.Property;

public class AttributeActiveState implements IBlockAttribute<Boolean> {
    private static final BooleanProperty ACTIVE_STATE = BooleanProperty.create("active");

    @Override
    public void has(BlockState state) {
        state.hasProperty(ACTIVE_STATE);
    }

    @Override
    public Boolean get(BlockState state) {
        return state.get(ACTIVE_STATE);
    }

    @Override
    public BlockState set(BlockState state, Boolean value) {
        return state.with(ACTIVE_STATE, value);
    }

    @Override
    public Property<Boolean> getProperty() {
        return ACTIVE_STATE;
    }

    @Override
    public BlockState getDefaultState(BlockState state) {
        return state.with(ACTIVE_STATE, false);
    }
}
