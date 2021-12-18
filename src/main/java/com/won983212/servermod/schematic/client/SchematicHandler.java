package com.won983212.servermod.schematic.client;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.ModKeys;
import com.won983212.servermod.client.gui.ToolSelectionScreen;
import com.won983212.servermod.client.render.SuperRenderTypeBuffer;
import com.won983212.servermod.client.render.outliner.AABBOutline;
import com.won983212.servermod.item.ModItems;
import com.won983212.servermod.item.SchematicItem;
import com.won983212.servermod.network.NetworkDispatcher;
import com.won983212.servermod.schematic.client.tools.Tools;
import com.won983212.servermod.schematic.network.CSchematicPlace;
import com.won983212.servermod.schematic.network.CSchematicSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.PlacementSettings;

import java.util.List;

public class SchematicHandler {

    private String displayedSchematic;
    private SchematicTransformation transformation;
    private AxisAlignedBB bounds;
    private boolean deployed;
    private boolean includeAir;
    private boolean active;
    private Tools currentTool;

    private static final int SYNC_DELAY = 10;
    private int syncCooldown;
    private int activeHotbarSlot;
    private ItemStack activeSchematicItem;
    private AABBOutline outline;

    private ToolSelectionScreen selectionScreen;
    private final SchematicRendererManager rendererManager;

    public SchematicHandler() {
        currentTool = Tools.Deploy;
        rendererManager = new SchematicRendererManager();
        selectionScreen = new ToolSelectionScreen(ImmutableList.of(Tools.Deploy), this::equip);
        transformation = new SchematicTransformation();
    }

    public void unload(){
        displayedSchematic = null;
        SchematicRendererManager.clearCache();
        rendererManager.setCurrentSchematic(null);
    }

    public void tick() {
        ClientPlayerEntity player = Minecraft.getInstance().player;

        if (activeSchematicItem != null && transformation != null) {
            transformation.tick();
        }

        ItemStack stack = findBlueprintInHand(player);
        if (stack == null) {
            active = false;
            syncCooldown = 0;
            if (activeSchematicItem != null && itemLost(player)) {
                rendererManager.setCurrentSchematic(null);
                activeHotbarSlot = 0;
                activeSchematicItem = null;
            }
            return;
        }

        boolean needsUpdate = !stack.getTag().getString("File").equals(displayedSchematic);
        if (!active || needsUpdate) {
            init(player, stack, needsUpdate);
        }

        if (syncCooldown > 0) {
            syncCooldown--;
        }
        if (syncCooldown == 1) {
            sync();
        }

        selectionScreen.update();
        currentTool.getTool().updateSelection();
    }

    private void init(ClientPlayerEntity player, ItemStack stack, boolean needsRendererUpdate) {
        loadSettings(stack);
        displayedSchematic = stack.getTag().getString("File");
        active = true;
        if (deployed) {
            if (needsRendererUpdate) {
                rendererManager.setCurrentSchematic(activeSchematicItem);
            }
            Tools toolBefore = currentTool;
            selectionScreen = new ToolSelectionScreen(Tools.getTools(player.isCreative()), this::equip);
            if (toolBefore != null) {
                selectionScreen.setSelectedElement(toolBefore);
                equip(toolBefore);
            }
        } else {
            selectionScreen = new ToolSelectionScreen(ImmutableList.of(Tools.Deploy), this::equip);
        }
    }

    public void render(MatrixStack ms, SuperRenderTypeBuffer buffer) {
        if (!active) {
            return;
        }

        ms.pushPose();
        currentTool.getTool().renderTool(ms, buffer);
        ms.popPose();

        ms.pushPose();
        transformation.applyGLTransformations(ms);
        rendererManager.render(ms, buffer, transformation);

        currentTool.getTool().renderOnSchematic(ms, buffer);
        ms.popPose();
    }

    public void renderOverlay(MatrixStack ms, IRenderTypeBuffer buffer, float partialTicks) {
        if (!active) {
            return;
        }
        currentTool.getTool().renderOverlay(ms, buffer);
        selectionScreen.renderPassive(ms, partialTicks);
    }

    public void onMouseInput(int button, boolean pressed) {
        if (!active) {
            return;
        }
        if (!pressed || button != 1) {
            return;
        }
        if (Minecraft.getInstance().player.isShiftKeyDown()) {
            return;
        }
        currentTool.getTool().handleRightClick();
    }

    public void onKeyInput(int key, boolean pressed) {
        if (!active) {
            return;
        }
        if (key != ModKeys.KEY_TOOL_MENU.getKey().getValue()) {
            return;
        }

        if (pressed && !selectionScreen.focused) {
            selectionScreen.focused = true;
        }
        if (!pressed && selectionScreen.focused) {
            selectionScreen.focused = false;
            selectionScreen.onClose();
        }
    }

    public boolean mouseScrolled(double delta) {
        if (!active) {
            return false;
        }

        if (selectionScreen.focused) {
            selectionScreen.cycle((int) delta);
            return true;
        }
        if (Screen.hasControlDown()) {
            return currentTool.getTool().handleMouseWheel(delta);
        }
        return false;
    }

    private ItemStack findBlueprintInHand(PlayerEntity player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() != ModItems.itemSchematic) {
            return null;
        }
        if (!stack.hasTag()) {
            return null;
        }
        activeSchematicItem = stack;
        activeHotbarSlot = player.inventory.selected;
        return stack;
    }

    private boolean itemLost(PlayerEntity player) {
        for (int i = 0; i < PlayerInventory.getSelectionSize(); i++) {
            if (!player.inventory.getItem(i).sameItem(activeSchematicItem)) {
                continue;
            }
            if (!ItemStack.tagMatches(player.inventory.getItem(i), activeSchematicItem)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public void markDirty() {
        syncCooldown = SYNC_DELAY;
    }

    public void sync() {
        if (activeSchematicItem == null) {
            return;
        }
        CSchematicSync packet = new CSchematicSync(activeHotbarSlot, transformation.toSettings(),
                transformation.getAnchor(), deployed, includeAir);
        NetworkDispatcher.sendToServer(packet);
    }

    public void equip(Tools tool) {
        this.currentTool = tool;
        currentTool.getTool().init();
    }

    public void loadSettings(ItemStack blueprint) {
        CompoundNBT tag = blueprint.getTag();
        BlockPos anchor = BlockPos.ZERO;
        PlacementSettings settings = SchematicItem.getSettings(blueprint);
        transformation = new SchematicTransformation();

        deployed = tag.getBoolean("Deployed");
        includeAir = tag.getBoolean("IncludeAir");
        if (deployed) {
            anchor = NBTUtil.readBlockPos(tag.getCompound("Anchor"));
        }
        BlockPos size = NBTUtil.readBlockPos(tag.getCompound("Bounds"));

        bounds = new AxisAlignedBB(BlockPos.ZERO, size);
        outline = new AABBOutline(bounds);
        outline.getParams().colored(0x6886c5).lineWidth(1 / 16f);
        transformation.init(anchor, settings, bounds);
    }

    public void deploy() {
        if (!deployed) {
            List<Tools> tools = Tools.getTools(Minecraft.getInstance().player.isCreative());
            selectionScreen = new ToolSelectionScreen(tools, this::equip);
            rendererManager.setCurrentSchematic(activeSchematicItem);
        }
        deployed = true;
    }

    public void printInstantly() {
        NetworkDispatcher.sendToServer(new CSchematicPlace(activeSchematicItem.copy()));
        CompoundNBT nbt = activeSchematicItem.getTag();
        nbt.putBoolean("Deployed", false);
        activeSchematicItem.setTag(nbt);
        active = false;
        markDirty();
    }

    public void toggleIncludeAir() {
        includeAir = !includeAir;
        markDirty();
    }

    public SchematicRendererManager getRendererManager(){
        return rendererManager;
    }

    public boolean isIncludeAir() {
        return includeAir;
    }

    public boolean isActive() {
        return active;
    }

    public AxisAlignedBB getBounds() {
        return bounds;
    }

    public SchematicTransformation getTransformation() {
        return transformation;
    }

    public boolean isDeployed() {
        return deployed;
    }

    public ItemStack getActiveSchematicItem() {
        return activeSchematicItem;
    }

    public AABBOutline getOutline() {
        return outline;
    }
}
