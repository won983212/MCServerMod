package com.won983212.servermod.schematic.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.Logger;
import com.won983212.servermod.client.render.SuperRenderTypeBuffer;
import com.won983212.servermod.schematic.IProgressEvent;
import com.won983212.servermod.schematic.world.SchematicWorld;
import com.won983212.servermod.utility.MatrixTransformStack;
import com.won983212.servermod.utility.animate.AnimationTickHolder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// TODO 먼 곳의 chunk는 안보이게!
public class SchematicRenderer {
    private final List<ChunkVertexBuffer> chunks = new ArrayList<>();
    protected SchematicWorld schematic;
    private BlockPos anchor;

    public void setSchematicWorld(SchematicWorld world, IProgressEvent event) {
        this.anchor = world.anchor;
        this.schematic = world;
        redraw(event);
    }

    public void render(MatrixStack ms, SuperRenderTypeBuffer buffer) {
        if (schematic == null) {
            return;
        }

        synchronized (chunks) {
            for (RenderType layer : RenderType.chunkBufferLayers()) {
                if (layer == RenderType.solid()) {
                    buffer.getBuffer(RenderType.solid());
                    renderTileEntities(ms, buffer);
                }
                for (ChunkVertexBuffer vertexBuffer : chunks) {
                    vertexBuffer.render(ms, layer);
                }
            }
        }
    }

    protected void redraw(IProgressEvent event) {
        MutableBoundingBox bounds = schematic.getBounds();
        int countX = (int) Math.ceil(bounds.getXSpan() / 16.0);
        int countY = (int) Math.ceil(bounds.getYSpan() / 16.0);
        int countZ = (int) Math.ceil(bounds.getZSpan() / 16.0);

        synchronized (chunks) {
            chunks.clear();
        }

        long total = (long) countX * countY * countZ;
        long current = 0;
        for (int x = 0; x < countX; x++) {
            for (int y = 0; y < countY; y++) {
                for (int z = 0; z < countZ; z++) {
                    current++;
                    ChunkVertexBuffer chunk = new ChunkVertexBuffer(x, y, z);
                    if (!chunk.buildChunkBuffer(schematic, anchor)) {
                        continue;
                    }
                    synchronized (chunks) {
                        chunks.add(chunk);
                    }
                    if (event != null) {
                        event.onProgress("Chunk 불러오는 중...", (double) current / total);
                    }
                }
            }
        }
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
                float pt = AnimationTickHolder.getPartialTicks();
                renderer.render(tileEntity, pt, ms, buffer, 15728880, OverlayTexture.NO_OVERLAY);
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
