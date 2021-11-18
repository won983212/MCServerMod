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
import com.won983212.servermod.schematic.SchematicInstances;
import com.won983212.servermod.schematic.SchematicWorld;
import com.won983212.servermod.schematic.client.tools.Tools;
import com.won983212.servermod.schematic.packet.SchematicPlacePacket;
import com.won983212.servermod.schematic.packet.SchematicSyncPacket;
import com.won983212.servermod.utility.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Mirror;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;

import java.util.List;
import java.util.Vector;

public class SchematicHandler {

	private String displayedSchematic;
	private SchematicTransformation transformation;
	private AxisAlignedBB bounds;
	private boolean deployed;
	private boolean active;
	private Tools currentTool;

	private static final int SYNC_DELAY = 10;
	private int syncCooldown;
	private int activeHotbarSlot;
	private ItemStack activeSchematicItem;
	private AABBOutline outline;

	private final Vector<SchematicRenderer> renderers;
	private ToolSelectionScreen selectionScreen;

	public SchematicHandler() {
		renderers = new Vector<>(3);
		for (int i = 0; i < renderers.capacity(); i++)
			renderers.add(new SchematicRenderer());

		currentTool = Tools.Deploy;
		selectionScreen = new ToolSelectionScreen(ImmutableList.of(Tools.Deploy), this::equip);
		transformation = new SchematicTransformation();
	}

	public void tick() {
		ClientPlayerEntity player = Minecraft.getInstance().player;

		if (activeSchematicItem != null && transformation != null)
			transformation.tick();

		ItemStack stack = findBlueprintInHand(player);
		if (stack == null) {
			active = false;
			syncCooldown = 0;
			if (activeSchematicItem != null && itemLost(player)) {
				activeHotbarSlot = 0;
				activeSchematicItem = null;
				renderers.forEach(r -> r.setActive(false));
			}
			return;
		}

		if (!active || !stack.getTag()
			.getString("File")
			.equals(displayedSchematic))
			init(player, stack);
		if (!active)
			return;

		renderers.forEach(SchematicRenderer::tick);
		if (syncCooldown > 0)
			syncCooldown--;
		if (syncCooldown == 1)
			sync();

		selectionScreen.update();
		currentTool.getTool()
			.updateSelection();
	}

	private void init(ClientPlayerEntity player, ItemStack stack) {
		loadSettings(stack);
		displayedSchematic = stack.getTag()
			.getString("File");
		active = true;
		if (deployed) {
			setupRenderer();
			Tools toolBefore = currentTool;
			selectionScreen = new ToolSelectionScreen(Tools.getTools(player.isCreative()), this::equip);
			if (toolBefore != null) {
				selectionScreen.setSelectedElement(toolBefore);
				equip(toolBefore);
			}
		} else
			selectionScreen = new ToolSelectionScreen(ImmutableList.of(Tools.Deploy), this::equip);
	}

	private void setupRenderer() {
		Template schematic = SchematicItem.loadSchematic(activeSchematicItem);
		BlockPos size = schematic.getSize();
		if (size.equals(BlockPos.ZERO))
			return;

		World clientWorld = Minecraft.getInstance().level;
		SchematicWorld w = new SchematicWorld(clientWorld);
		SchematicWorld wMirroredFB = new SchematicWorld(clientWorld);
		SchematicWorld wMirroredLR = new SchematicWorld(clientWorld);
		PlacementSettings placementSettings = new PlacementSettings();

		schematic.placeInWorldChunk(w, BlockPos.ZERO, placementSettings, w.getRandom());
		placementSettings.setMirror(Mirror.FRONT_BACK);
		schematic.placeInWorldChunk(wMirroredFB, BlockPos.ZERO.east(size.getX() - 1), placementSettings, wMirroredFB.getRandom());
		placementSettings.setMirror(Mirror.LEFT_RIGHT);
		schematic.placeInWorldChunk(wMirroredLR, BlockPos.ZERO.south(size.getZ() - 1), placementSettings, wMirroredFB.getRandom());

		renderers.get(0)
			.display(w);
		renderers.get(1)
			.display(wMirroredFB);
		renderers.get(2)
			.display(wMirroredLR);
	}

	public void render(MatrixStack ms, SuperRenderTypeBuffer buffer) {
		boolean present = activeSchematicItem != null;
		if (!active && !present)
			return;

		if (active) {
			ms.pushPose();
			currentTool.getTool().renderTool(ms, buffer);
			ms.popPose();
		}

		ms.pushPose();
		transformation.applyGLTransformations(ms);

		if (!renderers.isEmpty()) {
			float pt = AnimationTickHolder.getPartialTicks();
			boolean lr = transformation.getScaleLR()
				.get(pt) < 0;
			boolean fb = transformation.getScaleFB()
				.get(pt) < 0;
			if (lr && !fb)
				renderers.get(2)
					.render(ms, buffer);
			else if (fb && !lr)
				renderers.get(1)
					.render(ms, buffer);
			else
				renderers.get(0)
					.render(ms, buffer);
		}
		
		if (active)
			currentTool.getTool().renderOnSchematic(ms, buffer);
		
		ms.popPose();

	}

	public void renderOverlay(MatrixStack ms, IRenderTypeBuffer buffer, float partialTicks) {
		if (!active)
			return;
		currentTool.getTool()
			.renderOverlay(ms, buffer);
		selectionScreen.renderPassive(ms, partialTicks);
	}
	
	public void onMouseInput(int button, boolean pressed) {
		if (!active)
			return;
		if (!pressed || button != 1)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player.isShiftKeyDown())
			return;
		currentTool.getTool().handleRightClick();
	}

	public void onKeyInput(int key, boolean pressed) {
		if (!active)
			return;
		if (key != ModKeys.KEY_TOOL_MENU.getKey().getValue())
			return;

		if (pressed && !selectionScreen.focused)
			selectionScreen.focused = true;
		if (!pressed && selectionScreen.focused) {
			selectionScreen.focused = false;
			selectionScreen.onClose();
		}
	}

	public boolean mouseScrolled(double delta) {
		if (!active)
			return false;

		if (selectionScreen.focused) {
			selectionScreen.cycle((int) delta);
			return true;
		}
		if (Screen.hasControlDown())
			return currentTool.getTool().handleMouseWheel(delta);
		return false;
	}

	private ItemStack findBlueprintInHand(PlayerEntity player) {
		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() != ModItems.itemSchematic)
			return null;
		if (!stack.hasTag())
			return null;

		activeSchematicItem = stack;
		activeHotbarSlot = player.inventory.selected;
		return stack;
	}

	private boolean itemLost(PlayerEntity player) {
		for (int i = 0; i < PlayerInventory.getSelectionSize(); i++) {
			if (!player.inventory.getItem(i)
				.sameItem(activeSchematicItem))
				continue;
			if (!ItemStack.tagMatches(player.inventory.getItem(i), activeSchematicItem))
				continue;
			return false;
		}
		return true;
	}

	public void markDirty() {
		syncCooldown = SYNC_DELAY;
	}

	public void sync() {
		if (activeSchematicItem == null)
			return;
		NetworkDispatcher.sendToServer(new SchematicSyncPacket(activeHotbarSlot, transformation.toSettings(), transformation.getAnchor(), deployed));
	}

	public void equip(Tools tool) {
		this.currentTool = tool;
		currentTool.getTool()
			.init();
	}

	public void loadSettings(ItemStack blueprint) {
		CompoundNBT tag = blueprint.getTag();
		BlockPos anchor = BlockPos.ZERO;
		PlacementSettings settings = SchematicItem.getSettings(blueprint);
		transformation = new SchematicTransformation();

		deployed = tag.getBoolean("Deployed");
		if (deployed)
			anchor = NBTUtil.readBlockPos(tag.getCompound("Anchor"));
		BlockPos size = NBTUtil.readBlockPos(tag.getCompound("Bounds"));

		bounds = new AxisAlignedBB(BlockPos.ZERO, size);
		outline = new AABBOutline(bounds);
		outline.getParams()
			.colored(0x6886c5)
			.lineWidth(1 / 16f);
		transformation.init(anchor, settings, bounds);
	}

	public void deploy() {
		if (!deployed) {
			List<Tools> tools = Tools.getTools(Minecraft.getInstance().player.isCreative());
			selectionScreen = new ToolSelectionScreen(tools, this::equip);
		}
		deployed = true;
		setupRenderer();
	}

	public String getCurrentSchematicName() {
		return displayedSchematic != null ? displayedSchematic : "-";
	}

	public void printInstantly() {
		NetworkDispatcher.sendToServer(new SchematicPlacePacket(activeSchematicItem.copy()));
		CompoundNBT nbt = activeSchematicItem.getTag();
		nbt.putBoolean("Deployed", false);
		activeSchematicItem.setTag(nbt);
		SchematicInstances.clearHash(activeSchematicItem);
		renderers.forEach(r -> r.setActive(false));
		active = false;
		markDirty();
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
