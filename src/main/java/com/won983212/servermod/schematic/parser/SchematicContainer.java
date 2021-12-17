package com.won983212.servermod.schematic.parser;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.IProgressEvent;
import com.won983212.servermod.utility.EntityUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;
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
import java.util.List;

// TODO 구조 개선하자
// TODO CompletableFuture 말고 async한 방법으로 만들자
public class SchematicContainer {
    private final List<BlockInfo> plainBlocks = Lists.newArrayList();
    private final List<BlockInfo> tileBlocks = Lists.newArrayList();
    private final List<BlockInfo> specialBlocks = Lists.newArrayList();
    private final List<CompoundNBT> entities = new ArrayList<>();
    private BlockPos size = BlockPos.ZERO;


    public void resizeBlockContainer(BlockPos size) {
        this.size = size;
    }

    public void addBlock(BlockPos pos, BlockState state, CompoundNBT tileNbt) {
        BlockInfo tempBlock = new BlockInfo(pos, state, tileNbt);
        if (tempBlock.nbt != null) {
            tileBlocks.add(tempBlock);
        } else if (!state.getBlock().hasDynamicShape() && state.isCollisionShapeFullBlock(EmptyBlockReader.INSTANCE, BlockPos.ZERO)) {
            plainBlocks.add(tempBlock);
        } else {
            specialBlocks.add(tempBlock);
        }
    }

    public void addEntity(CompoundNBT entityNbt) {
        entities.add(entityNbt);
    }

    public Iterable<BlockInfo> blocks() {
        return Iterables.concat(plainBlocks, specialBlocks, tileBlocks);
    }

    public void placeSchematicToWorld(World world, BlockPos posStart, PlacementSettings placement, IProgressEvent event) {
        placeSchematicToWorld(world, posStart, placement, event, Constants.BlockFlags.BLOCK_UPDATE);
    }

    public void placeSchematicToWorld(World world, BlockPos posStart, PlacementSettings placement, IProgressEvent event, int setBlockStateFlags) {
        final long numBlocks = (long) plainBlocks.size() + specialBlocks.size() + tileBlocks.size();
        long current = 0;

        if (numBlocks > 0) {
            final Rotation rotation = placement.getRotation();
            final Mirror mirror = placement.getMirror();

            for (BlockInfo block : blocks()) {
                BlockState state = block.state;
                BlockPos pos = Template.calculateRelativePosition(placement, block.pos).offset(posStart);

                state = state.mirror(mirror);
                state = state.rotate(rotation);

                if (block.nbt != null) {
                    TileEntity te = world.getBlockEntity(pos);
                    if (te != null) {
                        if (te instanceof IInventory) {
                            ((IInventory) te).clearContent();
                        }
                        world.setBlock(pos, Blocks.BARRIER.defaultBlockState(), Constants.BlockFlags.UPDATE_NEIGHBORS | Constants.BlockFlags.NO_RERENDER);
                    }
                }

                if (world.setBlock(pos, state, setBlockStateFlags)) {
                    if (block.nbt != null) {
                        TileEntity te = world.getBlockEntity(pos);
                        if (te != null) {
                            block.nbt.putInt("x", pos.getX());
                            block.nbt.putInt("y", pos.getY());
                            block.nbt.putInt("z", pos.getZ());
                            try {
                                te.load(state, block.nbt);
                                te.mirror(mirror);
                                te.rotate(rotation);
                            } catch (Exception e) {
                                Logger.warn("Failed to load TileEntity data for " + state + " @ " + pos);
                            }
                        }
                    }
                }

                if (event != null) {
                    event.onProgress("블록 설치중..", 0.8 * (++current) / numBlocks);
                }
            }

            current = 0;
            if ((setBlockStateFlags & Constants.BlockFlags.BLOCK_UPDATE) != 0) {
                for (BlockInfo ent : blocks()) {
                    BlockPos pos = Template.calculateRelativePosition(placement, ent.pos).offset(posStart);
                    BlockState state = world.getBlockState(pos);
                    BlockState updatedState = Block.updateFromNeighbourShapes(state, world, pos);

                    if (state != updatedState) {
                        world.setBlock(pos, updatedState, setBlockStateFlags & -2 | 16);
                    }
                    world.blockUpdated(pos, updatedState.getBlock());

                    if (ent.nbt != null) {
                        TileEntity te = world.getBlockEntity(pos);
                        if (te != null) {
                            te.setChanged();
                        }
                    }

                    if (event != null) {
                        event.onProgress("설치한 Block 갱신중..", 0.8 + 0.2 * (++current) / numBlocks);
                    }
                }
            } else if (event != null) {
                event.onProgress("설치한 Block 갱신중..", 1.0);
            }

            if (!placement.isIgnoreEntities()) {
                this.addEntitiesToWorld(world, posStart, placement);
            }
        }
    }

    private void addEntitiesToWorld(World world, BlockPos posStart, PlacementSettings placement) {
        Mirror mirror = placement.getMirror();
        Rotation rotation = placement.getRotation();

        for (CompoundNBT tag : this.entities) {
            Vector3d relativePos = EntityUtils.readEntityPositionFromTag(tag);
            if (relativePos == null) {
                Logger.warn("Can't find position from entity tag: " + tag);
                continue;
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
    }

    public BlockPos getSize() {
        return size;
    }

    private static class BlockInfo {
        private final BlockPos pos;
        private final CompoundNBT nbt;
        private final BlockState state;

        private BlockInfo(BlockPos pos, BlockState state, CompoundNBT nbt) {
            this.pos = pos;
            this.state = state;
            this.nbt = nbt;
        }
    }
}
