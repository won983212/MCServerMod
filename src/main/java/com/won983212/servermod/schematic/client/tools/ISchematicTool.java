package com.won983212.servermod.schematic.client.tools;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.client.render.SuperRenderTypeBuffer;
import net.minecraft.client.renderer.IRenderTypeBuffer;

public interface ISchematicTool {

    void init();

    void updateSelection();

    boolean handleRightClick();

    boolean handleMouseWheel(double delta);

    void renderTool(MatrixStack ms, SuperRenderTypeBuffer buffer);

    void renderOverlay(MatrixStack ms, IRenderTypeBuffer buffer);

    void renderOnSchematic(MatrixStack ms, SuperRenderTypeBuffer buffer);

    int getHighlightColor();
}
