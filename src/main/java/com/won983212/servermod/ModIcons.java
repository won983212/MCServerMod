package com.won983212.servermod;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ModIcons {
    public static final ResourceLocation ICON_ATLAS = new ResourceLocation(ServerMod.MODID, "textures/gui/icons.png");
    private static int x = 0, y = -1;
    private final int iconX;
    private final int iconY;

    public static final ModIcons
            I_CONFIRM = newRow(),
            I_TOOL_MOVE_XZ = next(),
            I_TOOL_MOVE_Y = next(),
            I_TOOL_ROTATE = next(),
            I_TOOL_MIRROR = next(),
            I_TOOL_DEPLOY = next();

    public ModIcons(int x, int y) {
        iconX = x * 16;
        iconY = y * 16;
    }

    private static ModIcons next() {
        return new ModIcons(++x, y);
    }

    private static ModIcons newRow() {
        return new ModIcons(x = 0, ++y);
    }

    @OnlyIn(Dist.CLIENT)
    public void bind() {
        Minecraft.getInstance()
                .getTextureManager()
                .bind(ICON_ATLAS);
    }

    @OnlyIn(Dist.CLIENT)
    public void draw(MatrixStack matrixStack, AbstractGui screen, int x, int y) {
        bind();
        screen.blit(matrixStack, x, y, iconX, iconY, 16, 16);
    }

}
