package com.won983212.servermod.client.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class HoveringCover extends AbstractComponent {
    protected boolean isHovered;
    protected boolean isPressed;
    protected int pressedColor = 0xaaffffff;
    protected int hoveredColor = 0x88ffffff;
    protected final IPressable onPress;

    public HoveringCover(int x, int y, int width, int height, IPressable onPress) {
        super(x, y, width, height);
        this.onPress = onPress;
    }

    @Override
    public void render(MatrixStack ms, int x, int y, float partialTime) {
        isHovered = isMouseOver(x, y);
        if (isPressed) {
            drawRectangle(ms, pressedColor);
        } else if (isHovered) {
            drawRectangle(ms, hoveredColor);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) {
            boolean isHovered = isMouseOver(x, y);
            if (isHovered) {
                if (onPress != null) {
                    onPress.onPress(this);
                }
                isPressed = true;
            }
            return isHovered;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double p_231048_1_, double p_231048_3_, int p_231048_5_) {
        isPressed = false;
        return false;
    }

    @Override
    public boolean isMouseOver(double x, double y) {
        return x >= this.x && x <= this.x + this.width &&
                y >= this.y && y <= this.y + this.height;
    }

    @OnlyIn(Dist.CLIENT)
    public interface IPressable {
        void onPress(HoveringCover button);
    }
}
