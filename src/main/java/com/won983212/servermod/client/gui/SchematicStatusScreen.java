package com.won983212.servermod.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.ModIcons;
import com.won983212.servermod.client.gui.component.HoveringCover;
import com.won983212.servermod.schematic.IProgressEntry;
import com.won983212.servermod.schematic.IProgressEntryProducer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SchematicStatusScreen extends Screen implements HoveringCover.IPressable {
    private final List<IProgressEntryProducer> progressProducers;
    private final HoveringCover progressMenuButton;
    private boolean isOpenMenu;

    public SchematicStatusScreen() {
        super(new StringTextComponent("Schematic Upload Status"));
        this.progressProducers = new ArrayList<>();
        this.isOpenMenu = true;
        this.progressMenuButton = new HoveringCover(8, 8, 50, 16, this);
    }

    public void registerProgressProducer(IProgressEntryProducer producer) {
        progressProducers.add(producer);
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float tick) {
        int size = 0;
        for (IProgressEntryProducer producer : progressProducers) {
            size += producer.size();
        }
        if (size > 0) {
            renderProgressButton(ms, mx, my, tick, size);
            if (isOpenMenu) {
                renderProgressList(ms, mx, my, tick);
            }
        }
    }

    private void renderProgressButton(MatrixStack ms, int mx, int my, float tick, int size) {
        Rectangle bounds = progressMenuButton.getBounds();
        FontRenderer font = Minecraft.getInstance().font;

        String queueSize = String.valueOf(size);
        progressMenuButton.setWidth(24 + font.width(queueSize));
        progressMenuButton.drawRectangle(ms, 0xaa000000);
        ModIcons.UPLOADING.draw(ms, this, bounds.x, bounds.y);
        font.drawShadow(ms, queueSize, bounds.x + 18, bounds.y + (bounds.height - font.lineHeight) / 2.0f + 1, 0xffffffff);

        progressMenuButton.render(ms, mx, my, tick);
    }

    private void renderProgressList(MatrixStack ms, int mx, int my, float tick) {
        Rectangle bounds = progressMenuButton.getBounds();
        FontRenderer font = Minecraft.getInstance().font;

        int y = bounds.y + 18;
        final int gap = 4;
        final int width = 140;

        for (IProgressEntryProducer producer : progressProducers) {
            for (IProgressEntry ent : producer.getProgressEntries()) {
                String title = ent.getTitle();
                String subtitle = ent.getSubtitle();
                int progressWidth = (int) (ent.getProgress() * (width - 2 * gap));

                title = PanelScreen.ellipsisText(font, title, width - 2 * gap);
                subtitle = PanelScreen.ellipsisText(font, subtitle, width - 2 * gap);

                fill(ms, bounds.x, y, bounds.x + width, y + gap * 4 + 20, 0xaa000000);
                font.drawShadow(ms, title, bounds.x + gap, y + gap, 0xffffffff);

                y += gap + 12;
                font.drawShadow(ms, subtitle, bounds.x + gap, y, 0xffaaaaaa);
                fill(ms, bounds.x + gap, y + 12, bounds.x + width - gap, y + 15, 0xff333333);
                fill(ms, bounds.x + gap, y + 12, bounds.x + gap + progressWidth, y + 15, 0xff396EB0);

                y += 22;
            }
        }
    }

    public void onMouseInput(int button, boolean pressed, int x, int y) {
        if (pressed) {
            progressMenuButton.mouseClicked(x, y, button);
        } else {
            progressMenuButton.mouseReleased(x, y, button);
        }
    }

    @Override
    public void onPress(HoveringCover button) {
        if (button == progressMenuButton) {
            isOpenMenu = !isOpenMenu;
        }
    }
}
