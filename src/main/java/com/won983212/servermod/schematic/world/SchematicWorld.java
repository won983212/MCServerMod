package com.won983212.servermod.schematic.world;

import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.container.SchematicBlockStorage;
import com.won983212.servermod.schematic.container.SchematicContainer;
import com.won983212.servermod.schematic.world.chunk.WrappedChunkProvider;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeRegistry;
import net.minecraft.world.server.ServerWorld;

import java.util.*;
import java.util.function.Predicate;

public class SchematicWorld extends WrappedWorld implements IServerWorld {

    protected final SchematicBlockStorage storage;
    protected final Map<BlockPos, TileEntity> tileEntities;
    protected final List<Entity> entities;
    protected final MutableBoundingBox bounds;

    public final BlockPos anchor;

    public SchematicWorld(World original, BlockPos size) {
        this(BlockPos.ZERO, size, original);
    }

    public SchematicWorld(BlockPos anchor, BlockPos size, World original) {
        super(original, new WrappedChunkProvider());
        this.storage = new SchematicBlockStorage(size);
        this.tileEntities = new HashMap<>();
        this.bounds = new MutableBoundingBox();
        this.anchor = anchor;
        this.entities = new ArrayList<>();
    }

    @Override
    public boolean addFreshEntity(Entity entityIn) {
        if (entityIn instanceof ItemFrameEntity) {
            ((ItemFrameEntity) entityIn).getItem().setTag(null);
        }
        if (entityIn instanceof ArmorStandEntity) {
            ArmorStandEntity armorStandEntity = (ArmorStandEntity) entityIn;
            armorStandEntity.getAllSlots().forEach(stack -> stack.setTag(null));
        }

        return entities.add(entityIn);
    }

    @Override
    public TileEntity getBlockEntity(BlockPos pos) {
        if (isOutsideBuildHeight(pos)) {
            return null;
        }
        if (tileEntities.containsKey(pos)) {
            return tileEntities.get(pos);
        }
        if (!storage.isInBounds(pos.subtract(anchor))) {
            return null;
        }

        BlockState blockState = getBlockState(pos);
        if (blockState.hasTileEntity()) {
            try {
                TileEntity tileEntity = blockState.createTileEntity(this);
                if (tileEntity != null) {
                    onTEadded(tileEntity, pos);
                    tileEntities.put(pos, tileEntity);
                }
                return tileEntity;
            } catch (Exception e) {
                Logger.debug("Could not create TE of block " + blockState + ": " + e);
            }
        }
        return null;
    }

    protected void onTEadded(TileEntity tileEntity, BlockPos pos) {
        tileEntity.setLevelAndPosition(this, pos);
    }

    @Override
    public BlockState getBlockState(BlockPos globalPos) {
        BlockPos pos = globalPos.subtract(anchor);

        if (pos.getY() - bounds.y0 == -1) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (getBounds().isInside(pos) && storage.isInBounds(pos)) {
            return processBlockStateForPrinting(storage.getBlock(pos));
        }
        return SchematicContainer.AIR_BLOCK_STATE;
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return BiomeRegistry.THE_VOID;
    }

    @Override
    public int getBrightness(LightType p_226658_1_, BlockPos p_226658_2_) {
        return 10;
    }

    @Override
    public List<Entity> getEntities(Entity arg0, AxisAlignedBB arg1, Predicate<? super Entity> arg2) {
        return Collections.emptyList();
    }

    @Override
    public <T extends Entity> List<T> getEntitiesOfClass(Class<? extends T> arg0, AxisAlignedBB arg1, Predicate<? super T> arg2) {
        return Collections.emptyList();
    }

    @Override
    public List<? extends PlayerEntity> players() {
        return Collections.emptyList();
    }

    @Override
    public int getSkyDarken() {
        return 0;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
        return predicate.test(getBlockState(pos));
    }

    @Override
    public boolean destroyBlock(BlockPos arg0, boolean arg1) {
        return setBlock(arg0, SchematicContainer.AIR_BLOCK_STATE, 3);
    }

    @Override
    public boolean removeBlock(BlockPos arg0, boolean arg1) {
        return setBlock(arg0, SchematicContainer.AIR_BLOCK_STATE, 3);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState arg1, int arg2) {
        pos = pos.immutable().subtract(anchor);
        bounds.expand(new MutableBoundingBox(pos, pos));
        storage.setBlock(pos, arg1);

        if (tileEntities.containsKey(pos)) {
            TileEntity tileEntity = tileEntities.get(pos);
            if (!tileEntity.getType().isValid(arg1.getBlock())) {
                tileEntities.remove(pos);
            }
        }

        TileEntity tileEntity = getBlockEntity(pos);
        if (tileEntity != null) {
            tileEntities.put(pos, tileEntity);
        }

        return true;
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
    }

    @Override
    public ITickList<Block> getBlockTicks() {
        return EmptyTickList.empty();
    }

    @Override
    public ITickList<Fluid> getLiquidTicks() {
        return EmptyTickList.empty();
    }

    public MutableBoundingBox getBounds() {
        return bounds;
    }

    public Iterable<TileEntity> getRenderedTileEntities() {
        return tileEntities.values();
    }

    protected BlockState processBlockStateForPrinting(BlockState state) {
        if (state.getBlock() instanceof AbstractFurnaceBlock && state.hasProperty(BlockStateProperties.LIT)) {
            state = state.setValue(BlockStateProperties.LIT, false);
        }
        return state;
    }

    @Override
    public ServerWorld getLevel() {
        if (this.world instanceof ServerWorld) {
            return (ServerWorld) this.world;
        }
        throw new IllegalStateException("Cannot use IServerWorld#getWorld in a client environment");
    }

}
