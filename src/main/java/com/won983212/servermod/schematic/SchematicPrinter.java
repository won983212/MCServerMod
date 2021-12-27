package com.won983212.servermod.schematic;

import com.won983212.servermod.Logger;
import com.won983212.servermod.item.SchematicItem;
import com.won983212.servermod.schematic.container.SchematicContainer;
import com.won983212.servermod.schematic.parser.SchematicFileParser;
import com.won983212.servermod.task.IAsyncNoResultTask;
import com.won983212.servermod.task.IAsyncTask;
import com.won983212.servermod.task.TaskScheduler;
import com.won983212.servermod.utility.EntityUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IClearable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.common.util.Constants;

// TODO 큰거 설치하면 랙 ㅈㄴ걸림;; (async화 이후 해결)
public class SchematicPrinter implements IAsyncNoResultTask {

    private enum PrintStage {
        ERROR, LOAD_SCHEMATIC, BLOCKS, UPDATING, ENTITIES
    }

    public static final int BATCH_COUNT = 5000;

    private boolean isIncludeAir;
    private SchematicContainer blockReader;
    private BlockPos schematicAnchor;
    private PlacementSettings settings;
    private PrintStage printStage;

    private final World world;
    private final IProgressEvent event;
    private final BlockPos.Mutable current;
    private long processed;
    private long total = 0;

    private ItemStack blueprint;
    private IAsyncTask<SchematicContainer> schematicLoadingTask;


    private SchematicPrinter(World world, IProgressEvent event) {
        this.world = world;
        this.event = event;
        this.printStage = PrintStage.LOAD_SCHEMATIC;
        this.current = new BlockPos.Mutable(0, 0, 0);
        this.processed = 0;
    }

    public static SchematicPrinter newPlacingSchematicTask(SchematicContainer schematic, World world, BlockPos posStart,
                                                           PlacementSettings placement, IProgressEvent event) {
        SchematicPrinter printer = new SchematicPrinter(world, event);
        printer.schematicAnchor = posStart;
        printer.settings = placement;
        printer.blockReader = schematic;

        BlockPos size = schematic.getSize();
        printer.total = (long) size.getX() * size.getY() * size.getZ();
        printer.printStage = PrintStage.BLOCKS;
        return printer;
    }

    public static SchematicPrinter newPlacingSchematicTask(ItemStack blueprint, World world, IProgressEvent event) {
        if (!blueprint.hasTag() || !blueprint.getTag().getBoolean("Deployed")) {
            throw new IllegalArgumentException("Can't place it. Blueprint hasn't tag or not deployed");
        }

        SchematicPrinter printer = new SchematicPrinter(world, event);
        printer.schematicAnchor = NBTUtil.readBlockPos(blueprint.getTag().getCompound("Anchor"));
        printer.settings = SchematicItem.getSettings(blueprint);
        printer.blueprint = blueprint;
        printer.blockReader = null;
        return printer;
    }

    @Override
    public boolean tick() {
        if (printStage == PrintStage.ERROR) {
            return false;
        } else if (printStage == PrintStage.LOAD_SCHEMATIC) {
            if (schematicLoadingTask == null) {
                loadSchematicAsync();
            }
            return true;
        } else if (printStage == PrintStage.BLOCKS || printStage == PrintStage.UPDATING) {
            boolean continued = true;
            for (int i = 0; i < BATCH_COUNT && (continued = processed < total); i++) {
                if (printStage == PrintStage.BLOCKS) {
                    placeBlock();
                    IProgressEvent.safeFire(event, "블록 설치중...", Math.min(0.6 * processed / total, 1.0));
                } else if (printStage == PrintStage.UPDATING) {
                    updateBlock();
                    IProgressEvent.safeFire(event, "블록 업데이트중...", Math.min(0.6 + 0.3 * processed / total, 1.0));
                }
                next();
            }
            if (!continued) {
                printStage = PrintStage.values()[printStage.ordinal() + 1];
                processed = 0;
            }
        } else if (printStage == PrintStage.ENTITIES) {
            IProgressEvent.safeFire(event, "엔티디 추가하는 중...", 0.9);
            placeEntities();
            IProgressEvent.safeFire(event, "엔티디 추가하는 중...", 1);
            return false;
        }

        return true;
    }

    public SchematicPrinter includeAir(boolean include) {
        this.isIncludeAir = include;
        return this;
    }

    private void loadSchematicAsync() {
        schematicLoadingTask = SchematicFileParser.parseSchematicFromItemAsync(blueprint, (s, p) -> IProgressEvent.safeFire(event, s, p * 0.2));
        TaskScheduler.addAsyncTask(schematicLoadingTask)
                .thenAccept((c) -> {
                    BlockPos size = c.getSize();
                    this.total = (long) size.getX() * size.getY() * size.getZ();
                    this.blockReader = c;
                    this.printStage = PrintStage.BLOCKS;
                });
    }

    private void next() {
        processed++;
        BlockPos size = blockReader.getSize();
        int stride = size.getX() * size.getZ();
        this.current.setX((int) (processed % size.getX()));
        this.current.setY((int) (processed / stride));
        this.current.setZ((int) ((processed - current.getY() * stride) / size.getX()));
    }

    private void placeBlock() {
        final Rotation rotation = settings.getRotation();
        final Mirror mirror = settings.getMirror();

        BlockState state = blockReader.getBlockAt(this.current);
        if (state == SchematicContainer.AIR_BLOCK_STATE && !isIncludeAir) {
            return;
        }

        BlockPos pos = Template.calculateRelativePosition(settings, this.current)
                .offset(schematicAnchor);
        CompoundNBT tag = blockReader.getTileTagAt(this.current);

        state = fixBlockState(pos, state);
        state = state.mirror(mirror);
        state = state.rotate(rotation);

        if (tag != null) {
            TileEntity te = world.getBlockEntity(pos);
            if (te != null) {
                IClearable.tryClear(te);
                world.setBlock(pos, Blocks.BARRIER.defaultBlockState(), Constants.BlockFlags.UPDATE_NEIGHBORS | Constants.BlockFlags.NO_RERENDER);
            }
        }

        if (world.setBlock(pos, state, Constants.BlockFlags.BLOCK_UPDATE | Constants.BlockFlags.UPDATE_NEIGHBORS)) {
            if (tag != null) {
                TileEntity te = world.getBlockEntity(pos);
                if (te != null) {
                    CompoundNBT nbt = tag.copy();
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
    }

    private void updateBlock() {
        BlockPos pos = Template.calculateRelativePosition(settings, this.current)
                .offset(schematicAnchor);
        BlockState state = world.getBlockState(pos);

        if (state == SchematicContainer.AIR_BLOCK_STATE) {
            return;
        }

        state = fixBlockState(pos, state);
        BlockState updatedState = Block.updateFromNeighbourShapes(state, world, pos);
        if (state != updatedState) {
            world.setBlock(pos, updatedState, Constants.BlockFlags.BLOCK_UPDATE & -2 | 16);
        }
        world.blockUpdated(pos, updatedState.getBlock());

        CompoundNBT tag = blockReader.getTileTagAt(pos);
        if (tag != null) {
            TileEntity te = world.getBlockEntity(pos);
            if (te != null) {
                te.setChanged();
            }
        }
    }

    private void placeEntities() {
        if (settings.isIgnoreEntities()) {
            return;
        }
        for (CompoundNBT tag : blockReader.getEntities()) {
            Mirror mirror = settings.getMirror();
            Rotation rotation = settings.getRotation();
            Vector3d relativePos = EntityUtils.readEntityPositionFromTag(tag);

            if (relativePos == null) {
                Logger.warn("Can't find position from entity tag: " + tag);
                return;
            }

            Vector3d transformedRelativePos = EntityUtils.getTransformedPosition(relativePos, mirror, rotation);
            Vector3d realPos = transformedRelativePos.add(schematicAnchor.getX(), schematicAnchor.getY(), schematicAnchor.getZ());
            Entity entity = EntityUtils.createEntityAndPassengersFromNBT(tag, world);

            if (entity != null) {
                float rotationYaw = entity.mirror(mirror);
                rotationYaw = rotationYaw + (entity.yRot - entity.rotate(rotation));
                entity.moveTo(realPos.x, realPos.y, realPos.z, rotationYaw, entity.xRot);
                EntityUtils.spawnEntityAndPassengersInWorld(entity, world);
            }
        }
    }

    private BlockState fixBlockState(BlockPos pos, BlockState state) {
        if (state.hasProperty(DoorBlock.HALF) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            BlockState lowerState = world.getBlockState(pos.below());
            if (lowerState.getBlock() instanceof DoorBlock) {
                return lowerState.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            }
        }
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return state.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE);
        }
        return state;
    }
}
