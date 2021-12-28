package com.won983212.servermod.schematic.container;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public class SchematicBlockStorage {
    private final short[] data;
    private final BlockPalette palette;
    private final int stride;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public SchematicBlockStorage(BlockPos size) {
        int len = size.getX() * size.getY() * size.getZ();
        this.sizeX = size.getX();
        this.sizeY = size.getY();
        this.sizeZ = size.getZ();
        this.data = new short[len];
        this.palette = new BlockPalette();
        this.stride = sizeX * size.getZ();

        short air = (short) palette.idFor(SchematicContainer.AIR_BLOCK_STATE);
        Arrays.fill(data, air);
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

    public boolean isInBounds(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeZ;
    }

    private int getIndex(BlockPos pos) {
        return pos.getY() * stride + pos.getZ() * sizeX + pos.getX();
    }
}
