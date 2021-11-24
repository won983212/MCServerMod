package com.won983212.servermod.schematic.world;

import com.won983212.servermod.schematic.world.chunk.WrappedChunkProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.World;
import net.minecraft.world.lighting.WorldLightManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class PlacementSimulationWorld extends WrappedWorld {
    public Map<BlockPos, BlockState> blocksAdded;
    public Map<BlockPos, TileEntity> tesAdded;

    public Set<SectionPos> spannedSections;
    public WorldLightManager lighter;
    public WrappedChunkProvider chunkProvider;
    private final BlockPos.Mutable scratch = new BlockPos.Mutable();

    public PlacementSimulationWorld(World wrapped, WrappedChunkProvider chunkProvider) {
        super(wrapped, chunkProvider);
        this.chunkProvider = chunkProvider.setWorld(this);
        spannedSections = new HashSet<>();
        lighter = new WorldLightManager(chunkProvider, true, false); // blockLight, skyLight
        blocksAdded = new HashMap<>();
        tesAdded = new HashMap<>();
    }

    @Override
    public WorldLightManager getLightEngine() {
        return lighter;
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState newState, int flags) {
        blocksAdded.put(pos, newState);

        SectionPos sectionPos = SectionPos.of(pos);
        if (spannedSections.add(sectionPos)) {
            lighter.updateSectionStatus(sectionPos, false);
        }

        if ((flags & 128) == 0) {
            lighter.checkBlock(pos);
        }

        return true;
    }

    @Override
    public boolean setBlockAndUpdate(BlockPos pos, BlockState state) {
        return setBlock(pos, state, 0);
    }

    @Override
    public TileEntity getBlockEntity(BlockPos pos) {
        return tesAdded.get(pos);
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> condition) {
        return condition.test(getBlockState(pos));
    }

    @Override
    public boolean isLoaded(BlockPos pos) {
        return true;
    }

    @Override
    public boolean isAreaLoaded(BlockPos center, int range) {
        return true;
    }

    public BlockState getBlockState(int x, int y, int z) {
        return getBlockState(scratch.set(x, y, z));
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        BlockState state = blocksAdded.get(pos);
        if (state != null) {
            return state;
        }
        return Blocks.AIR.defaultBlockState();
    }
}
