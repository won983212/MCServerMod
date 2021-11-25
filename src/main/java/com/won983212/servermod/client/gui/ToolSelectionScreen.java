package com.won983212.servermod.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.won983212.servermod.ModKeys;
import com.won983212.servermod.ModTextures;
import com.won983212.servermod.schematic.client.tools.Tools;
import com.won983212.servermod.utility.Lang;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.List;
import java.util.function.Consumer;

public class ToolSelectionScreen extends Screen {

    public final String scrollToCycle = Lang.translate("gui.toolmenu.cycle").getString();
    public final String holdToFocus = "gui.toolmenu.focusKey";

    protected List<Tools> tools;
    protected Consumer<Tools> callback;
    public boolean focused;
    private float yOffset;
    protected int selection;
    private boolean initialized;

    protected int w;
    protected int h;

    public ToolSelectionScreen(List<Tools> tools, Consumer<Tools> callback) {
        super(new StringTextComponent("Tool Selection"));
        this.minecraft = Minecraft.getInstance();
        this.tools = tools;
        this.callback = callback;
        focused = false;
        yOffset = 0;
        selection = 0;
        initialized = false;

        callback.accept(tools.get(selection));

        w = Math.max(tools.size() * 50 + 30, 220);
        h = 32;
    }

    public void setSelectedElement(Tools tool) {
        if (!tools.contains(tool)) {
            return;
        }
        selection = tools.indexOf(tool);
    }

    public void cycle(int direction) {
        selection += (direction < 0) ? 1 : -1;
        selection = (selection + tools.size()) % tools.size();
    }

    private void draw(MatrixStack matrixStack, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        MainWindow mainWindow = mc.getWindow();
        if (!initialized) {
            init(mc, mainWindow.getGuiScaledWidth(), mainWindow.getGuiScaledHeight());
        }

        int x = (mainWindow.getGuiScaledWidth() - w) / 2 + 15;
        int y = mainWindow.getGuiScaledHeight() - h - 75;

        matrixStack.pushPose();
        matrixStack.translate(0, -yOffset, focused ? 100 : 0);

        RenderSystem.enableBlend();
        RenderSystem.color4f(1, 1, 1, focused ? 7 / 8f : 1 / 2f);
        ModTextures.OVERLAY.draw(matrixStack, x - 15, y, w, h);

        float toolTipAlpha = yOffset / 10;
        List<ITextComponent> toolTip = tools.get(selection).getDescription();
        int stringAlphaComponent = ((int) (toolTipAlpha * 0xFF)) << 24;

        if (toolTipAlpha > 0.25f) {
            RenderSystem.color4f(.7f, .7f, .8f, toolTipAlpha);
            ModTextures.OVERLAY.draw(matrixStack, x - 15, y + 33, w, h + 22);
            RenderSystem.color4f(1, 1, 1, 1);

            if (toolTip.size() > 0) {
                font.draw(matrixStack, toolTip.get(0), x - 10, y + 38, 0xEEEEEE + stringAlphaComponent);
            }
            if (toolTip.size() > 1) {
                font.draw(matrixStack, toolTip.get(1), x - 10, y + 50, 0xCCDDFF + stringAlphaComponent);
            }
            if (toolTip.size() > 2) {
                font.draw(matrixStack, toolTip.get(2), x - 10, y + 60, 0xCCDDFF + stringAlphaComponent);
            }
            if (toolTip.size() > 3) {
                font.draw(matrixStack, toolTip.get(3), x - 10, y + 72, 0xCCCCDD + stringAlphaComponent);
            }
        }

        RenderSystem.color4f(1, 1, 1, 1);
        if (tools.size() > 1) {
            String keyName = ModKeys.KEY_TOOL_MENU.getKey().getDisplayName().getString();
            int width = minecraft.getWindow().getGuiScaledWidth();
            if (!focused) {
                drawCenteredString(matrixStack, minecraft.font, Lang.translate(holdToFocus, keyName), width / 2,
                        y - 10, 0xCCDDFF);
            } else {
                drawCenteredString(matrixStack, minecraft.font, scrollToCycle, width / 2, y - 10, 0xCCDDFF);
            }
        } else {
            x += 65;
        }

        for (int i = 0; i < tools.size(); i++) {
            matrixStack.pushPose();

            int startX = x + i * 50;
            int bandColor = tools.get(i).getTool().getHighlightColor();
            if(bandColor != 0) {
                fill(matrixStack, startX, y, startX + 50, y + 2, bandColor);
            }

            float alpha = focused ? 1 : .2f;
            if (i == selection) {
                matrixStack.translate(0, -10, 0);
                String toolName = tools.get(i).getDisplayName().getString();
                drawCenteredString(matrixStack, minecraft.font, toolName, startX + 24, y + 30, 0xCCDDFF);
                alpha = 1;
            }

            RenderSystem.color4f(0, 0, 0, alpha);
            tools.get(i).getIcon().draw(matrixStack, this, startX + 16, y + 14);
            RenderSystem.color4f(1, 1, 1, alpha);
            tools.get(i).getIcon().draw(matrixStack, this, startX + 16, y + 13);

            matrixStack.popPose();
        }

        RenderSystem.enableBlend();
        matrixStack.popPose();
    }

    public void update() {
        if (focused) {
            yOffset += (10 - yOffset) * .1f;
        } else {
            yOffset *= .9f;
        }
    }

    public void renderPassive(MatrixStack matrixStack, float partialTicks) {
        draw(matrixStack, partialTicks);
    }

    @Override
    public void onClose() {
        callback.accept(tools.get(selection));
    }

    @Override
    protected void init() {
        super.init();
        initialized = true;
    }
}
