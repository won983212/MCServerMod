package com.won983212.servermod.client.render.outliner;

import com.mojang.blaze3d.matrix.MatrixStack;

import com.won983212.servermod.client.render.SuperRenderTypeBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public class LineOutline extends Outline {
    protected Vector3d start = Vector3d.ZERO;
    protected Vector3d end = Vector3d.ZERO;

    public LineOutline set(Vector3d start, Vector3d end) {
        this.start = start;
        this.end = end;
        return this;
    }

    @Override
    public void render(MatrixStack ms, SuperRenderTypeBuffer buffer, float pt) {
        renderCuboidLine(ms, buffer, start, end);
    }
}
