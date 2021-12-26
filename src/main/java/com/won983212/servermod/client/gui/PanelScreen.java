package com.won983212.servermod.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.won983212.servermod.ModTextures;
import com.won983212.servermod.client.gui.component.AbstractComponent;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;

import java.awt.*;

public class PanelScreen extends Screen {

    private long lastAlertTime = 0;
    private String lastAlertMessage = "";

    PanelScreen(ITextComponent title) {
        super(title);
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTime) {
        super.render(ms, mouseX, mouseY, partialTime);
        for (IGuiEventListener child : children) {
            if (child instanceof IRenderable) {
                ((IRenderable) child).render(ms, mouseX, mouseY, partialTime);
            }
        }
        renderAlert(ms);
    }

    private void renderAlert(MatrixStack ms){
        long currentTime = System.currentTimeMillis();
        if (currentTime < lastAlertTime){
            int alertWidth = font.width(lastAlertMessage) + 50;
            final int alertHeight = 30;
            final int x0 = (width - alertWidth) / 2;
            final int x1 = (width + alertWidth) / 2;
            final int y0 = (height - alertHeight) / 2;
            final int y1 = (height + alertHeight) / 2;

            fill(ms, x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xffaaaaaa);
            fill(ms, x0, y0, x1, y1, 0xff000000);
            drawCenteredString(ms, font, lastAlertMessage, width / 2,
                    (height - font.lineHeight) / 2, 0xffffffff);
        }
    }

    protected void drawTexturedBackground(MatrixStack ms, ModTextures texture) {
        int x = (this.width - texture.width) / 2;
        int y = (this.height - texture.height) / 2;
        texture.draw(ms, this, x, y);
        new Point(x, y);
    }

    protected void applyBackgroundOffset(ModTextures texture) {
        int x = (this.width - texture.width) / 2;
        int y = (this.height - texture.height) / 2;
        for (IGuiEventListener child : children) {
            if (child instanceof AbstractComponent) {
                ((AbstractComponent) child).addOffset(x, y);
            }
        }
    }

    protected void alert(String message){
        alert(message, 3000);
    }

    protected void alert(String message, long duration){
        lastAlertTime = System.currentTimeMillis() + duration;
        lastAlertMessage = message;
    }

    public static String ellipsisText(FontRenderer font, String str, int width) {
        int sizeStr = font.width(str);
        int sizeDots = font.width("...");
        if (sizeStr > width) {
            str = font.plainSubstrByWidth(str, width - sizeDots);
            str += "...";
        }
        return str;
    }

    public static void fillFloat(MatrixStack stack, float x0, float y0, float x1, float y1, int color) {
        Matrix4f p_238460_0_ = stack.last().pose();

        if (x0 < x1) {
            float i = x0;
            x0 = x1;
            x1 = i;
        }

        if (y0 < y1) {
            float j = y0;
            y0 = y1;
            y1 = j;
        }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.vertex(p_238460_0_, x0, y1, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(p_238460_0_, x1, y1, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(p_238460_0_, x1, y0, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(p_238460_0_, x0, y0, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.end();
        WorldVertexBufferUploader.end(bufferbuilder);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
