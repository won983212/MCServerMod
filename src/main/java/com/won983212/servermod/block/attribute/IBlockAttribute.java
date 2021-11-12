package com.won983212.servermod.block.attribute;

import net.minecraft.block.BlockState;
import net.minecraft.state.Property;

public interface IBlockAttribute<T extends Comparable<T>> {
    void has(BlockState state);

    T get(BlockState state);

    BlockState set(BlockState state, T value);

    Property<T> getProperty();

    BlockState getDefaultState(BlockState state);
}
