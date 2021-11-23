package com.won983212.servermod.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraftforge.client.model.pipeline.LightUtil;

public class SchematicVertexFactory {

    private final BufferBuilderReader template;

    // Vertex Position
    private final MatrixStack transforms;

    // Temporary
    private final Vector4f pos = new Vector4f();
    private final Vector3f normal = new Vector3f();

    public SchematicVertexFactory(BufferBuilder buf) {
        template = new BufferBuilderReader(buf);
        transforms = new MatrixStack();
        transforms.pushPose();
    }

    public BufferBuilder makeBuffer(RenderType layer) {
        BufferBuilder builder = new BufferBuilder(256);
        builder.begin(layer.mode(), layer.format());

        if (template.isEmpty()) {
            builder.end();
            return builder;
        }

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
            normal.transform(transforms.last().normal().copy());
            float nx = normal.x();
            float ny = normal.y();
            float nz = normal.z();

            float staticDiffuse = LightUtil.diffuseLight(normalX, normalY, normalZ);
            float instanceDiffuse = LightUtil.diffuseLight(nx, ny, nz);

            pos.set(x, y, z, 1F);
            builder.vertex(pos.x(), pos.y(), pos.z());

            float diffuseMult = instanceDiffuse / staticDiffuse;
            int colorR = transformColor(r, diffuseMult);
            int colorG = transformColor(g, diffuseMult);
            int colorB = transformColor(b, diffuseMult);
            builder.color(colorR, colorG, colorB, a);

            float u = template.getU(i);
            float v = template.getV(i);
            builder.uv(u, v);

            builder.uv2(15728880); // full lighting

            builder.normal(nx, ny, nz);
            builder.endVertex();
        }
        builder.end();

        reset();
        return builder;
    }

    public void reset() {
        while (!transforms.clear())
            transforms.popPose();
        transforms.pushPose();
    }

    public static int transformColor(byte component, float scale) {
        return MathHelper.clamp((int) (Byte.toUnsignedInt(component) * scale), 0, 255);
    }
}
