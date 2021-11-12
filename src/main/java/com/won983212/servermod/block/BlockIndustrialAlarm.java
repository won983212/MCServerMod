package com.won983212.servermod.block;

import com.won983212.servermod.block.attribute.Attributes;
import com.won983212.servermod.client.VoxelShapeUtils;
import com.won983212.servermod.tile.TileEntityIndustrialAlarm;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockIndustrialAlarm extends Block {
    private static final VoxelShape[] MIN_SHAPES = new VoxelShape[VoxelShapeUtils.DIRECTIONS.length];

    static {
        VoxelShapeUtils.setShape(makeCuboidShape(5, 11, 5, 11, 16, 11), MIN_SHAPES, true);
    }

    public BlockIndustrialAlarm() {
        super(AbstractBlock.Properties.create(Material.GLASS).hardnessAndResistance(2, 2.4F));
        setDefaultState(Attributes.ACTIVE.getDefaultState(this.getStateContainer().getBaseState()));
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TileEntityIndustrialAlarm();
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return Attributes.FACING.set(this.getDefaultState(), context.getFace());
    }

    @Override
    protected void fillStateContainer(@Nonnull StateContainer.Builder<Block, BlockState> builder) {
        Attributes.fillStateContainer(builder);
    }

    @Override
    public BlockRenderType getRenderType(@Nonnull BlockState iBlockState) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull IBlockReader worldIn, @Nonnull BlockPos pos, @Nonnull ISelectionContext context) {
        return MIN_SHAPES[Attributes.FACING.get(state).getIndex()];
    }

    @Override
    public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity placer, @Nonnull ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityIndustrialAlarm) {
                ((TileEntityIndustrialAlarm) te).onAdded();
            }
        }
    }

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Block blockIn,
                                @Nonnull BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, blockIn, neighborPos, isMoving);
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityIndustrialAlarm) {
                ((TileEntityIndustrialAlarm) te).onNeighborChange(state, neighborPos);
            }
        }
    }
}