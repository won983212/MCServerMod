package com.won983212.servermod.client.render.tile;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.client.model.ModelIndustrialAlarm;
import com.won983212.servermod.tile.TileEntityIndustrialAlarm;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3f;

public class RenderIndustrialAlarm extends TileEntityRenderer<TileEntityIndustrialAlarm> {

    private static final float ROTATE_SPEED = 10F;
    private final ModelIndustrialAlarm model = new ModelIndustrialAlarm();

    public RenderIndustrialAlarm(TileEntityRendererDispatcher renderer) {
        super(renderer);
    }

    @Override
    public void render(TileEntityIndustrialAlarm tile, float partialTicks, MatrixStack matrix, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        Direction dir = tile.getDirection();

        matrix.pushPose();
        matrix.translate(0.5, 0, 0.5);
        if (dir != null) {
            switch (dir) {
                case DOWN:
                    matrix.translate(0, 1, 0);
                    matrix.mulPose(Vector3f.XP.rotationDegrees(180));
                    break;
                case NORTH:
                    matrix.translate(0, 0.5, 0.5);
                    matrix.mulPose(Vector3f.XN.rotationDegrees(90));
                    break;
                case SOUTH:
                    matrix.translate(0, 0.5, -0.5);
                    matrix.mulPose(Vector3f.XP.rotationDegrees(90));
                    break;
                case EAST:
                    matrix.translate(-0.5, 0.5, 0);
                    matrix.mulPose(Vector3f.ZN.rotationDegrees(90));
                    break;
                case WEST:
                    matrix.translate(0.5, 0.5, 0);
                    matrix.mulPose(Vector3f.ZP.rotationDegrees(90));
                    break;
                default:
                    break;
            }
        }

        float rotation = (tile.getLevel().getGameTime() + partialTicks) * ROTATE_SPEED % 360;
        model.render(matrix, bufferIn, combinedLightIn, combinedOverlayIn, tile.isActive(), rotation, false, false);
        matrix.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(TileEntityIndustrialAlarm tile) {
        return true;
    }
}