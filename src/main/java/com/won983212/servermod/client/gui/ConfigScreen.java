package com.won983212.servermod.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.skin.SkinCacheCleaner;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;

public class ConfigScreen extends Screen {
    /**
     * Width of a button
     */
    private static final int BUTTON_WIDTH = 200;
    /**
     * Height of a button
     */
    private static final int BUTTON_HEIGHT = 20;
    /**
     * Distance from bottom of the screen to the "Done" button's top
     */
    private static final int DONE_BUTTON_TOP_OFFSET = 26;

    /**
     * Distance from top of the screen to this GUI's title
     */
    private static final int TITLE_HEIGHT = 8;


    public ConfigScreen() {
        super(new TranslationTextComponent("servermod.settings.title"));
    }

    @Override
    protected void init() {
        this.addButton(new Button(
                (this.width - BUTTON_WIDTH) / 2,
                TITLE_HEIGHT + font.lineHeight + 30,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslationTextComponent("servermod.settings.clearcache"),
                button -> SkinCacheCleaner.clearSkinCache()
        ));
        this.addButton(new Button(
                (this.width - BUTTON_WIDTH) / 2,
                this.height - DONE_BUTTON_TOP_OFFSET,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslationTextComponent("gui.done"),
                button -> this.onClose()
        ));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        drawCenteredString(matrixStack, font, title.getString(), width / 2,
                TITLE_HEIGHT, 0xffffff);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
