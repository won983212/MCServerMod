package com.won983212.servermod.schematic.container;

import net.minecraft.block.BlockState;

class BlockArrayPalette {
    public static final int ARRAY_SIZE_LIMIT = 16;

    public final BlockState[] states;
    private final IArrayPaletteFullEvent event;
    private int currentSize;

    protected BlockArrayPalette(IArrayPaletteFullEvent event) {
        this.states = new BlockState[ARRAY_SIZE_LIMIT];
        this.currentSize = 0;
        this.event = event;
    }

    public int idFor(BlockState state) {
        for (int i = 0; i < this.currentSize; ++i) {
            if (this.states[i] == state) {
                return i;
            }
        }

        final int size = this.currentSize;

        if (size < this.states.length) {
            this.states[size] = state;
            ++this.currentSize;
            if (currentSize >= ARRAY_SIZE_LIMIT) {
                this.event.onFull();
            }
            return size;
        }
        return -1;
    }

    public BlockState stateFor(int id) {
        return id >= 0 && id < this.currentSize ? this.states[id] : SchematicContainer.AIR_BLOCK_STATE;
    }

    public interface IArrayPaletteFullEvent {
        void onFull();
    }
}
