package com.won983212.servermod.schematic.parser.container;

import com.won983212.servermod.Logger;
import com.won983212.servermod.utility.EntityUtils;
import com.won983212.servermod.utility.NBTUtils;
import com.won983212.servermod.utility.PositionUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.BitSetVoxelShapePart;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchematicContainer {
    private LitematicaBlockStateContainer blocks;
    private final Map<BlockPos, CompoundNBT> tiles = new HashMap<>();
    private final List<CompoundNBT> entities = new ArrayList<>();
    private BlockPos size = BlockPos.ZERO;


    public void resizeBlockContainer(BlockPos size) {
        this.blocks = new LitematicaBlockStateContainer(size.getX(), size.getY(), size.getZ());
        this.tiles.clear();
        this.size = size;
    }

    public void addBlock(BlockPos pos, BlockState state, CompoundNBT tileNbt) {
        blocks.set(pos.getX(), pos.getY(), pos.getZ(), state);
        if (tileNbt != null) {
            tiles.put(pos, tileNbt);
        }
    }

    public void addEntity(CompoundNBT entityNbt) {
        entities.add(entityNbt);
    }

    public void placeSchematicToWorld(World world, BlockPos posStart, PlacementSettings placement) {
        placeSchematicToWorld(world, posStart, placement, Constants.BlockFlags.BLOCK_UPDATE);
    }

    // TODO 설치할 때 조심해서 설치해야지
    public void placeSchematicToWorld(World world, BlockPos posStart, PlacementSettings placement, int setBlockStateFlags) {
        final int width = this.size.getX();
        final int height = this.size.getY();
        final int length = this.size.getZ();
        final int numBlocks = width * height * length;
        BitSetVoxelShapePart voxelshapepart = new BitSetVoxelShapePart(width, height, length);

        if (this.blocks != null && numBlocks > 0 && this.blocks.getSize().equals(this.size)) {
            final Rotation rotation = placement.getRotation();
            final Mirror mirror = placement.getMirror();

            // Place blocks and read any TileEntity data
            for (int y = 0; y < height; ++y) {
                for (int z = 0; z < length; ++z) {
                    for (int x = 0; x < width; ++x) {
                        BlockState state = this.blocks.get(x, y, z);
                        BlockPos pos = new BlockPos(x, y, z);
                        CompoundNBT teNBT = this.tiles.get(pos);

                        pos = Template.calculateRelativePosition(placement, pos).offset(posStart);
                        state = state.mirror(mirror);
                        state = state.rotate(rotation);

                        if (teNBT != null) {
                            TileEntity te = world.getBlockEntity(pos);
                            if (te != null) {
                                if (te instanceof IInventory) {
                                    ((IInventory) te).clearContent();
                                }
                                world.setBlock(pos, Blocks.BARRIER.defaultBlockState(), Constants.BlockFlags.UPDATE_NEIGHBORS | Constants.BlockFlags.NO_RERENDER);
                            }
                        }

                        if (world.setBlock(pos, state, setBlockStateFlags)) {
                            voxelshapepart.setFull(x, y, z, true, true);
                            if (teNBT != null) {
                                TileEntity te = world.getBlockEntity(pos);
                                if (te != null) {
                                    teNBT.putInt("x", pos.getX());
                                    teNBT.putInt("y", pos.getY());
                                    teNBT.putInt("z", pos.getZ());
                                    try {
                                        te.load(state, teNBT);
                                        te.mirror(mirror);
                                        te.rotate(rotation);
                                    } catch (Exception e) {
                                        Logger.warn("Failed to load TileEntity data for " + state + " @ " + pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            int xMin = voxelshapepart.firstFull(Direction.Axis.X);
            int yMin = voxelshapepart.firstFull(Direction.Axis.Y);
            int zMin = voxelshapepart.firstFull(Direction.Axis.Z);
            Template.updateShapeAtEdge(world, setBlockStateFlags, voxelshapepart, xMin, yMin, zMin);

            if ((setBlockStateFlags & Constants.BlockFlags.BLOCK_UPDATE) != 0) {
                for (int y = 0; y < height; ++y) {
                    for (int z = 0; z < length; ++z) {
                        for (int x = 0; x < width; ++x) {
                            BlockPos pos = new BlockPos(x, y, z);
                            CompoundNBT teNBT = this.tiles.get(pos);

                            pos = Template.calculateRelativePosition(placement, pos).offset(posStart);
                            BlockState state = world.getBlockState(pos);
                            BlockState updatedState = Block.updateFromNeighbourShapes(state, world, pos);

                            if (state != updatedState) {
                                world.setBlock(pos, updatedState, setBlockStateFlags & -2 | 16);
                            }
                            world.blockUpdated(pos, updatedState.getBlock());

                            if (teNBT != null) {
                                TileEntity te = world.getBlockEntity(pos);
                                if (te != null) {
                                    te.setChanged();
                                }
                            }
                        }
                    }
                }
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
            Vector3d relativePos = NBTUtils.readEntityPositionFromTag(tag);
            Vector3d transformedRelativePos = PositionUtils.getTransformedPosition(relativePos, mirror, rotation);
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
}
