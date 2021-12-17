package com.won983212.servermod.client.render.item;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.client.model.ModelIndustrialAlarm;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class RenderIndustrialAlarmItem extends ItemStackTileEntityRenderer {

    private static final ModelIndustrialAlarm industrialAlarm = new ModelIndustrialAlarm();

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemCameraTransforms.TransformType transformType, @Nonnull MatrixStack matrix,
                             @Nonnull IRenderTypeBuffer renderer, int light, int overlayLight) {
        matrix.pushPose();
        matrix.translate(0.5, 0.3, 0.5);
        industrialAlarm.render(matrix, renderer, light, overlayLight, false, 0, true, stack.hasFoil());
        matrix.popPose();
    }
}