package com.won983212.servermod.utility;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector3i;

public class MatrixTransformStack {

    private final MatrixStack internal;

    public MatrixTransformStack(MatrixStack internal) {
        this.internal = internal;
    }

    public static MatrixTransformStack of(MatrixStack ms) {
        return new MatrixTransformStack(ms);
    }

    public MatrixTransformStack translate(double x, double y, double z) {
        internal.translate(x, y, z);
        return this;
    }

    public MatrixTransformStack multiply(Quaternion quaternion) {
        internal.mulPose(quaternion);
        return this;
    }

    public MatrixTransformStack scale(float factor) {
        internal.scale(factor, factor, factor);
        return this;
    }

    public MatrixTransformStack push() {
        internal.pushPose();
        return this;
    }

    public MatrixTransformStack pop() {
        internal.popPose();
        return this;
    }

    public MatrixTransformStack rotateX(double angle) {
        return multiply(Vector3f.XP, angle);
    }

    public MatrixTransformStack rotateY(double angle) {
        return multiply(Vector3f.YP, angle);
    }

    public MatrixTransformStack rotateZ(double angle) {
        return multiply(Vector3f.ZP, angle);
    }

    public MatrixTransformStack translateX(double x) {
        return translate(x, 0, 0);
    }

    public MatrixTransformStack translateY(double y) {
        return translate(0, y, 0);
    }

    public MatrixTransformStack translateZ(double z) {
        return translate(0, 0, z);
    }

    public MatrixTransformStack translate(Vector3i vec) {
        return translate(vec.getX(), vec.getY(), vec.getZ());
    }

    public MatrixTransformStack translate(Vector3d vec) {
        return translate(vec.x, vec.y, vec.z);
    }

    public MatrixTransformStack translateBack(Vector3d vec) {
        return translate(-vec.x, -vec.y, -vec.z);
    }

    public MatrixTransformStack multiply(Vector3f axis, double angle) {
        if (angle == 0)
            return this;
        return multiply(axis.rotationDegrees((float) angle));
    }
}