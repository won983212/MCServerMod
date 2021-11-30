package com.won983212.servermod.schematic.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.schematic.world.SchematicWorld;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.fluid.FluidState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.lwjgl.opengl.GL11;

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

    public static void setCameraPosition(Vector3d pos){
        cameraPosition = pos;
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

    protected boolean buildChunkBuffer(SchematicWorld schematic, BlockPos anchor) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockRendererDispatcher blockRendererDispatcher = minecraft.getBlockRenderer();

        BlockPos pos1 = origin;
        BlockPos pos2 = pos1.offset(15, 15, 15);
        Set<RenderType> startedBufferBuilders = new HashSet<>(RenderType.chunkBufferLayers().size());
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
                                usedBlockRenderLayers.add(layer);
                            }
                        }

                        if (isRenderSolid) {
                            ms.pushPose();
                            ms.translate(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
                            TileEntity tileEntity = schematic.getBlockEntity(pos);
                            if (blockRendererDispatcher.renderModel(state, pos, schematic, ms, bufferBuilder, true, minecraft.level.random,
                                    tileEntity != null ? tileEntity.getModelData() : EmptyModelData.INSTANCE)) {
                                usedBlockRenderLayers.add(layer);
                            }

                            // render floor
                            if (localPos.getY() == 0) {
                                BlockPos floorPos = pos.below();
                                tileEntity = schematic.getBlockEntity(floorPos);
                                if (blockRendererDispatcher.renderModel(state, floorPos, schematic, ms, bufferBuilder, true, minecraft.level.random,
                                        tileEntity != null ? tileEntity.getModelData() : EmptyModelData.INSTANCE)) {
                                    usedBlockRenderLayers.add(layer);
                                }
                            }

                            ms.popPose();
                        }
                    }
                });

        ForgeHooksClient.setRenderLayer(null);

        if (usedBlockRenderLayers.contains(RenderType.translucent())) {
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
            blockBufferCache.put(layer, vBuf);
        }

        BlockModelRenderer.clearCache();
        return !isEmpty();
    }
}