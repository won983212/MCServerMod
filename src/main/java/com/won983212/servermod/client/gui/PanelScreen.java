package com.won983212.servermod.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.ModTextures;
import com.won983212.servermod.client.gui.component.AbstractComponent;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.screen.Screen;
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
}
