package com.won983212.servermod.schematic.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.world.SchematicWorld;
import com.won983212.servermod.utility.MatrixTransformStack;
import com.won983212.servermod.utility.animate.AnimationTickHolder;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.lwjgl.opengl.GL11;

import java.util.*;

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
        BlockBufferContext context = new BlockBufferContext(schematic, anchor);

        BlockModelRenderer.enableCaching();
        BlockPos.betweenClosedStream(pos1, pos2)
                .forEach(localPos -> process(localPos, context));
        ForgeHooksClient.setRenderLayer(null);

        if (usedBlockRenderLayers.contains(RenderType.translucent())) {
            BufferBuilder bufferBuilder = context.buffers.get(RenderType.translucent());
            bufferBuilder.sortQuads((float) cameraPosition.x - (float) pos1.getX(),
                    (float) cameraPosition.y - (float) pos1.getY(),
                    (float) cameraPosition.z - (float) pos1.getZ());
        }

        // finishDrawing
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            if (!context.startedBufferBuilders.contains(layer)) {
                continue;
            }

            BufferBuilder buf = context.buffers.get(layer);
            buf.end();

            VertexBuffer vBuf = new VertexBuffer(layer.format());
            vBuf.upload(buf);
            blockBufferCache.put(layer, vBuf);
        }

        BlockModelRenderer.clearCache();
        return !isEmpty();
    }

    private void process(BlockPos localPos, BlockBufferContext ctx) {
        BlockPos pos = localPos.offset(ctx.anchor);
        BlockState state = ctx.schematic.getBlockState(pos);
        FluidState fluidState = ctx.schematic.getFluidState(pos);

        for (RenderType layer : RenderType.chunkBufferLayers()) {
            ForgeHooksClient.setRenderLayer(layer);

            boolean isRenderFluid = !fluidState.isEmpty() && RenderTypeLookup.canRenderInLayer(fluidState, layer);
            boolean isRenderSolid = state.getRenderShape() != BlockRenderType.INVISIBLE && RenderTypeLookup.canRenderInLayer(state, layer);

            BufferBuilder bufferBuilder = null;
            if (isRenderFluid || isRenderSolid) {
                if (!ctx.buffers.containsKey(layer)) {
                    ctx.buffers.put(layer, new BufferBuilder(DefaultVertexFormats.BLOCK.getIntegerSize()));
                }
                bufferBuilder = ctx.buffers.get(layer);
                if (ctx.startedBufferBuilders.add(layer)) {
                    bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                }
            }

            if (isRenderFluid) {
                if (ctx.blockRendererDispatcher.renderLiquid(pos, ctx.schematic, bufferBuilder, fluidState)) {
                    usedBlockRenderLayers.add(layer);
                }
            }

            if (isRenderSolid) {
                ctx.ms.pushPose();
                ctx.ms.translate(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
                TileEntity tileEntity = ctx.schematic.getBlockEntity(pos);
                if (ctx.blockRendererDispatcher.renderModel(state, pos, ctx.schematic, ctx.ms, bufferBuilder, true, ctx.random,
                        tileEntity != null ? tileEntity.getModelData() : EmptyModelData.INSTANCE)) {
                    usedBlockRenderLayers.add(layer);
                }

                // render floor
                if (localPos.getY() == 0) {
                    BlockPos floorPos = pos.below();
                    tileEntity = ctx.schematic.getBlockEntity(floorPos);
                    if (ctx.blockRendererDispatcher.renderModel(state, floorPos, ctx.schematic, ctx.ms, bufferBuilder, true, ctx.random,
                            tileEntity != null ? tileEntity.getModelData() : EmptyModelData.INSTANCE)) {
                        usedBlockRenderLayers.add(layer);
                    }
                }

                ctx.ms.popPose();
            }
        }
    }

    private static class BlockBufferContext {
        private final BlockRendererDispatcher blockRendererDispatcher;
        private final Random random;

        private final Set<RenderType> startedBufferBuilders;
        private final Map<RenderType, BufferBuilder> buffers;
        private final MatrixStack ms;
        private final SchematicWorld schematic;
        private final BlockPos anchor;

        private BlockBufferContext(SchematicWorld schematic, BlockPos anchor) {
            this.startedBufferBuilders = new HashSet<>(RenderType.chunkBufferLayers().size());
            this.buffers = new HashMap<>();
            this.ms = new MatrixStack();
            this.schematic = schematic;
            this.anchor = anchor;

            Minecraft mc = Minecraft.getInstance();
            this.blockRendererDispatcher = mc.getBlockRenderer();
            this.random = mc.level.random;
        }
    }
}