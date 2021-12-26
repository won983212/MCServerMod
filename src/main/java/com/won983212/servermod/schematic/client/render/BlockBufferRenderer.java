package com.won983212.servermod.schematic.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.world.SchematicWorld;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.fluid.FluidState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class BlockBufferRenderer {
    private final BlockRendererDispatcher blockRendererDispatcher;
    private final Random random;

    private final Set<RenderType> usedBlockRenderLayers;
    private final Set<RenderType> startedBufferBuilders;
    private final Map<RenderType, BufferBuilder> buffers;
    private final MatrixStack ms;
    private final SchematicWorld schematic;
    private final BlockPos anchor;

    protected BlockBufferRenderer(SchematicWorld schematic, Set<RenderType> usedBlockRenderLayers, BlockPos anchor) {
        this.usedBlockRenderLayers = usedBlockRenderLayers;
        this.startedBufferBuilders = new HashSet<>(RenderType.chunkBufferLayers().size());
        this.buffers = new HashMap<>();
        this.ms = new MatrixStack();
        this.schematic = schematic;
        this.anchor = anchor;

        Minecraft mc = Minecraft.getInstance();
        this.blockRendererDispatcher = mc.getBlockRenderer();

        if (mc.level == null) {
            Logger.error("Minecraft level is null? It's a bug!");
            this.random = new Random();
        } else {
            this.random = mc.level.random;
        }
    }

    public BufferBuilder getBufferBuilder(RenderType type) {
        return buffers.get(type);
    }

    public boolean isStarted(RenderType type) {
        return startedBufferBuilders.contains(type);
    }

    public void renderBlock(BlockPos localPos) {
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
                if (blockRendererDispatcher.renderModel(state, pos, schematic, ms, bufferBuilder, true, random,
                        tileEntity != null ? tileEntity.getModelData() : EmptyModelData.INSTANCE)) {
                    usedBlockRenderLayers.add(layer);
                }

                // render floor
                if (localPos.getY() == 0) {
                    BlockPos floorPos = pos.below();
                    tileEntity = schematic.getBlockEntity(floorPos);
                    if (blockRendererDispatcher.renderModel(state, floorPos, schematic, ms, bufferBuilder, true, random,
                            tileEntity != null ? tileEntity.getModelData() : EmptyModelData.INSTANCE)) {
                        usedBlockRenderLayers.add(layer);
                    }
                }

                ms.popPose();
            }
        }
    }
}