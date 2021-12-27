package com.won983212.servermod.schematic.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.Logger;
import com.won983212.servermod.client.render.SuperRenderTypeBuffer;
import com.won983212.servermod.schematic.IProgressEntry;
import com.won983212.servermod.schematic.IProgressEntryProducer;
import com.won983212.servermod.schematic.IProgressEvent;
import com.won983212.servermod.schematic.SchematicPrinter;
import com.won983212.servermod.schematic.client.render.SchematicRenderer;
import com.won983212.servermod.schematic.container.SchematicContainer;
import com.won983212.servermod.schematic.parser.SchematicFileParser;
import com.won983212.servermod.schematic.world.SchematicWorld;
import com.won983212.servermod.task.*;
import com.won983212.servermod.utility.animate.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Mirror;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.PlacementSettings;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SchematicRendererManager implements IProgressEntryProducer {

    private static final Cache<ItemStack, SchematicRenderer[]> rendererCache;
    private final ConcurrentHashMap<String, LoadingEntry> loadingEntries;
    private ItemStack currentStack;
    private SchematicRenderer[] renderers;

    static {
        rendererCache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build();
    }

    public SchematicRendererManager() {
        renderers = null;
        currentStack = null;
        loadingEntries = new ConcurrentHashMap<>();
    }

    public void render(MatrixStack ms, SuperRenderTypeBuffer buffer, SchematicTransformation transformation) {
        if (renderers != null) {
            float pt = AnimationTickHolder.getPartialTicks();
            boolean lr = transformation.getScaleLR().get(pt) < 0;
            boolean fb = transformation.getScaleFB().get(pt) < 0;
            if (lr && !fb) {
                renderers[2].render(ms, buffer, transformation);
            } else if (fb && !lr) {
                renderers[1].render(ms, buffer, transformation);
            } else {
                renderers[0].render(ms, buffer, transformation);
            }
        }
    }

    public void setCurrentSchematic(ItemStack activeSchematicItem) {
        currentStack = activeSchematicItem;
        if (activeSchematicItem == null) {
            this.renderers = null;
            return;
        }

        final String schematicFilePath = activeSchematicItem.getTag().getString("File");
        if (loadingEntries.containsKey(schematicFilePath)) {
            this.renderers = null;
            Logger.debug("Already loading file: " + schematicFilePath);
            return;
        }

        LoadingEntry loadingEntry = new LoadingEntry(schematicFilePath);
        loadingEntries.put(schematicFilePath, loadingEntry);

        this.renderers = rendererCache.getIfPresent(activeSchematicItem);
        if (renderers == null) {
            final IAsyncTask<SchematicContainer> task0 =
                    SchematicFileParser.parseSchematicFromItemAsync(activeSchematicItem, (s, p) -> loadingEntry.onProgress(s, 0.4 * p));
            TaskScheduler.addAsyncTask(task0)
                    .groupId(schematicFilePath.hashCode())
                    .exceptionally((e) -> {
                        Logger.error(e);
                        loadingEntries.remove(schematicFilePath);
                    })
                    .then((schematic) -> {
                        TaskScheduler.pushGroupIdContext(schematicFilePath.hashCode());
                        IAsyncTask<SchematicRenderer[]> task = startPreparingSchematicAsync(schematic, (s, p) -> loadingEntry.onProgress(s, 0.4 + 0.6 * p));
                        TaskScheduler.popGroupIdContext();
                        return task;
                    })
                    .thenAccept((newRenderers) -> {
                        loadingEntries.remove(schematicFilePath);
                        if (currentStack == activeSchematicItem) {
                            this.renderers = newRenderers;
                        }
                        rendererCache.put(activeSchematicItem, newRenderers);
                    });
        } else {
            Logger.debug("in cache: " + schematicFilePath);
            loadingEntries.remove(schematicFilePath);
        }
    }

    private IAsyncTask<SchematicRenderer[]> startPreparingSchematicAsync(SchematicContainer schematic, IProgressEvent event) {
        BlockPos size = schematic.getSize();
        if (size.equals(BlockPos.ZERO)) {
            throw new IllegalArgumentException("Template size is zero!");
        }

        final SchematicRenderer[] renderers = new SchematicRenderer[3];
        for (int i = 0; i < renderers.length; i++) {
            renderers[i] = new SchematicRenderer();
        }

        final int taskSize = 3;
        final QueuedAsyncTask<?>[] tasks = new QueuedAsyncTask[3];
        final double[] progress = new double[taskSize];
        BlockPos[] pos = new BlockPos[]{BlockPos.ZERO, BlockPos.ZERO.east(size.getX() - 1), BlockPos.ZERO.south(size.getZ() - 1)};

        for (int i = 0; i < taskSize; i++) {
            final int index = i;
            tasks[i] = placeSchematicWorldAsync(schematic, pos[index], renderers, index,
                    (s, p) -> {
                        progress[index] = p;
                        event.onProgress("Block 렌더링 준비중...", (progress[0] + progress[1] + progress[2]) / 3.0);
                    }
            );
        }

        return new JobJoinTask<SchematicRenderer[]>(tasks)
                .setResult(renderers);
    }

    private QueuedAsyncTask<Void> placeSchematicWorldAsync(SchematicContainer schematic, BlockPos position, SchematicRenderer[] renderers, int rendererIndex, IProgressEvent event) {
        PlacementSettings pSettings = new PlacementSettings();
        if (rendererIndex == 1) {
            pSettings.setMirror(Mirror.FRONT_BACK);
        } else if (rendererIndex == 2) {
            pSettings.setMirror(Mirror.LEFT_RIGHT);
        }

        Logger.debug("Placing " + rendererIndex + " schematic...");
        SchematicWorld world = new SchematicWorld(Minecraft.getInstance().level);
        SchematicPrinter printer = SchematicPrinter.newPlacingSchematicTask(schematic, world, position, pSettings, (s, p) -> event.onProgress(s, 0.7 * p))
                .includeAir(false);
        return TaskScheduler.addAsyncTask(printer)
                .then((c) -> cachingDrawBufferAsync(world, renderers, rendererIndex, (s, p) -> event.onProgress(s, 0.7 + 0.3 * p)));
    }

    private IAsyncNoResultTask cachingDrawBufferAsync(SchematicWorld world, SchematicRenderer[] renderers, int rendererIndex, IProgressEvent event) {
        Logger.debug("Draw buffer caching " + rendererIndex + " schematic...");
        return renderers[rendererIndex].newDrawingSchematicWorldTask(world, event);
    }

    public void clearCache() {
        rendererCache.invalidateAll();
        loadingEntries.clear();
    }

    @Override
    public Iterable<LoadingEntry> getProgressEntries() {
        return loadingEntries.values();
    }

    @Override
    public int size() {
        return loadingEntries.size();
    }

    public void cancelLoadingTask(ItemStack itemStack) {
        String schematicFilePath = itemStack.getTag().getString("File");
        TaskScheduler.cancelGroupTask(schematicFilePath.hashCode());
        loadingEntries.remove(schematicFilePath);
    }

    public static class LoadingEntry implements IProgressEvent, IProgressEntry {
        private final String name;
        private String status;
        private double progress;

        public LoadingEntry(String name) {
            this.name = name;
            this.status = "";
            this.progress = 0;
        }

        @Override
        public void onProgress(String status, double progress) {
            this.status = status;
            this.progress = progress;
        }

        @Override
        public String getTitle() {
            return name;
        }

        @Override
        public String getSubtitle() {
            return status;
        }

        @Override
        public double getProgress() {
            return progress;
        }
    }
}
