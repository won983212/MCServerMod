package com.won983212.servermod.schematic;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.won983212.servermod.Logger;
import com.won983212.servermod.utility.EntityUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IClearable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.EmptyBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SchematicContainer {
    private final BlockStroage blocks = new BlockStroage();
    private final List<CompoundNBT> entities = new ArrayList<>();
    private BlockPos size = BlockPos.ZERO;


    public void addBlock(BlockPos pos, BlockState state, CompoundNBT tileNbt) {
        blocks.addBlock(pos, state, tileNbt);
    }

    public void resizeBlockContainer(BlockPos size) {
        this.size = size;
    }

    public void addEntity(CompoundNBT entityNbt) {
        entities.add(entityNbt);
    }

    public void placeSchematicToWorld(World world, BlockPos posStart, PlacementSettings placement, IProgressEvent event) {
        PlacingContext context = new PlacingContext(world, posStart, placement, event);
        context.setEventProgressRange(0, 0.8);
        placeBlocks(context);
        context.setEventProgressRange(0.8, 1);
        updateBlocks(context);
        placeEntities(context);
    }

    private void placeBlocks(PlacingContext context) {
        long current = 0;
        long totalBlocks = blocks.size();

        if (totalBlocks > 0) {
            final Rotation rotation = context.placement.getRotation();
            final Mirror mirror = context.placement.getMirror();

            for (BlockData block : blocks) {
                BlockState state = block.state;
                BlockPos pos = Template.calculateRelativePosition(context.placement, block.pos)
                        .offset(context.posStart);

                state = state.mirror(mirror);
                state = state.rotate(rotation);

                if (block.nbt != null) {
                    TileEntity te = context.world.getBlockEntity(pos);
                    if (te != null) {
                        IClearable.tryClear(te);
                        context.world.setBlock(pos, Blocks.BARRIER.defaultBlockState(), Constants.BlockFlags.UPDATE_NEIGHBORS | Constants.BlockFlags.NO_RERENDER);
                    }
                }

                if (context.world.setBlock(pos, state, Constants.BlockFlags.BLOCK_UPDATE)) {
                    if (block.nbt != null) {
                        TileEntity te = context.world.getBlockEntity(pos);
                        if (te != null) {
                            CompoundNBT nbt = block.nbt.copy();
                            nbt.putInt("x", pos.getX());
                            nbt.putInt("y", pos.getY());
                            nbt.putInt("z", pos.getZ());
                            try {
                                te.load(state, nbt);
                                te.mirror(mirror);
                                te.rotate(rotation);
                            } catch (Exception e) {
                                Logger.warn("Failed to load TileEntity data for " + state + " @ " + pos);
                                Logger.error(e);
                            }
                        }
                    }
                }

                context.fireEvent("블록 설치중..", (double) (++current) / totalBlocks);
            }
        }
    }

    private void updateBlocks(PlacingContext context) {
        long current = 0;
        long totalBlocks = blocks.size();

        for (BlockData block : blocks) {
            BlockPos pos = Template.calculateRelativePosition(context.placement, block.pos)
                    .offset(context.posStart);
            BlockState state = context.world.getBlockState(pos);

            if (state.hasProperty(DoorBlock.HALF) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                continue;
            }

            BlockState updatedState = Block.updateFromNeighbourShapes(state, context.world, pos);
            if (state != updatedState) {
                context.world.setBlock(pos, updatedState, Constants.BlockFlags.BLOCK_UPDATE & -2 | 16);
            }
            context.world.blockUpdated(pos, updatedState.getBlock());

            if (block.nbt != null) {
                TileEntity te = context.world.getBlockEntity(pos);
                if (te != null) {
                    te.setChanged();
                }
            }

            context.fireEvent("설치한 Block 갱신중..", (double) (++current) / totalBlocks);
        }
    }

    private void placeEntities(PlacingContext context) {
        if (context.placement.isIgnoreEntities()) {
            return;
        }
        for (int i = 0; i < entities.size(); i++) {
            placeEntityAt(context.world, context.posStart, context.placement, i);
        }
    }

    public void placeEntityAt(World world, BlockPos posStart, PlacementSettings placement, int index) {
        Mirror mirror = placement.getMirror();
        Rotation rotation = placement.getRotation();

        CompoundNBT tag = this.entities.get(index);
        Vector3d relativePos = EntityUtils.readEntityPositionFromTag(tag);
        if (relativePos == null) {
            Logger.warn("Can't find position from entity tag: " + tag);
            return;
        }

        Vector3d transformedRelativePos = EntityUtils.getTransformedPosition(relativePos, mirror, rotation);
        Vector3d realPos = transformedRelativePos.add(posStart.getX(), posStart.getY(), posStart.getZ());
        Entity entity = EntityUtils.createEntityAndPassengersFromNBT(tag, world);

        if (entity != null) {
            float rotationYaw = entity.mirror(mirror);
            rotationYaw = rotationYaw + (entity.yRot - entity.rotate(rotation));
            entity.moveTo(realPos.x, realPos.y, realPos.z, rotationYaw, entity.xRot);
            EntityUtils.spawnEntityAndPassengersInWorld(entity, world);
        }
    }

    public BlockPos getSize() {
        return size;
    }


    private static class PlacingContext {
        private final World world;
        private final BlockPos posStart;
        private final PlacementSettings placement;
        private final IProgressEvent event;
        private double eventMin, eventMax;

        private PlacingContext(World world, BlockPos posStart, PlacementSettings placement, IProgressEvent event) {
            this.world = world;
            this.posStart = posStart;
            this.placement = placement;
            this.event = event;
            this.eventMin = 0;
            this.eventMax = 1;
        }

        public void setEventProgressRange(double min, double max) {
            this.eventMin = min;
            this.eventMax = max;
        }

        public void fireEvent(String status, double progress) {
            IProgressEvent.safeFire(event, status, eventMin + progress * (eventMax - eventMin));
        }
    }

    private static class BlockData {
        private final BlockPos pos;
        private final CompoundNBT nbt;
        private final BlockState state;

        private BlockData(BlockPos pos, BlockState state, CompoundNBT nbt) {
            this.pos = pos;
            this.state = state;
            this.nbt = nbt;
        }
    }

    private static class BlockStroage implements Iterable<BlockData> {
        private final List<BlockData> plainBlocks = Lists.newArrayList();
        private final List<BlockData> tileBlocks = Lists.newArrayList();
        private final List<BlockData> specialBlocks = Lists.newArrayList();

        public void addBlock(BlockPos pos, BlockState state, CompoundNBT tileNbt) {
            BlockData tempBlock = new BlockData(pos, state, tileNbt);
            if (tempBlock.nbt != null) {
                tileBlocks.add(tempBlock);
            } else if (!state.getBlock().hasDynamicShape() && state.isCollisionShapeFullBlock(EmptyBlockReader.INSTANCE, BlockPos.ZERO)) {
                plainBlocks.add(tempBlock);
            } else {
                specialBlocks.add(tempBlock);
            }
        }

        public long size() {
            return (long) plainBlocks.size() + tileBlocks.size() + specialBlocks.size();
        }

        @Override
        public Iterator<BlockData> iterator() {
            return Iterables.concat(plainBlocks, specialBlocks, tileBlocks).iterator();
        }
    }
}
