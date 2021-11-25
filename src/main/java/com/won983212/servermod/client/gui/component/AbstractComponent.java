package com.won983212.servermod.client.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;

import java.awt.*;

public abstract class AbstractComponent extends AbstractGui implements IRenderable, IGuiEventListener {
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected final FontRenderer font;

    public AbstractComponent(int x, int y, int width, int height) {
        setBounds(x, y, width, height);
        this.font = Minecraft.getInstance().font;
    }

    public void setX(int x){
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle getBounds(){
        return new Rectangle(x, y, width, height);
    }

    public void drawRectangle(MatrixStack ms, int color) {
        fill(ms, x, y, x + width, y + height, color);
    }

    public void addOffset(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }
}
