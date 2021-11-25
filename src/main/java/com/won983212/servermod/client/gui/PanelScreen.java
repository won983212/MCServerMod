package com.won983212.servermod.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.ModTextures;
import com.won983212.servermod.client.gui.component.AbstractComponent;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;

import java.awt.*;

public class PanelScreen extends Screen {

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
    }

    protected Point drawTexturedBackground(MatrixStack ms, ModTextures texture) {
        int x = (this.width - texture.width) / 2;
        int y = (this.height - texture.height) / 2;
        texture.draw(ms, this, x, y);
        return new Point(x, y);
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
}
