package com.won983212.servermod.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import net.minecraft.world.World;
import net.minecraftforge.client.model.pipeline.LightUtil;

public class SuperByteBuffer {

    private final BufferBuilderReader template;

    // Vertex Position
    private final MatrixStack transforms;

    // Vertex Coloring
    private boolean shouldColor;
    private int r, g, b, a;
    private boolean disableDiffuseDiv;
    private boolean disableDiffuseMult;

    // Vertex Texture Coords
    private SpriteShiftFunc spriteShiftFunc;

    // Vertex Overlay Color
    private boolean hasOverlay;
    private int overlay = OverlayTexture.NO_OVERLAY;

    // Vertex Lighting
    private boolean useWorldLight;
    private Matrix4f lightTransform;
    private boolean hasCustomLight;
    private int packedLightCoords;
    private boolean hybridLight;

    // Vertex Normals
    private boolean fullNormalTransform;

    // Temporary
    private static final Long2IntMap WORLD_LIGHT_CACHE = new Long2IntOpenHashMap();
    private final Vector4f pos = new Vector4f();
    private final Vector3f normal = new Vector3f();
    private final Vector4f lightPos = new Vector4f();

    public SuperByteBuffer(BufferBuilder buf) {
        template = new BufferBuilderReader(buf);
        transforms = new MatrixStack();
        transforms.pushPose();
    }

    public void renderInto(MatrixStack input, IVertexBuilder builder) {
        if (isEmpty())
            return;

        Matrix4f modelMat = input.last()
                .pose()
                .copy();
        Matrix4f localTransforms = transforms.last()
                .pose();
        modelMat.multiply(localTransforms);

        Matrix3f normalMat;
        if (fullNormalTransform) {
            normalMat = input.last().normal().copy();
            Matrix3f localNormalTransforms = transforms.last().normal();
            normalMat.mul(localNormalTransforms);
        } else {
            normalMat = transforms.last().normal().copy();
        }

        if (useWorldLight) {
            WORLD_LIGHT_CACHE.clear();
        }

        float f = .5f;
        int vertexCount = template.getVertexCount();
        for (int i = 0; i < vertexCount; i++) {
            float x = template.getX(i);
            float y = template.getY(i);
            float z = template.getZ(i);
            byte r = template.getR(i);
            byte g = template.getG(i);
            byte b = template.getB(i);
            byte a = template.getA(i);
            float normalX = template.getNX(i) / 127f;
            float normalY = template.getNY(i) / 127f;
            float normalZ = template.getNZ(i) / 127f;

            normal.set(normalX, normalY, normalZ);
            normal.transform(normalMat);
            float nx = normal.x();
            float ny = normal.y();
            float nz = normal.z();

            float staticDiffuse = LightUtil.diffuseLight(normalX, normalY, normalZ);
            float instanceDiffuse = LightUtil.diffuseLight(nx, ny, nz);

            pos.set(x, y, z, 1F);
            pos.transform(modelMat);
            builder.vertex(pos.x(), pos.y(), pos.z());

            if (shouldColor) {
                if (disableDiffuseMult) {
                    builder.color(this.r, this.g, this.b, this.a);
                } else {
                    int colorR = transformColor(this.r, instanceDiffuse);
                    int colorG = transformColor(this.g, instanceDiffuse);
                    int colorB = transformColor(this.b, instanceDiffuse);
                    builder.color(colorR, colorG, colorB, this.a);
                }
            } else {
                if (disableDiffuseDiv && disableDiffuseMult) {
                    builder.color(r, g, b, a);
                } else {
                    float diffuseMult;
                    if (disableDiffuseDiv) {
                        diffuseMult = instanceDiffuse;
                    } else if (disableDiffuseMult) {
                        diffuseMult = 1 / staticDiffuse;
                    } else {
                        diffuseMult = instanceDiffuse / staticDiffuse;
                    }
                    int colorR = transformColor(r, diffuseMult);
                    int colorG = transformColor(g, diffuseMult);
                    int colorB = transformColor(b, diffuseMult);
                    builder.color(colorR, colorG, colorB, a);
                }
            }

            float u = template.getU(i);
            float v = template.getV(i);
            if (spriteShiftFunc != null) {
                spriteShiftFunc.shift(builder, u, v);
            } else {
                builder.uv(u, v);
            }

            if (hasOverlay) {
                builder.overlayCoords(overlay);
            }

            int light;
            if (useWorldLight) {
                lightPos.set(((x - f) * 15 / 16f) + f, (y - f) * 15 / 16f + f, (z - f) * 15 / 16f + f, 1F);
                lightPos.transform(localTransforms);
                if (lightTransform != null) {
                    lightPos.transform(lightTransform);
                }

                light = getLight(Minecraft.getInstance().level, lightPos);
                if (hasCustomLight) {
                    light = maxLight(light, packedLightCoords);
                }
            } else if (hasCustomLight) {
                light = packedLightCoords;
            } else {
                light = template.getLight(i);
            }

            if (hybridLight) {
                builder.uv2(maxLight(light, template.getLight(i)));
            } else {
                builder.uv2(light);
            }

            builder.normal(nx, ny, nz);

            builder.endVertex();
        }

        reset();
    }

    public SuperByteBuffer reset() {
        while (!transforms.clear())
            transforms.popPose();
        transforms.pushPose();

        shouldColor = false;
        r = 0;
        g = 0;
        b = 0;
        a = 0;
        disableDiffuseDiv = false;
        disableDiffuseMult = false;
        spriteShiftFunc = null;
        hasOverlay = false;
        overlay = OverlayTexture.NO_OVERLAY;
        useWorldLight = false;
        lightTransform = null;
        hasCustomLight = false;
        packedLightCoords = 0;
        hybridLight = false;
        fullNormalTransform = false;
        return this;
    }

    public SuperByteBuffer translate(Vector3d vec) {
        return translate(vec.x, vec.y, vec.z);
    }

    public SuperByteBuffer translate(double x, double y, double z) {
        return translate((float) x, (float) y, (float) z);
    }

    public SuperByteBuffer translate(float x, float y, float z) {
        transforms.translate(x, y, z);
        return this;
    }

    public boolean isEmpty() {
        return template.isEmpty();
    }

    public static int transformColor(byte component, float scale) {
        return MathHelper.clamp((int) (Byte.toUnsignedInt(component) * scale), 0, 255);
    }

    public static int transformColor(int component, float scale) {
        return MathHelper.clamp((int) (component * scale), 0, 255);
    }

    public static int maxLight(int packedLight1, int packedLight2) {
        int blockLight1 = LightTexture.block(packedLight1);
        int skyLight1 = LightTexture.sky(packedLight1);
        int blockLight2 = LightTexture.block(packedLight2);
        int skyLight2 = LightTexture.sky(packedLight2);
        return LightTexture.pack(Math.max(blockLight1, blockLight2), Math.max(skyLight1, skyLight2));
    }

    private static int getLight(World world, Vector4f lightPos) {
        BlockPos pos = new BlockPos(lightPos.x(), lightPos.y(), lightPos.z());
        return WORLD_LIGHT_CACHE.computeIfAbsent(pos.asLong(), $ -> WorldRenderer.getLightColor(world, pos));
    }

    @FunctionalInterface
    public interface SpriteShiftFunc {
        void shift(IVertexBuilder builder, float u, float v);
    }

}
