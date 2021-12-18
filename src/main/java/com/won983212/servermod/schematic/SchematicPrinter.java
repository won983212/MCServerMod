package com.won983212.servermod.schematic;

import com.won983212.servermod.Logger;
import com.won983212.servermod.item.SchematicItem;
import com.won983212.servermod.schematic.world.SchematicWorld;
import com.won983212.servermod.task.IAsyncTask;
import com.won983212.servermod.utility.BlockHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.common.util.Constants;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SchematicPrinter implements IAsyncTask {

    public enum PrintStage {
        LOADING, ERROR, BLOCKS, DEFERRED_BLOCKS, ENTITIES
    }

    public static final int BATCH_COUNT = 20 * 20 * 20;

    private final boolean isIncludeAir;
    private SchematicWorld blockReader;
    private BlockPos schematicAnchor;

    private BlockPos currentPos;
    private int printingEntityIndex;
    private volatile PrintStage printStage;
    private final List<BlockPos> deferredBlocks;

    private World originalWorld;
    private IProgressEvent event;
    private long processed = 0;
    private long total = 0;


    public SchematicPrinter(boolean isIncludeAir) {
        this.printingEntityIndex = -1;
        this.printStage = PrintStage.LOADING;
        this.deferredBlocks = new LinkedList<>();
        this.isIncludeAir = isIncludeAir;
    }

    public CompletableFuture<Boolean> loadSchematicAsync(ItemStack blueprint, World originalWorld, boolean processNBT, IProgressEvent event) {
        if (!blueprint.hasTag() || !blueprint.getTag().getBoolean("Deployed")) {
            return CompletableFuture.completedFuture(null);
        }

        this.event = event;
        this.originalWorld = originalWorld;

        return CompletableFuture.supplyAsync(() -> SchematicItem.loadSchematic(blueprint, (s, p) -> IProgressEvent.safeFire(event, s, p * 0.2)))
                .thenApply((t) -> load(t, processNBT, blueprint))
                .exceptionally((e) -> {
                    Logger.error(e);
                    printStage = PrintStage.ERROR;
                    return false;
                });
    }

    private boolean load(SchematicContainer activeTemplate, boolean processNBT, ItemStack blueprint) {
        PlacementSettings settings = SchematicItem.getSettings(blueprint, processNBT);
        BlockPos size = activeTemplate.getSize();
        this.total = (long) size.getX() * size.getY() * size.getZ();

        schematicAnchor = NBTUtil.readBlockPos(blueprint.getTag().getCompound("Anchor"));
        blockReader = new SchematicWorld(schematicAnchor, originalWorld);

        activeTemplate.placeSchematicToWorld(blockReader, schematicAnchor, settings,
                (s, p) -> IProgressEvent.safeFire(event, s, 0.2 + 0.3 * p));

        BlockPos extraBounds = Template.calculateRelativePosition(settings, activeTemplate.getSize().offset(-1, -1, -1));
        blockReader.getBounds().expand(new MutableBoundingBox(extraBounds, extraBounds));

        printingEntityIndex = -1;
        printStage = PrintStage.BLOCKS;
        deferredBlocks.clear();
        MutableBoundingBox bounds = blockReader.getBounds();
        currentPos = new BlockPos(bounds.x0 - 1, bounds.y0, bounds.z0);
        return true;
    }

    public boolean tick() {
        if (isUnloaded()) {
            return printStage == PrintStage.LOADING;
        }

        int count = 0;
        boolean end = true;
        while (count < BATCH_COUNT && (end = advanceCurrentPos())) {
            if (!shouldPlaceCurrent(originalWorld)) {
                continue;
            }

            processed++;
            count++;
            BlockPos target = getCurrentTarget();
            if (printStage == PrintStage.ENTITIES) {
                Entity entity = blockReader.getEntities().get(printingEntityIndex);
                originalWorld.addFreshEntity(entity);
            } else {
                BlockState blockState = blockReader.getBlockState(target);
                TileEntity tileEntity = blockReader.getBlockEntity(target);
                boolean placingAir = blockState.getBlock().isAir(blockState, originalWorld, target);
                if (placingAir && !isIncludeAir) {
                    continue;
                }
                CompoundNBT tileData = tileEntity != null ? tileEntity.save(new CompoundNBT()) : null;
                BlockHelper.placeSchematicBlock(originalWorld, blockState, target, null, tileData);
            }
        }
        
        IProgressEvent.safeFire(event, "월드에 블록 설치중...", Math.min(0.5 + 0.5 * processed / total, 1.0));
        return end;
    }

    public boolean isUnloaded() {
        return printStage == PrintStage.LOADING || printStage == PrintStage.ERROR;
    }

    public BlockPos getCurrentTarget() {
        if (isUnloaded()) {
            return null;
        }
        return schematicAnchor.offset(currentPos);
    }

    private boolean shouldPlaceCurrent(World world) {
        if (world == null) {
            return false;
        }

        if (printStage == PrintStage.ENTITIES) {
            return true;
        }

        return !World.isOutsideBuildHeight(getCurrentTarget());
    }

    private boolean advanceCurrentPos() {
        List<Entity> entities = blockReader.getEntities();

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

    private boolean tryAdvanceCurrentPos() {
        currentPos = currentPos.relative(Direction.EAST);
        MutableBoundingBox bounds = blockReader.getBounds();

        if (currentPos.getX() > bounds.getXSpan() + bounds.x0) {
            currentPos = new BlockPos(bounds.x0, currentPos.getY(), currentPos.getZ() + 1).west();
        }

        if (currentPos.getZ() > bounds.getZSpan() + bounds.z0) {
            currentPos = new BlockPos(currentPos.getX(), currentPos.getY() + 1, bounds.z0).west();
        }

        // End of blocks reached
        if (currentPos.getY() > bounds.getYSpan() + bounds.y0 || World.isOutsideBuildHeight(currentPos.getY())) {
            printStage = PrintStage.DEFERRED_BLOCKS;
            return false;
        }

        return BlockHelper.shouldDeferBlock(blockReader.getBlockState(getCurrentTarget()));
    }
}
