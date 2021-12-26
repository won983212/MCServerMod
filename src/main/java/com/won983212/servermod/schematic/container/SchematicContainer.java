package com.won983212.servermod.schematic.container;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SchematicContainer {
    public static final BlockState AIR_BLOCK_STATE = Blocks.AIR.defaultBlockState();

    private final SchematicBlockStorage blocks;
    private final HashMap<BlockPos, CompoundNBT> tiles;
    private final List<CompoundNBT> entities;
    private final BlockPos size;

    public SchematicContainer(BlockPos size) {
        this.blocks = new SchematicBlockStorage(size);
        this.entities = new ArrayList<>();
        this.tiles = new HashMap<>();
        this.size = size;
    }

    public void setBlock(BlockPos pos, BlockState state, CompoundNBT nbt) {
        blocks.setBlock(pos, state);
        if (nbt != null) {
            tiles.put(pos, nbt);
        }
    }

    public void addEntity(CompoundNBT tag) {
        entities.add(tag);
    }

    public CompoundNBT getTileTagAt(BlockPos pos) {
        return tiles.get(pos);
    }

    public BlockState getBlockAt(BlockPos pos) {
        return blocks.getBlock(pos);
    }

    public Iterable<CompoundNBT> getEntities() {
        return entities;
    }

    public BlockPos getSize() {
        return size;
    }
}
