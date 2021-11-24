package com.won983212.servermod.tile;

import com.won983212.servermod.block.attribute.Attributes;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ActiveStateTile extends TileEntity implements ITickableTileEntity {
    private boolean redstone = false;
    private boolean activeCache = false;
    private Direction directionCache = null;

    public ActiveStateTile(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
        onPowerChange();
    }

    public void onNeighborChange(BlockState block, BlockPos neighborPos) {
        if (level != null && !level.isClientSide()) {
            updatePower();
        }
    }

    public void onAdded() {
        updatePower();
    }

    @Override
    public void tick() {
    }

    @Override
    public double getViewDistance() {
        final int MAXIMUM_DISTANCE_IN_BLOCKS = 128;
        return MAXIMUM_DISTANCE_IN_BLOCKS * MAXIMUM_DISTANCE_IN_BLOCKS;
    }

    protected boolean isClient() {
        return level != null && level.isClientSide;
    }

    // active state manage
    public void setActive(boolean activeCache) {
        if (this.activeCache != activeCache) {
            this.activeCache = activeCache;
            if (getActiveState() != activeCache) {
                level.setBlockAndUpdate(getBlockPos(), Attributes.ACTIVE.set(getBlockState(), activeCache));
            }
        }
    }

    public boolean isActive() {
        if (level != null) {
            if (level.isClientSide()) {
                return getActiveState();
            } else {
                return activeCache;
            }
        }
        return false;
    }

    private boolean getActiveState() {
        World level = getLevel();
        if (level != null) {
            return Attributes.ACTIVE.get(getBlockState());
        }
        return false;
    }

    // directional state manage
    public void setDirection(Direction dir) {
        if (directionCache != dir && level != null) {
            directionCache = dir;
            level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(BlockStateProperties.FACING, dir));
        }
    }

    public Direction getDirection() {
        if (directionCache != null) {
            return directionCache;
        }

        if (getLevel() != null) {
            return getBlockState().getValue(BlockStateProperties.FACING);
        }
        return null;
    }

    // redstone active logic
    protected boolean isPowered() {
        return redstone;
    }

    protected void onPowerChange() {
    }

    private void updatePower() {
        if (level == null) {
            return;
        }

        boolean power = level.hasNeighborSignal(getBlockPos());
        if (redstone != power) {
            redstone = power;
            onPowerChange();
        }
    }
}
