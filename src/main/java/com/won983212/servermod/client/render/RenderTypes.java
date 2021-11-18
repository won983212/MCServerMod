package com.won983212.servermod.client.render;

import com.won983212.servermod.ModTextures;
import com.won983212.servermod.ServerMod;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderTypes extends RenderState {

    public static RenderType getOutlineTranslucent(ResourceLocation texture, boolean cull) {
        RenderType.State rendertype$state = RenderType.State.builder()
                .setTextureState(new TextureState(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDiffuseLightingState(DIFFUSE_LIGHTING)
                .setAlphaState(DEFAULT_ALPHA)
                .setCullState(cull ? CULL : NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
        return RenderType.create(createLayerName("outline_translucent" + (cull ? "_cull" : "")),
                DefaultVertexFormats.NEW_ENTITY, GL11.GL_QUADS, 256, true, true, rendertype$state);
    }

    private static final RenderType OUTLINE_SOLID =
            RenderType.create(createLayerName("outline_solid"), DefaultVertexFormats.NEW_ENTITY, GL11.GL_QUADS, 256, true,
                    false, RenderType.State.builder()
                            .setTextureState(new TextureState(ModTextures.BLANK.getLocation(), false, false))
                            .setDiffuseLightingState(DIFFUSE_LIGHTING)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .createCompositeState(true));

    private static String createLayerName(String name) {
        return ServerMod.MODID + ":" + name;
    }

    public static RenderType getOutlineSolid() {
        return OUTLINE_SOLID;
    }

    // Mmm gimme those protected fields
    public RenderTypes() {
        super(null, null, null);
    }

}
