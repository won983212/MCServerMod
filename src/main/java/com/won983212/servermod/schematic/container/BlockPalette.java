package com.won983212.servermod.schematic.container;

import net.minecraft.block.BlockState;
import net.minecraft.util.IntIdentityHashBiMap;

public class BlockPalette implements BlockArrayPalette.IArrayPaletteFullEvent {
    private final IntIdentityHashBiMap<BlockState> paletteMap;
    private BlockArrayPalette smallPalette;

    protected BlockPalette() {
        this.paletteMap = new IntIdentityHashBiMap<>(BlockArrayPalette.ARRAY_SIZE_LIMIT << 1);
        this.smallPalette = new BlockArrayPalette(this);
    }

    public int idFor(BlockState state) {
        if (smallPalette != null) {
            return smallPalette.idFor(state);
        }
        int i = this.paletteMap.getId(state);
        if (i == -1) {
            i = this.paletteMap.add(state);
        }
        return i;
    }

    public BlockState stateFor(int id) {
        if (smallPalette != null) {
            return smallPalette.stateFor(id);
        }
        return this.paletteMap.byId(id);
    }

    @Override
    public void onFull() {
        for (int i = 0; i < smallPalette.states.length; i++) {
            paletteMap.addMapping(smallPalette.states[i], i);
        }
        smallPalette = null;
    }
}
