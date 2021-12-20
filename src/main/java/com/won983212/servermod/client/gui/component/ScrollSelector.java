package com.won983212.servermod.client.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.client.gui.PanelScreen;

import java.util.List;

public class ScrollSelector extends HoveringCover {
    private final List<?> elements;
    private int selectedIndex = 0;

    public ScrollSelector(int x, int y, int width, int height, List<?> elements) {
        super(x, y, width, height, null);
        this.elements = elements;
        this.hoveredColor = 0x33ffffff;
    }

    private void selectNext(int delta) {
        int max = elements.size();
        if (max == 0) {
            return;
        }

        selectedIndex += delta;
        if (selectedIndex < 0) {
            selectedIndex += max;
        }
        if (selectedIndex >= max) {
            selectedIndex %= max;
        }
    }

    public int getSelectedIndex() {
        if (selectedIndex >= elements.size()) {
            selectedIndex = elements.size() - 1;
        }
        return selectedIndex;
    }

    @Override
    public void render(MatrixStack ms, int x, int y, float partialTime) {
        String str = elements.get(getSelectedIndex()).toString();
        str = (selectedIndex + 1) + ": " + str;
        str = PanelScreen.ellipsisText(font, str, width - 10);

        drawString(ms, font, str, this.x + 5, this.y + (height - font.lineHeight) / 2 + 1, 0xffffffff);
        super.render(ms, x, y, partialTime);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        selectNext((int) delta);
        return true;
    }
}
