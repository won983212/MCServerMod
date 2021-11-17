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
        VoxelShapeUtils.setShape(box(5, 11, 5, 11, 16, 11), MIN_SHAPES, true);
    }

    public BlockIndustrialAlarm() {
        super(AbstractBlock.Properties.of(Material.GLASS).strength(2, 2.4F));
        registerDefaultState(Attributes.ACTIVE.getDefaultState(this.getStateDefinition().any()));
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
        return Attributes.FACING.set(this.defaultBlockState(), context.getClickedFace());
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateContainer.Builder<Block, BlockState> builder) {
        Attributes.fillStateContainer(builder);
    }

    @Override
    public BlockRenderType getRenderShape(@Nonnull BlockState iBlockState) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull IBlockReader worldIn, @Nonnull BlockPos pos, @Nonnull ISelectionContext context) {
        return MIN_SHAPES[Attributes.FACING.get(state).get3DDataValue()];
    }

    @Override
    public void setPlacedBy(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity placer, @Nonnull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide) {
            TileEntity te = world.getBlockEntity(pos);
            if (te instanceof TileEntityIndustrialAlarm) {
                ((TileEntityIndustrialAlarm) te).onAdded();
            }
        }
    }

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Block blockIn,
                                @Nonnull BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, blockIn, neighborPos, isMoving);
        if (!world.isClientSide) {
            TileEntity te = world.getBlockEntity(pos);
            if (te instanceof TileEntityIndustrialAlarm) {
                ((TileEntityIndustrialAlarm) te).onNeighborChange(state, neighborPos);
            }
        }
    }
}