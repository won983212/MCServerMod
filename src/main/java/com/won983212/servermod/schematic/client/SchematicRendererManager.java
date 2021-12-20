package com.won983212.servermod.schematic.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.Logger;
import com.won983212.servermod.client.render.SuperRenderTypeBuffer;
import com.won983212.servermod.item.SchematicItem;
import com.won983212.servermod.schematic.IProgressEntry;
import com.won983212.servermod.schematic.IProgressEntryProducer;
import com.won983212.servermod.schematic.IProgressEvent;
import com.won983212.servermod.schematic.SchematicContainer;
import com.won983212.servermod.schematic.client.render.SchematicRenderer;
import com.won983212.servermod.schematic.world.SchematicWorld;
import com.won983212.servermod.utility.animate.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Mirror;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.PlacementSettings;

import java.util.concurrent.*;

// TODO 로딩을 tick에 따라 Async하게 바꾸자. executor는 취소할 수도 없으며, rendering도중 랙걸리니까!
public class SchematicRendererManager implements IProgressEntryProducer {

    private static final Cache<ItemStack, SchematicRenderer[]> rendererCache;
    private final ConcurrentHashMap<String, LoadingEntry> loadingEntries;
    private final ExecutorService executor = new ForkJoinPool();
    private volatile ItemStack currentStack;
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

        SchematicRenderer[] renderers = rendererCache.getIfPresent(activeSchematicItem);
        if (renderers == null) {
            CompletableFuture.supplyAsync(() ->
                    SchematicItem.loadSchematic(activeSchematicItem, (s, p) -> loadingEntry.onProgress(s, 0.6 * p)), executor)
                    .thenCompose((t) -> prepareSchematicRenderer(t, (s, p) -> loadingEntry.onProgress(s, 0.6 + 0.4 * p)))
                    .whenComplete((t, u) -> {
                        loadingEntries.remove(schematicFilePath);
                        if (t != null) {
                            if (currentStack == activeSchematicItem) {
                                this.renderers = t;
                            }
                            rendererCache.put(activeSchematicItem, t);
                        }
                    })
                    .exceptionally((e) -> {
                        Logger.error(e);
                        return null;
                    });
        } else {
            Logger.debug("in cache: " + schematicFilePath);
            this.renderers = renderers;
            loadingEntries.remove(schematicFilePath);
        }
    }

    private CompletableFuture<SchematicRenderer[]> prepareSchematicRenderer(SchematicContainer schematic, IProgressEvent event) {
        BlockPos size = schematic.getSize();
        if (size.equals(BlockPos.ZERO)) {
            Logger.warn("Template size is zero!");
            return CompletableFuture.completedFuture(null);
        }

        final int taskSize = 3;
        final double[] progress = new double[taskSize];
        CompletableFuture<?>[] tasks = new CompletableFuture<?>[taskSize];
        BlockPos[] pos = new BlockPos[]{BlockPos.ZERO, BlockPos.ZERO.east(size.getX() - 1), BlockPos.ZERO.south(size.getZ() - 1)};

        SchematicRenderer[] renderers = new SchematicRenderer[3];
        for (int i = 0; i < renderers.length; i++) {
            renderers[i] = new SchematicRenderer();
        }

        for (int i = 0; i < taskSize; i++) {
            final int index = i;
            tasks[i] = CompletableFuture.runAsync(() -> placeSchematicWorld(schematic, pos[index], renderers, index,
                    (s, p) -> {
                        progress[index] = p;
                        event.onProgress("Block 렌더링 준비중...", (progress[0] + progress[1] + progress[2]) / 3.0);
                    }
            ), executor);
        }

        return CompletableFuture.allOf(tasks)
                .thenApply((t) -> renderers);
    }

    private void placeSchematicWorld(SchematicContainer schematic, BlockPos position, SchematicRenderer[] renderers, int rendererIndex, IProgressEvent event) {
        PlacementSettings pSettings = new PlacementSettings();
        if (rendererIndex == 1) {
            pSettings.setMirror(Mirror.FRONT_BACK);
        } else if (rendererIndex == 2) {
            pSettings.setMirror(Mirror.LEFT_RIGHT);
        }

        Logger.debug("Placing " + rendererIndex + " schematic...");
        SchematicWorld world = new SchematicWorld(Minecraft.getInstance().level);
        schematic.placeSchematicToWorld(world, position, pSettings, (s, p) -> event.onProgress(s, 0.7 * p));

        Logger.debug("Draw buffer caching " + rendererIndex + " schematic...");
        renderers[rendererIndex].setSchematicWorld(world, (s, p) -> event.onProgress(s, 0.7 + 0.3 * p));
        Logger.debug("Draw buffer caching " + rendererIndex + " complete!");
    }

    public void clearCache() {
        rendererCache.invalidateAll();
    }

    @Override
    public Iterable<LoadingEntry> getProgressEntries() {
        return loadingEntries.values();
    }

    @Override
    public int size() {
        return loadingEntries.size();
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
