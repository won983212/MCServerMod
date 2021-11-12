package com.won983212.servermod.client.render;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class ModRenderType extends RenderType {

    // Ignored
    public ModRenderType(String nameIn, VertexFormat formatIn, int drawModeIn, int bufferSizeIn, boolean useDelegateIn, boolean needsSortingIn, Runnable setupTaskIn, Runnable clearTaskIn) {
        super(nameIn, formatIn, drawModeIn, bufferSizeIn, useDelegateIn, needsSortingIn, setupTaskIn, clearTaskIn);
    }

    public static RenderType standard(ResourceLocation resourceLocation) {
        RenderType.State state = RenderType.State.getBuilder()
                .texture(new RenderState.TextureState(resourceLocation, false, false))//Texture state
                .shadeModel(SHADE_ENABLED)//shadeModel(GL11.GL_SMOOTH)
                .alpha(ZERO_ALPHA)//disableAlphaTest
                .transparency(TRANSLUCENT_TRANSPARENCY)//enableBlend/blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA)
                .build(true);
        return makeType("standard", DefaultVertexFormats.ENTITY, GL11.GL_QUADS, 256, true, false, state);
    }
}
