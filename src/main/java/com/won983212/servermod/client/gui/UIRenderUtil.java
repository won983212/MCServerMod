package com.won983212.servermod.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.opengl.GL11;

public class UIRenderUtil {
    public static void drawColoredTexture(MatrixStack ms, int color, int x, int y, int tex_left, int tex_top, int width, int height) {
        drawColoredTexture(ms, color, x, y, 0, (float) tex_left, (float) tex_top, width, height, 256, 256);
    }

    public static void drawColoredTexture(MatrixStack ms, int color, int x, int y, int z, float tex_left, float tex_top, int width, int height, int sheet_width, int sheet_height) {
        drawColoredTexture(ms, color, x, x + width, y, y + height, z, width, height, tex_left, tex_top, sheet_width, sheet_height);
    }

    private static void drawColoredTexture(MatrixStack ms, int color, int left, int right, int top, int bot, int z, int tex_width, int tex_height, float tex_left, float tex_top, int sheet_width, int sheet_height) {
        drawTexturedQuad(ms.last().pose(), color, left, right, top, bot, z, (tex_left + 0.0F) / (float) sheet_width, (tex_left + (float) tex_width) / (float) sheet_width, (tex_top + 0.0F) / (float) sheet_height, (tex_top + (float) tex_height) / (float) sheet_height);
    }

    private static void drawTexturedQuad(Matrix4f m, int color, int left, int right, int top, int bot, int z, float u1, float u2, float v1, float v2) {
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        RenderSystem.enableBlend();
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuilder();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
        bufferbuilder.vertex(m, (float) left , (float) bot, (float) z).color(r, g, b, a).uv(u1, v2).endVertex();
        bufferbuilder.vertex(m, (float) right, (float) bot, (float) z).color(r, g, b, a).uv(u2, v2).endVertex();
        bufferbuilder.vertex(m, (float) right, (float) top, (float) z).color(r, g, b, a).uv(u2, v1).endVertex();
        bufferbuilder.vertex(m, (float) left , (float) top, (float) z).color(r, g, b, a).uv(u1, v1).endVertex();
        bufferbuilder.end();
        RenderSystem.enableAlphaTest();
        WorldVertexBufferUploader.end(bufferbuilder);
    }
}
