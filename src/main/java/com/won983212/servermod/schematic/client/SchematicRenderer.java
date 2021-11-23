package com.won983212.servermod.schematic.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.Logger;
import com.won983212.servermod.client.render.SchematicVertexFactory;
import com.won983212.servermod.client.render.SuperRenderTypeBuffer;
import com.won983212.servermod.schematic.world.SchematicWorld;
import com.won983212.servermod.utility.MatrixTransformStack;
import com.won983212.servermod.utility.animate.AnimationTickHolder;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SchematicRenderer {
    private final Set<RenderType> usedBlockRenderLayers = new HashSet<>(getLayerCount());
    private final Map<RenderType, VertexBuffer> bufferCache = new HashMap<>(getLayerCount());
    private boolean changed;
    protected SchematicWorld schematic;
    private BlockPos anchor;
    private BlockRendererDispatcher blockRendererDispatcher;
    private CompletableFuture<Void> renderCachingTask;

    public SchematicRenderer() {
        changed = false;
    }

    public void display(SchematicWorld world) {
        this.anchor = world.anchor;
        this.schematic = world;
        update();
    }

    public void update() {
        changed = true;
    }

    public void cancelRenderCachingTask(){
        if (renderCachingTask != null){
            renderCachingTask.cancel(true);
            Logger.debug("Pre schematic loading task is cancelled");
        }
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !changed)
            return;

        cancelRenderCachingTask();

        Logger.debug("Schematic caching....");
        renderCachingTask = CompletableFuture.runAsync(() -> {
            redraw(mc);
            renderCachingTask = null;
            Logger.debug("Schematic renderer is ready!");
        });

        changed = false;
    }

    public void render(MatrixStack ms, SuperRenderTypeBuffer buffer) {
        if (renderCachingTask != null || schematic == null)
            return;

        for (RenderType layer : RenderType.chunkBufferLayers()) {
            if (!usedBlockRenderLayers.contains(layer))
                continue;
            VertexBuffer buf = bufferCache.get(layer);
            if (buf != null){
                buf.bind();
                layer.setupRenderState();
                layer.format().setupBufferState(0L);
                buf.draw(ms.last().pose(), layer.mode());
                VertexBuffer.unbind();
                layer.format().clearBufferState();
                layer.clearRenderState();
            }
        }

        buffer.getBuffer(RenderType.solid());
        renderTileEntities(ms, buffer);
    }

    protected void redraw(Minecraft minecraft) {
        usedBlockRenderLayers.clear();
        bufferCache.clear();
        blockRendererDispatcher = minecraft.getBlockRenderer();

        Set<RenderType> startedBufferBuilders = new HashSet<>(getLayerCount());
        Map<RenderType, BufferBuilder> buffers = new HashMap<>();
        MatrixStack ms = new MatrixStack();

        BlockModelRenderer.enableCaching();
        BlockPos.betweenClosedStream(schematic.getBounds())
                .forEach(localPos -> {
                    ms.pushPose();
                    MatrixTransformStack.of(ms).translate(localPos);
                    BlockPos pos = localPos.offset(anchor);
                    BlockState state = schematic.getBlockState(pos);

                    for (RenderType blockRenderLayer : RenderType.chunkBufferLayers()) {
                        if (!RenderTypeLookup.canRenderInLayer(state, blockRenderLayer))
                            continue;
                        ForgeHooksClient.setRenderLayer(blockRenderLayer);
                        if (!buffers.containsKey(blockRenderLayer))
                            buffers.put(blockRenderLayer, new BufferBuilder(DefaultVertexFormats.BLOCK.getIntegerSize()));

                        BufferBuilder bufferBuilder = buffers.get(blockRenderLayer);
                        if (startedBufferBuilders.add(blockRenderLayer))
                            bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

                        TileEntity tileEntity = schematic.getBlockEntity(localPos);

                        if (blockRendererDispatcher.renderModel(state, pos, schematic, ms, bufferBuilder, true, minecraft.level.random,
                                tileEntity != null ? tileEntity.getModelData() : EmptyModelData.INSTANCE)) {
                            usedBlockRenderLayers.add(blockRenderLayer);
                        }
                    }

                    ForgeHooksClient.setRenderLayer(null);
                    ms.popPose();
                });

        // finishDrawing
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            if (!startedBufferBuilders.contains(layer))
                continue;
            BufferBuilder buf = buffers.get(layer);
            buf.end();

            SchematicVertexFactory factory = new SchematicVertexFactory(buf);
            VertexBuffer vBuf = new VertexBuffer(layer.format());
            vBuf.upload(factory.makeBuffer(layer));
            bufferCache.put(layer, vBuf);
        }
        BlockModelRenderer.clearCache();
    }

    private static int getLayerCount() {
        return RenderType.chunkBufferLayers().size();
    }

    private void renderTileEntities(MatrixStack ms, IRenderTypeBuffer buffer) {
        Iterator<TileEntity> iterator = schematic.getRenderedTileEntities().iterator();
        while (iterator.hasNext()) {
            TileEntity tileEntity = iterator.next();
            TileEntityRenderer<TileEntity> renderer = TileEntityRendererDispatcher.instance.getRenderer(tileEntity);
            if (renderer == null) {
                iterator.remove();
                continue;
            }

            BlockPos pos = tileEntity.getBlockPos();
            ms.pushPose();
            MatrixTransformStack.of(ms).translate(pos);

            try {
                int worldLight = WorldRenderer.getLightColor(schematic, pos);
                float pt = AnimationTickHolder.getPartialTicks();
                renderer.render(tileEntity, pt, ms, buffer, worldLight, OverlayTexture.NO_OVERLAY);
            } catch (Exception e) {
                iterator.remove();
                String message = "TileEntity " + tileEntity.getType().getRegistryName().toString()
                        + " didn't want to render while moved.\n";
                Logger.error(message + e.toString());
            }

            ms.popPose();
        }
    }
}
