package com.won983212.servermod.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.ModIcons;
import com.won983212.servermod.client.gui.component.HoveringCover;
import com.won983212.servermod.schematic.ClientSchematicLoader;
import com.won983212.servermod.schematic.IUploadEntryProducer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.awt.*;
import java.util.Map;

public class SchematicUploadStatusScreen extends Screen implements HoveringCover.IPressable {
    private final IUploadEntryProducer schematicLoader;
    private final HoveringCover uploadButton;
    private boolean isOpenMenu = false;

    public SchematicUploadStatusScreen(IUploadEntryProducer schematicLoader) {
        super(new StringTextComponent("Schematic Upload Status"));
        this.schematicLoader = schematicLoader;
        uploadButton = new HoveringCover(8, 8, 50, 16, this);
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float tick) {
        if (schematicLoader.size() > 0) {
            renderUploadButton(ms, mx, my, tick);
            if (isOpenMenu) {
                renderUploadList(ms, mx, my, tick);
            }
        }
    }

    private void renderUploadButton(MatrixStack ms, int mx, int my, float tick) {
        Rectangle bounds = uploadButton.getBounds();
        FontRenderer font = Minecraft.getInstance().font;

        String uploadingSize = String.valueOf(schematicLoader.size());
        uploadButton.setWidth(24 + font.width(uploadingSize));
        uploadButton.drawRectangle(ms, 0xaa000000);
        ModIcons.UPLOADING.draw(ms, this, bounds.x, bounds.y);
        font.draw(ms, uploadingSize, bounds.x + 18, bounds.y + (bounds.height - font.lineHeight) / 2.0f + 1, 0xffffffff);

        uploadButton.render(ms, mx, my, tick);
    }

    private void renderUploadList(MatrixStack ms, int mx, int my, float tick) {
        Rectangle bounds = uploadButton.getBounds();
        FontRenderer font = Minecraft.getInstance().font;

        int y = bounds.y + 18;
        final int gap = 4;
        final int width = 120;

        for (Map.Entry<String, ClientSchematicLoader.SchematicUploadEntry> ent : schematicLoader.getUploadEntries()) {
            String fileName = ent.getKey();
            float progress = ent.getValue().getServerReceivedProgress();
            String fileSizeString = ent.getValue().getSizeString();
            int progressWidth = (int) (progress * (width - 2 * gap));

            fill(ms, bounds.x, y, bounds.x + width, y + 26, 0xaa000000);
            font.draw(ms, getSubstrText(font, fileName, fileSizeString, width - gap * 2), bounds.x + gap, y + gap, 0xffffffff);
            fill(ms, bounds.x + gap, y + gap + 15, bounds.x + width - gap, y + gap + 18, 0xff333333);
            fill(ms, bounds.x + gap, y + gap + 15, bounds.x + gap + progressWidth, y + gap + 18, 0xff396EB0);

            y += 28;
        }
    }

    private String getSubstrText(FontRenderer font, String title, String sizeStr, int width) {
        int sizeTitle = font.width(title);
        int sizeTextWidth = font.width(" (" + sizeStr + ")");
        int sizeDots = font.width("...");
        if (sizeTitle + sizeTextWidth > width) {
            title = font.plainSubstrByWidth(title, width - sizeTextWidth - sizeDots);
            title += "...";
        }
        return title + " §7(" + sizeStr + ")";
    }

    public void onMouseInput(int button, boolean pressed, int x, int y) {
        if (pressed) {
            uploadButton.mouseClicked(x, y, button);
        } else {
            uploadButton.mouseReleased(x, y, button);
        }
    }

    @Override
    public void onPress(HoveringCover button) {
        if (button == uploadButton) {
            isOpenMenu = !isOpenMenu;
        }
    }
}
