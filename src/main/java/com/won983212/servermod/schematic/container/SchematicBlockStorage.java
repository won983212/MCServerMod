package com.won983212.servermod.schematic.container;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class SchematicBlockStorage {
    private final short[] data;
    private final BlockPalette palette;
    private final int stride;
    private final int sizeX;

    public SchematicBlockStorage(BlockPos size) {
        int len = size.getX() * size.getY() * size.getZ();
        this.sizeX = size.getX();
        this.data = new short[len];
        this.palette = new BlockPalette();
        this.stride = sizeX * size.getZ();
    }

    public void setBlock(BlockPos pos, BlockState state) {
        int index = getIndex(pos);
        data[index] = (short) palette.idFor(state);
    }

    public BlockState getBlock(BlockPos pos) {
        int index = getIndex(pos);
        BlockState state = palette.stateFor(data[index]);
        return state == null ? SchematicContainer.AIR_BLOCK_STATE : state;
    }

    private int getIndex(BlockPos pos) {
        return pos.getY() * stride + pos.getZ() * sizeX + pos.getX();
    }
}
