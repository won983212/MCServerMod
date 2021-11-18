package com.won983212.servermod.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.PlacementSimulationWorld;
import com.won983212.servermod.utility.AnimationTickHolder;
import com.won983212.servermod.utility.MatrixTransformStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Iterator;

public class TileEntityRenderHelper {

    public static void renderTileEntities(World world, Iterable<TileEntity> customRenderTEs, MatrixStack ms,
                                          IRenderTypeBuffer buffer) {
        renderTileEntities(world, null, customRenderTEs, ms, null, buffer);
    }

    public static void renderTileEntities(World world, Iterable<TileEntity> customRenderTEs, MatrixStack ms,
                                          IRenderTypeBuffer buffer, float pt) {
        renderTileEntities(world, null, customRenderTEs, ms, null, buffer, pt);
    }

    public static void renderTileEntities(World world, @Nullable PlacementSimulationWorld renderWorld,
                                          Iterable<TileEntity> customRenderTEs, MatrixStack ms, @Nullable Matrix4f lightTransform, IRenderTypeBuffer buffer) {
        renderTileEntities(world, renderWorld, customRenderTEs, ms, lightTransform, buffer,
                AnimationTickHolder.getPartialTicks());
    }

    public static void renderTileEntities(World world, @Nullable PlacementSimulationWorld renderWorld,
                                          Iterable<TileEntity> customRenderTEs, MatrixStack ms, @Nullable Matrix4f lightTransform, IRenderTypeBuffer buffer,
                                          float pt) {
        Iterator<TileEntity> iterator = customRenderTEs.iterator();
        while (iterator.hasNext()) {
            TileEntity tileEntity = iterator.next();
            TileEntityRenderer<TileEntity> renderer = TileEntityRendererDispatcher.instance.getRenderer(tileEntity);
            if (renderer == null) {
                iterator.remove();
                continue;
            }

            BlockPos pos = tileEntity.getBlockPos();
            ms.pushPose();
            MatrixTransformStack.of(ms)
                    .translate(pos);

            try {
                BlockPos lightPos;
                if (lightTransform != null) {
                    Vector4f lightVec = new Vector4f(pos.getX() + .5f, pos.getY() + .5f, pos.getZ() + .5f, 1);
                    lightVec.transform(lightTransform);
                    lightPos = new BlockPos(lightVec.x(), lightVec.y(), lightVec.z());
                } else {
                    lightPos = pos;
                }
                int worldLight = getCombinedLight(world, lightPos, renderWorld, pos);
                renderer.render(tileEntity, pt, ms, buffer, worldLight, OverlayTexture.NO_OVERLAY);

            } catch (Exception e) {
                iterator.remove();

                String message = "TileEntity " + tileEntity.getType()
                        .getRegistryName()
                        .toString() + " didn't want to render while moved.\n";

                Logger.error(message + e.toString());
            }

            ms.popPose();
        }
    }

    public static int getCombinedLight(World world, BlockPos worldPos, @Nullable PlacementSimulationWorld renderWorld,
                                       BlockPos renderWorldPos) {
        int worldLight = WorldRenderer.getLightColor(world, worldPos);

        if (renderWorld != null) {
            int renderWorldLight = WorldRenderer.getLightColor(renderWorld, renderWorldPos);
            return SuperByteBuffer.maxLight(worldLight, renderWorldLight);
        }

        return worldLight;
    }

}
