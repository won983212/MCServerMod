package com.won983212.servermod.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.won983212.servermod.*;
import com.won983212.servermod.client.render.SuperRenderTypeBuffer;
import com.won983212.servermod.schematic.client.render.ChunkVertexBuffer;
import com.won983212.servermod.schematic.parser.SchematicFileParser;
import com.won983212.servermod.skin.SkinCacheCleaner;
import com.won983212.servermod.task.TaskScheduler;
import com.won983212.servermod.utility.animate.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

@Mod.EventBusSubscriber(modid = ServerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }
        CommonModDist.CLIENT_SCHEDULER.tick();
    }

    @SubscribeEvent
    public static void onTooltipShow(ItemTooltipEvent e) {
        if (e.getFlags().isAdvanced()) {
            Item item = e.getItemStack().getItem();
            if (item instanceof BlockItem) {
                int legacyId = LegacyMapper.getLegacyFromBlock(((BlockItem) item).getBlock().defaultBlockState());
                if (legacyId != -1) {
                    e.getToolTip().add((new StringTextComponent("# " + (legacyId >> 4) + ":" + (legacyId & 15)).withStyle(TextFormatting.DARK_GRAY)));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup e) {
        ChunkVertexBuffer.setCameraPosition(e.getInfo().getPosition());
    }

    @SubscribeEvent
    public static void onLoadWorld(WorldEvent.Load event) {
        IWorld world = event.getWorld();
        if (world.isClientSide() && world instanceof ClientWorld) {
            AnimationTickHolder.reset();
        }
    }

    @SubscribeEvent
    public static void onUnloadWorld(WorldEvent.Unload event) {
        if (event.getWorld().isClientSide()) {
            AnimationTickHolder.reset();
            SchematicFileParser.clearCache();
            ClientDist.SCHEMATIC_HANDLER.unload();
            CommonModDist.CLIENT_SCHEDULER.cancelAllTask();
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        AnimationTickHolder.tick();
        ClientDist.SCHEMATIC_HANDLER.tick();
        ClientDist.SCHEMATIC_SENDER.tick();
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldLastEvent event) {
        Vector3d cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        MatrixStack ms = event.getMatrixStack();

        ms.pushPose();
        ms.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());

        SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();
        ClientDist.SCHEMATIC_HANDLER.render(ms, buffer);
        buffer.draw();
        RenderSystem.enableCull();

        ms.popPose();
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) {
            return;
        }

        MatrixStack ms = event.getMatrixStack();
        IRenderTypeBuffer.Impl buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Point mousePos = getMousePosition();
        ClientDist.SCHEMATIC_HANDLER.renderOverlay(ms, buffers, event.getPartialTicks());
        ClientDist.SCHEMATIC_UPLOAD_SCREEN.render(ms, mousePos.x, mousePos.y, event.getPartialTicks());
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent e) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        int key = e.getKey();
        boolean pressed = !(e.getAction() == 0);

        ClientDist.SCHEMATIC_HANDLER.onKeyInput(key, pressed);
        if (ModKeys.KEY_CLEAR_CACHE.isDown()) {
            SkinCacheCleaner.clearSkinCache();
        }
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseInputEvent event) {
        int button = event.getButton();
        boolean pressed = !(event.getAction() == 0);

        if (Minecraft.getInstance().screen == null) {
            ClientDist.SCHEMATIC_HANDLER.onMouseInput(button, pressed);
        } else {
            Point p = getMousePosition();
            ClientDist.SCHEMATIC_UPLOAD_SCREEN.onMouseInput(button, pressed, p.x, p.y);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(InputEvent.MouseScrollEvent event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        double delta = event.getScrollDelta();
        event.setCanceled(ClientDist.SCHEMATIC_HANDLER.mouseScrolled(delta));
    }

    private static Point getMousePosition() {
        Minecraft mc = Minecraft.getInstance();
        int i = (int) (mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth());
        int j = (int) (mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight());
        return new Point(i, j);
    }
}
