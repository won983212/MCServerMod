package com.won983212.servermod.schematic;

import com.won983212.servermod.item.SchematicItem;
import com.won983212.servermod.schematic.world.SchematicWorld;
import com.won983212.servermod.utility.BlockHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.state.properties.BedPart;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class SchematicPrinter {

    public enum PrintStage {
        BLOCKS, DEFERRED_BLOCKS, ENTITIES
    }

    private boolean schematicLoaded;
    private SchematicWorld blockReader;
    private BlockPos schematicAnchor;

    private BlockPos currentPos;
    private int printingEntityIndex;
    private PrintStage printStage;
    private final List<BlockPos> deferredBlocks;

    public SchematicPrinter() {
        printingEntityIndex = -1;
        printStage = PrintStage.BLOCKS;
        deferredBlocks = new LinkedList<>();
    }

    public void loadSchematic(ItemStack blueprint, World originalWorld, boolean processNBT, IProgressEvent event) {
        if (!blueprint.hasTag() || !blueprint.getTag().getBoolean("Deployed")) {
            return;
        }

        Template activeTemplate = SchematicItem.loadSchematic(blueprint, (s, p) -> event.onProgress(s, p * 0.3));
        PlacementSettings settings = SchematicItem.getSettings(blueprint, processNBT);

        schematicAnchor = NBTUtil.readBlockPos(blueprint.getTag().getCompound("Anchor"));
        blockReader = new SchematicWorld(schematicAnchor, originalWorld);
        
        final long totalSize = (long) activeTemplate.size.getX() * activeTemplate.size.getY() * activeTemplate.size.getZ();
        blockReader.setBlockCountProgressEvent((s, p) -> event.onProgress(s, 0.3 + 0.7 * p / totalSize));
        activeTemplate.placeInWorldChunk(blockReader, schematicAnchor, settings, blockReader.getRandom());
        blockReader.setBlockCountProgressEvent(null);

        BlockPos extraBounds = Template.calculateRelativePosition(settings, activeTemplate.getSize().offset(-1, -1, -1));
        blockReader.getBounds().expand(new MutableBoundingBox(extraBounds, extraBounds));

        printingEntityIndex = -1;
        printStage = PrintStage.BLOCKS;
        deferredBlocks.clear();
        MutableBoundingBox bounds = blockReader.getBounds();
        currentPos = new BlockPos(bounds.x0 - 1, bounds.y0, bounds.z0);
        schematicLoaded = true;
    }

    public boolean isLoaded() {
        return schematicLoaded;
    }

    public BlockPos getCurrentTarget() {
        if (!isLoaded()) {
            return null;
        }
        return schematicAnchor.offset(currentPos);
    }

    @FunctionalInterface
    public interface BlockTargetHandler {
        void handle(BlockPos target, BlockState blockState, TileEntity tileEntity);
    }

    @FunctionalInterface
    public interface EntityTargetHandler {
        void handle(BlockPos target, Entity entity);
    }

    public void handleCurrentTarget(BlockTargetHandler blockHandler, EntityTargetHandler entityHandler) {
        BlockPos target = getCurrentTarget();

        if (printStage == PrintStage.ENTITIES) {
            Entity entity = blockReader.getEntities()
                    .collect(Collectors.toList())
                    .get(printingEntityIndex);
            entityHandler.handle(target, entity);
        } else {
            BlockState blockState = blockReader.getBlockState(target);
            TileEntity tileEntity = blockReader.getBlockEntity(target);
            blockHandler.handle(target, blockState, tileEntity);
        }
    }

    @FunctionalInterface
    public interface PlacementPredicate {
        boolean shouldPlace(BlockPos target, BlockState blockState, TileEntity tileEntity,
                            BlockState toReplace, BlockState toReplaceOther, boolean isNormalCube);
    }

    public boolean shouldPlaceCurrent(World world) {
        return shouldPlaceCurrent(world, (a, b, c, d, e, f) -> true);
    }

    public boolean shouldPlaceCurrent(World world, PlacementPredicate predicate) {
        if (world == null) {
            return false;
        }

        if (printStage == PrintStage.ENTITIES) {
            return true;
        }

        return shouldPlaceBlock(world, predicate, getCurrentTarget());
    }

    public boolean shouldPlaceBlock(World world, PlacementPredicate predicate, BlockPos pos) {
        BlockState state = blockReader.getBlockState(pos);
        TileEntity tileEntity = blockReader.getBlockEntity(pos);

        BlockState toReplace = world.getBlockState(pos);
        BlockState toReplaceOther = null;
        if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT) {
            toReplaceOther = world.getBlockState(pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
        }
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            toReplaceOther = world.getBlockState(pos.above());
        }

        if (!world.isLoaded(pos)) {
            return false;
        }
        if (toReplace.getDestroySpeed(world, pos) == -1 || (toReplaceOther != null && toReplaceOther.getDestroySpeed(world, pos) == -1)) {
            return false;
        }

        boolean isNormalCube = state.isRedstoneConductor(blockReader, currentPos);
        return predicate.shouldPlace(pos, state, tileEntity, toReplace, toReplaceOther, isNormalCube);
    }

    public boolean advanceCurrentPos() {
        List<Entity> entities = blockReader.getEntities().collect(Collectors.toList());

        do {
            if (printStage == PrintStage.BLOCKS) {
                while (tryAdvanceCurrentPos()) {
                    deferredBlocks.add(currentPos);
                }
            }

            if (printStage == PrintStage.DEFERRED_BLOCKS) {
                if (deferredBlocks.isEmpty()) {
                    printStage = PrintStage.ENTITIES;
                } else {
                    currentPos = deferredBlocks.remove(0);
                }
            }

            if (printStage == PrintStage.ENTITIES) {
                if (printingEntityIndex + 1 < entities.size()) {
                    printingEntityIndex++;
                    currentPos = entities.get(printingEntityIndex).blockPosition().subtract(schematicAnchor);
                } else {
                    // Reached end of printing
                    return false;
                }
            }
        } while (!blockReader.getBounds().isInside(currentPos));

        // More things available to print
        return true;
    }

    public boolean tryAdvanceCurrentPos() {
        currentPos = currentPos.relative(Direction.EAST);
        MutableBoundingBox bounds = blockReader.getBounds();
        BlockPos posInBounds = currentPos.offset(-bounds.x0, -bounds.y0, -bounds.z0);

        if (posInBounds.getX() > bounds.getXSpan()) {
            currentPos = new BlockPos(bounds.x0, currentPos.getY(), currentPos.getZ() + 1).west();
        }
        if (posInBounds.getZ() > bounds.getZSpan()) {
            currentPos = new BlockPos(currentPos.getX(), currentPos.getY() + 1, bounds.z0).west();
        }

        // End of blocks reached
        if (currentPos.getY() > bounds.getYSpan()) {
            printStage = PrintStage.DEFERRED_BLOCKS;
            return false;
        }

        return BlockHelper.shouldDeferBlock(blockReader.getBlockState(getCurrentTarget()));
    }
}
