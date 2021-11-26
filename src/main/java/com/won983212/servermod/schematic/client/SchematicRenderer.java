package com.won983212.servermod.schematic.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.Logger;
import com.won983212.servermod.client.render.SuperRenderTypeBuffer;
import com.won983212.servermod.schematic.world.SchematicWorld;
import com.won983212.servermod.utility.MatrixTransformStack;
import com.won983212.servermod.utility.animate.AnimationTickHolder;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BushBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.fluid.FluidState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class SchematicRenderer {
    private static Vector3d cameraPosition = new Vector3d(0, 0, 0);
    private final List<ChunkVertexBuffer> chunks = new ArrayList<>();
    protected SchematicWorld schematic;
    private BlockPos anchor;
    private BlockRendererDispatcher blockRendererDispatcher;
    private boolean loading = false;

    public static void setCameraPosition(Vector3d pos) {
        cameraPosition = pos;
    }

    public void cacheSchematicWorld(SchematicWorld world) {
        this.anchor = world.anchor;
        this.schematic = world;
        this.loading = true;
        redraw();
        this.loading = false;
    }

    public void render(MatrixStack ms, SuperRenderTypeBuffer buffer) {
        if (loading || schematic == null) {
            return;
        }

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

    protected void redraw() {
        chunks.clear();
        MutableBoundingBox bounds = schematic.getBounds();
        int countX = (int) Math.ceil(bounds.getXSpan() / 16.0);
        int countY = (int) Math.ceil(bounds.getYSpan() / 16.0);
        int countZ = (int) Math.ceil(bounds.getZSpan() / 16.0);

        for (int x = 0; x < countX; x++) {
            for (int y = 0; y < countY; y++) {
                for (int z = 0; z < countZ; z++) {
                    ChunkVertexBuffer chunk = redrawChunk(x, y, z);
                    if (!chunk.isEmpty()) {
                        chunks.add(chunk);
                    }
                }
            }
        }
    }

    protected ChunkVertexBuffer redrawChunk(int chunkX, int chunkY, int chunkZ) {
        ChunkVertexBuffer buffer = new ChunkVertexBuffer(chunkX, chunkY, chunkZ);
        Minecraft minecraft = Minecraft.getInstance();
        blockRendererDispatcher = minecraft.getBlockRenderer();

        BlockPos pos1 = buffer.origin;
        BlockPos pos2 = pos1.offset(15, 15, 15);
        Set<RenderType> startedBufferBuilders = new HashSet<>(getLayerCount());
        Map<RenderType, BufferBuilder> buffers = new HashMap<>();
        MatrixStack ms = new MatrixStack();

        BlockModelRenderer.enableCaching();
        BlockPos.betweenClosedStream(pos1, pos2)
                .forEach(localPos -> {
                    BlockPos pos = localPos.offset(anchor);
                    BlockState state = schematic.getBlockState(pos);
                    FluidState fluidState = schematic.getFluidState(pos);

                    for (RenderType layer : RenderType.chunkBufferLayers()) {
                        ForgeHooksClient.setRenderLayer(layer);

                        boolean isRenderFluid = !fluidState.isEmpty() && RenderTypeLookup.canRenderInLayer(fluidState, layer);
                        boolean isRenderSolid = state.getRenderShape() != BlockRenderType.INVISIBLE && RenderTypeLookup.canRenderInLayer(state, layer);

                        BufferBuilder bufferBuilder = null;
                        if (isRenderFluid || isRenderSolid) {
                            if (!buffers.containsKey(layer)) {
                                buffers.put(layer, new BufferBuilder(DefaultVertexFormats.BLOCK.getIntegerSize()));
                            }
                            bufferBuilder = buffers.get(layer);
                            if (startedBufferBuilders.add(layer)) {
                                bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                            }
                        }

                        if (isRenderFluid) {
                            if (blockRendererDispatcher.renderLiquid(pos, schematic, bufferBuilder, fluidState)) {
                                buffer.usedBlockRenderLayers.add(layer);
                            }
                        }

                        if (isRenderSolid) {
                            ms.pushPose();
                            ms.translate(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
                            TileEntity tileEntity = schematic.getBlockEntity(pos);
                            if (blockRendererDispatcher.renderModel(state, pos, schematic, ms, bufferBuilder, true, minecraft.level.random,
                                    tileEntity != null ? tileEntity.getModelData() : EmptyModelData.INSTANCE)) {
                                buffer.usedBlockRenderLayers.add(layer);
                            }

                            // render floor
                            if (localPos.getY() == 0) {
                                BlockPos floorPos = pos.below();
                                tileEntity = schematic.getBlockEntity(floorPos);
                                if (blockRendererDispatcher.renderModel(state, floorPos, schematic, ms, bufferBuilder, true, minecraft.level.random,
                                        tileEntity != null ? tileEntity.getModelData() : EmptyModelData.INSTANCE)) {
                                    buffer.usedBlockRenderLayers.add(layer);
                                }
                            }

                            ms.popPose();
                        }
                    }
                });

        ForgeHooksClient.setRenderLayer(null);
        if (buffer.usedBlockRenderLayers.contains(RenderType.translucent())) {
            BufferBuilder bufferBuilder = buffers.get(RenderType.translucent());
            bufferBuilder.sortQuads((float) cameraPosition.x - (float) pos1.getX(),
                    (float) cameraPosition.y - (float) pos1.getY(),
                    (float) cameraPosition.z - (float) pos1.getZ());
        }

        // finishDrawing
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            if (!startedBufferBuilders.contains(layer)) {
                continue;
            }

            BufferBuilder buf = buffers.get(layer);
            buf.end();

            VertexBuffer vBuf = new VertexBuffer(layer.format());
            vBuf.upload(buf);
            buffer.blockBufferCache.put(layer, vBuf);
        }

        BlockModelRenderer.clearCache();
        return buffer;
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

    private static class ChunkVertexBuffer {
        private final BlockPos origin;
        private final Set<RenderType> usedBlockRenderLayers = new HashSet<>(getLayerCount());
        private final Map<RenderType, VertexBuffer> blockBufferCache = new HashMap<>(getLayerCount());

        private ChunkVertexBuffer(int chunkX, int chunkY, int chunkZ) {
            origin = new BlockPos(chunkX * 16, chunkY * 16, chunkZ * 16);
        }

        public boolean isEmpty() {
            return usedBlockRenderLayers.isEmpty();
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
    }
}
