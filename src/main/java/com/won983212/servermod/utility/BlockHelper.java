package com.won983212.servermod.utility;

import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraftforge.common.IPlantable;

import javax.annotation.Nullable;

public class BlockHelper {
    private static void placeRailWithoutUpdate(World world, BlockState state, BlockPos target) {
        int i = target.getX() & 15;
        int j = target.getY();
        int k = target.getZ() & 15;
        Chunk chunk = world.getChunkAt(target);
        ChunkSection chunksection = chunk.getSections()[j >> 4];
        if (chunksection == Chunk.EMPTY_SECTION) {
            chunksection = new ChunkSection(j >> 4 << 4);
            chunk.getSections()[j >> 4] = chunksection;
        }
        BlockState old = chunksection.setBlockState(i, j & 15, k, state);
        chunk.markUnsaved();
        world.markAndNotifyBlock(target, chunk, old, state, 82, 512);

        world.setBlock(target, state, 82);
        world.neighborChanged(target, world.getBlockState(target.below()).getBlock(), target.below());
    }

    public static void placeSchematicBlock(World world, BlockState state, BlockPos target, ItemStack stack,
                                           @Nullable CompoundNBT data) {
        // Piston
        if (state.hasProperty(BlockStateProperties.EXTENDED))
            state = state.setValue(BlockStateProperties.EXTENDED, Boolean.FALSE);
        if (state.hasProperty(BlockStateProperties.WATERLOGGED))
            state = state.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE);

        if (state.getBlock() == Blocks.COMPOSTER)
            state = Blocks.COMPOSTER.defaultBlockState();
        else if (state.getBlock() != Blocks.SEA_PICKLE && state.getBlock() instanceof IPlantable)
            state = ((IPlantable) state.getBlock()).getPlant(world, target);

        if (world.dimensionType()
                .ultraWarm()
                && state.getFluidState()
                .getType()
                .is(FluidTags.WATER)) {
            int i = target.getX();
            int j = target.getY();
            int k = target.getZ();
            world.playSound(null, target, SoundEvents.FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
                    2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);

            for (int l = 0; l < 8; ++l) {
                world.addParticle(ParticleTypes.LARGE_SMOKE, i + Math.random(), j + Math.random(), k + Math.random(),
                        0.0D, 0.0D, 0.0D);
            }
            Block.dropResources(state, world, target);
            return;
        }

        if (state.getBlock() instanceof AbstractRailBlock) {
            placeRailWithoutUpdate(world, state, target);
        } else {
            world.setBlock(target, state, 18);
        }

        if (data != null) {
            TileEntity tile = world.getBlockEntity(target);
            if (tile != null) {
                data.putInt("x", target.getX());
                data.putInt("y", target.getY());
                data.putInt("z", target.getZ());
                tile.load(tile.getBlockState(), data);
            }
        }

        try {
            state.getBlock()
                    .setPlacedBy(world, target, state, null, stack);
        } catch (Exception ignored) {
        }
    }

    public static boolean shouldDeferBlock(BlockState state) {
        Block block = state.getBlock();
        if (state.hasProperty(BlockStateProperties.HANGING))
            return true;

        if (block instanceof LadderBlock)
            return true;
        if (block instanceof TorchBlock)
            return true;
        if (block instanceof AbstractSignBlock)
            return true;
        if (block instanceof AbstractPressurePlateBlock)
            return true;
        if (block instanceof HorizontalFaceBlock && !(block instanceof GrindstoneBlock))
            return true;
        if (block instanceof AbstractRailBlock)
            return true;
        if (block instanceof RedstoneDiodeBlock)
            return true;
        if (block instanceof RedstoneWireBlock)
            return true;
        return block instanceof CarpetBlock;
    }

    public static BlockState setZeroAge(BlockState blockState) {
        if (blockState.hasProperty(BlockStateProperties.AGE_1))
            return blockState.setValue(BlockStateProperties.AGE_1, 0);
        if (blockState.hasProperty(BlockStateProperties.AGE_2))
            return blockState.setValue(BlockStateProperties.AGE_2, 0);
        if (blockState.hasProperty(BlockStateProperties.AGE_3))
            return blockState.setValue(BlockStateProperties.AGE_3, 0);
        if (blockState.hasProperty(BlockStateProperties.AGE_5))
            return blockState.setValue(BlockStateProperties.AGE_5, 0);
        if (blockState.hasProperty(BlockStateProperties.AGE_7))
            return blockState.setValue(BlockStateProperties.AGE_7, 0);
        if (blockState.hasProperty(BlockStateProperties.AGE_15))
            return blockState.setValue(BlockStateProperties.AGE_15, 0);
        if (blockState.hasProperty(BlockStateProperties.AGE_25))
            return blockState.setValue(BlockStateProperties.AGE_25, 0);
        if (blockState.hasProperty(BlockStateProperties.LEVEL_HONEY))
            return blockState.setValue(BlockStateProperties.LEVEL_HONEY, 0);
        if (blockState.hasProperty(BlockStateProperties.HATCH))
            return blockState.setValue(BlockStateProperties.HATCH, 0);
        if (blockState.hasProperty(BlockStateProperties.STAGE))
            return blockState.setValue(BlockStateProperties.STAGE, 0);
        if (blockState.hasProperty(BlockStateProperties.LEVEL_CAULDRON))
            return blockState.setValue(BlockStateProperties.LEVEL_CAULDRON, 0);
        if (blockState.hasProperty(BlockStateProperties.LEVEL_COMPOSTER))
            return blockState.setValue(BlockStateProperties.LEVEL_COMPOSTER, 0);
        if (blockState.hasProperty(BlockStateProperties.EXTENDED))
            return blockState.setValue(BlockStateProperties.EXTENDED, false);
        return blockState;
    }
}
