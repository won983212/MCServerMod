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
import com.won983212.servermod.schematic.client.render.SchematicRenderer;
import com.won983212.servermod.schematic.world.SchematicWorld;
import com.won983212.servermod.utility.animate.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Mirror;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;

import java.util.Vector;
import java.util.concurrent.*;

public class SchematicRendererManager implements IProgressEntryProducer {

    private static final Cache<ItemStack, SchematicWorld[]> schematicWorldCache;
    private final Vector<SchematicRenderer> renderers;
    private final ConcurrentHashMap<String, LoadingEntry> loadingEntries;
    private ExecutorService executor = new ForkJoinPool();

    static {
        schematicWorldCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build();
    }

    public SchematicRendererManager() {
        renderers = new Vector<>(3);
        for (int i = 0; i < renderers.capacity(); i++) {
            renderers.add(new SchematicRenderer());
        }
        loadingEntries = new ConcurrentHashMap<>();
    }

    public void render(MatrixStack ms, SuperRenderTypeBuffer buffer, SchematicTransformation transformation) {
        if (!renderers.isEmpty()) {
            float pt = AnimationTickHolder.getPartialTicks();
            boolean lr = transformation.getScaleLR().get(pt) < 0;
            boolean fb = transformation.getScaleFB().get(pt) < 0;
            if (lr && !fb) {
                renderers.get(2).render(ms, buffer);
            } else if (fb && !lr) {
                renderers.get(1).render(ms, buffer);
            } else {
                renderers.get(0).render(ms, buffer);
            }
        }
    }

    public void setupRenderer(ItemStack activeSchematicItem) {
        final String schematicFilePath = activeSchematicItem.getTag().getString("File");
        World clientWorld = Minecraft.getInstance().level;

        LoadingEntry loadingEntry = new LoadingEntry(schematicFilePath);
        loadingEntries.put(schematicFilePath, loadingEntry);

        if (executor.isShutdown()) {
            executor = new ForkJoinPool();
        }

        SchematicWorld[] worlds = schematicWorldCache.getIfPresent(activeSchematicItem);
        if (worlds == null) {
            worlds = new SchematicWorld[3];
            for (int i = 0; i < worlds.length; i++) {
                worlds[i] = new SchematicWorld(clientWorld);
            }
            schematicWorldCache.put(activeSchematicItem, worlds);

            final SchematicWorld[] finalWorlds = worlds;
            CompletableFuture.supplyAsync(() ->
                    SchematicItem.loadSchematic(activeSchematicItem, (s, p) -> loadingEntry.onProgress(s, 0.6 * p)), executor)
                    .thenCompose((t) -> setupRendererImpl(t, finalWorlds, (s, p) -> loadingEntry.onProgress(s, 0.6 + 0.4 * p)))
                    .whenComplete((t, u) -> loadingEntries.remove(schematicFilePath));
        } else {
            Logger.debug("in cache: " + schematicFilePath);
            final SchematicWorld[] finalWorlds = worlds;
            CompletableFuture.runAsync(() -> {
                for (int i = 0; i < finalWorlds.length; i++) {
                    renderers.get(i).cacheSchematicWorld(finalWorlds[i], loadingEntry);
                }
            }, executor).whenComplete((t, u) -> loadingEntries.remove(schematicFilePath));
        }
    }

    private CompletableFuture<Void> setupRendererImpl(Template schematic, SchematicWorld[] worlds, IProgressEvent event) {
        BlockPos size = schematic.getSize();
        if (size.equals(BlockPos.ZERO)) {
            return CompletableFuture.completedFuture(null);
        }

        final int taskSize = 3;
        final double[] progress = new double[taskSize];
        CompletableFuture<?>[] tasks = new CompletableFuture<?>[taskSize];
        BlockPos[] pos = new BlockPos[]{BlockPos.ZERO, BlockPos.ZERO.east(size.getX() - 1), BlockPos.ZERO.south(size.getZ() - 1)};

        for (int i = 0; i < taskSize; i++) {
            final int index = i;
            tasks[i] = CompletableFuture.runAsync(() -> placeSchematicWorld(schematic, worlds[index], pos[index], index,
                    (s, p) -> {
                        progress[index] = p;
                        event.onProgress("Block 렌더링 준비중...", (progress[0] + progress[1] + progress[2]) / 3.0);
                    }
            ), executor);
        }

        return CompletableFuture.allOf(tasks);
    }

    private void placeSchematicWorld(Template schematic, SchematicWorld world, BlockPos position, int rendererIndex, IProgressEvent event) {
        PlacementSettings pSettings = new PlacementSettings();
        if (rendererIndex == 1) {
            pSettings.setMirror(Mirror.FRONT_BACK);
        } else if (rendererIndex == 2) {
            pSettings.setMirror(Mirror.LEFT_RIGHT);
        }

        Logger.debug("Placing " + rendererIndex + " schematic...");
        BlockPos size = schematic.getSize();
        long totalBlocks = (long) size.getX() * size.getY() * size.getZ();

        world.setBlockPlaceProgressEvent((s, p) -> event.onProgress(s, 0.7 * p / totalBlocks));
        schematic.placeInWorld(world, position, pSettings, world.getRandom());
        world.setBlockPlaceProgressEvent(null);

        Logger.debug("Draw buffer caching " + rendererIndex + " schematic...");
        renderers.get(rendererIndex).cacheSchematicWorld(world, (s, p) -> event.onProgress(s, 0.7 + 0.3 * p));
        Logger.debug("Draw buffer caching " + rendererIndex + " complete!");
    }

    public void cancelSetupTask() {
        executor.shutdownNow();
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
