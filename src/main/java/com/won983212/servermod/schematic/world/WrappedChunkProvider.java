package com.won983212.servermod.schematic.world;

import net.minecraft.world.IBlockReader;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.lighting.WorldLightManager;

import javax.annotation.Nullable;

public class WrappedChunkProvider extends AbstractChunkProvider {

    private final EmptierChunk virtualChunk = new EmptierChunk();

    @Nullable
    @Override
    public IBlockReader getChunkForLighting(int x, int z) {
        return virtualChunk;
    }

    @Override
    public IBlockReader getLevel() {
        return null;
    }

    @Nullable
    @Override
    public IChunk getChunk(int x, int z, ChunkStatus status, boolean p_212849_4_) {
        return virtualChunk;
    }

    @Override
    public String gatherStats() {
        return "WrappedChunkProvider";
    }

    @Override
    public WorldLightManager getLightEngine() {
        return null;
    }
}