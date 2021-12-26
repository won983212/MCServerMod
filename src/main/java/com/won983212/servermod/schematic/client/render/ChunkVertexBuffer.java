package com.won983212.servermod.schematic.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.schematic.world.SchematicWorld;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.ForgeHooksClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChunkVertexBuffer {
    private static Vector3d cameraPosition = new Vector3d(0, 0, 0);
    protected final BlockPos origin;
    protected final Set<RenderType> usedBlockRenderLayers;
    protected final Map<RenderType, VertexBuffer> blockBufferCache;


    protected ChunkVertexBuffer(int chunkX, int chunkY, int chunkZ) {
        int layerCount = RenderType.chunkBufferLayers().size();
        origin = new BlockPos(chunkX * 16, chunkY * 16, chunkZ * 16);
        usedBlockRenderLayers = new HashSet<>(layerCount);
        blockBufferCache = new HashMap<>(layerCount);
    }

    public static void setCameraPosition(Vector3d pos) {
        cameraPosition = pos;
    }

    public boolean isEmpty() {
        return usedBlockRenderLayers.isEmpty();
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public void render(MatrixStack ms, RenderType layer) {
        if (!usedBlockRenderLayers.contains(layer)) {
            return;
        }
        VertexBuffer buf = blockBufferCache.get(layer);
        if (buf != null) {
            buf.bind();
            layer.setupRenderState();
            layer.format().setupBufferState(0L);

            ms.pushPose();
            ms.translate(origin.getX(), origin.getY(), origin.getZ());
            buf.draw(ms.last().pose(), layer.mode());
            ms.popPose();

            VertexBuffer.unbind();
            layer.format().clearBufferState();
            layer.clearRenderState();
        }
    }

    protected boolean buildChunkBuffer(SchematicWorld schematic, BlockPos anchor) {
        BlockPos pos1 = origin;
        BlockPos pos2 = pos1.offset(15, 15, 15);
        BlockBufferRenderer bufferRenderer = new BlockBufferRenderer(schematic, usedBlockRenderLayers, anchor);

        BlockModelRenderer.enableCaching();
        BlockPos.betweenClosedStream(pos1, pos2).forEach(bufferRenderer::renderBlock);
        ForgeHooksClient.setRenderLayer(null);

        if (usedBlockRenderLayers.contains(RenderType.translucent())) {
            BufferBuilder bufferBuilder = bufferRenderer.getBufferBuilder(RenderType.translucent());
            bufferBuilder.sortQuads((float) cameraPosition.x - (float) pos1.getX(),
                    (float) cameraPosition.y - (float) pos1.getY(),
                    (float) cameraPosition.z - (float) pos1.getZ());
        }

        // finishDrawing
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            if (!bufferRenderer.isStarted(layer)) {
                continue;
            }

            BufferBuilder buf = bufferRenderer.getBufferBuilder(layer);
            buf.end();

            VertexBuffer vBuf = new VertexBuffer(layer.format());
            vBuf.upload(buf);
            blockBufferCache.put(layer, vBuf);
        }

        BlockModelRenderer.clearCache();
        return !isEmpty();
    }
}