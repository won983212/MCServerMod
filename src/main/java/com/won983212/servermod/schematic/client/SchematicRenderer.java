package com.won983212.servermod.schematic.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.Logger;
import com.won983212.servermod.client.render.SuperByteBuffer;
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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class SchematicRenderer {
    private final Map<RenderType, SuperByteBuffer> bufferCache = new HashMap<>(getLayerCount());
    private final Set<RenderType> usedBlockRenderLayers = new HashSet<>(getLayerCount());
    private final Set<RenderType> startedBufferBuilders = new HashSet<>(getLayerCount());
    private boolean active;
    private boolean changed;
    protected SchematicWorld schematic;
    private BlockPos anchor;

    public SchematicRenderer() {
        changed = false;
    }

    public void display(SchematicWorld world) {
        this.anchor = world.anchor;
        this.schematic = world;
        this.active = true;
        this.changed = true;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void update() {
        changed = true;
    }

    public void tick() {
        if (!active)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !changed)
            return;

        redraw(mc);
        changed = false;
    }

    public void render(MatrixStack ms, SuperRenderTypeBuffer buffer) {
        if (!active)
            return;
        buffer.getBuffer(RenderType.solid());
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            if (!usedBlockRenderLayers.contains(layer))
                continue;
            SuperByteBuffer superByteBuffer = bufferCache.get(layer);
            superByteBuffer.renderInto(ms, buffer.getBuffer(layer));
        }
        renderTileEntities(ms, buffer);
    }

    protected void redraw(Minecraft minecraft) {
        usedBlockRenderLayers.clear();
        startedBufferBuilders.clear();

        final SchematicWorld blockAccess = schematic;
        final BlockRendererDispatcher blockRendererDispatcher = minecraft.getBlockRenderer();

        List<BlockState> blockstates = new LinkedList<>();
        Map<RenderType, BufferBuilder> buffers = new HashMap<>();
        MatrixStack ms = new MatrixStack();

        BlockPos.betweenClosedStream(blockAccess.getBounds())
                .forEach(localPos -> {
                    ms.pushPose();
                    MatrixTransformStack.of(ms).translate(localPos);
                    BlockPos pos = localPos.offset(anchor);
                    BlockState state = blockAccess.getBlockState(pos);

                    for (RenderType blockRenderLayer : RenderType.chunkBufferLayers()) {
                        if (!RenderTypeLookup.canRenderInLayer(state, blockRenderLayer))
                            continue;
                        ForgeHooksClient.setRenderLayer(blockRenderLayer);
                        if (!buffers.containsKey(blockRenderLayer))
                            buffers.put(blockRenderLayer, new BufferBuilder(DefaultVertexFormats.BLOCK.getIntegerSize()));

                        BufferBuilder bufferBuilder = buffers.get(blockRenderLayer);
                        if (startedBufferBuilders.add(blockRenderLayer))
                            bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

                        TileEntity tileEntity = blockAccess.getBlockEntity(localPos);

                        if (blockRendererDispatcher.renderModel(state, pos, blockAccess, ms, bufferBuilder, true,
                                minecraft.level.random,
                                tileEntity != null ? tileEntity.getModelData() : EmptyModelData.INSTANCE)) {
                            usedBlockRenderLayers.add(blockRenderLayer);
                        }
                        blockstates.add(state);
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
            bufferCache.put(layer, new SuperByteBuffer(buf));
        }
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
