package com.won983212.servermod.schematic.client.tools;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.ModTextures;
import com.won983212.servermod.client.ClientDist;
import com.won983212.servermod.client.render.outliner.AABBOutline;
import com.won983212.servermod.client.render.SuperRenderTypeBuffer;
import com.won983212.servermod.schematic.client.SchematicHandler;
import com.won983212.servermod.schematic.client.SchematicTransformation;
import com.won983212.servermod.utility.AnimationTickHolder;
import com.won983212.servermod.utility.RaycastHelper;
import com.won983212.servermod.utility.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Arrays;
import java.util.List;

public abstract class SchematicToolBase implements ISchematicTool {

	protected SchematicHandler schematicHandler;

	protected BlockPos selectedPos;
	protected Vector3d chasingSelectedPos;
	protected Vector3d lastChasingSelectedPos;

	protected boolean selectIgnoreBlocks;
	protected int selectionRange;
	protected boolean schematicSelected;
	protected boolean renderSelectedFace;
	protected Direction selectedFace;

	@Override
	public void init() {
		schematicHandler = ClientDist.SCHEMATIC_HANDLER;
		selectedPos = null;
		selectedFace = null;
		schematicSelected = false;
		chasingSelectedPos = Vector3d.ZERO;
		lastChasingSelectedPos = Vector3d.ZERO;
	}

	@Override
	public void updateSelection() {
		updateTargetPos();

		if (selectedPos == null)
			return;
		lastChasingSelectedPos = chasingSelectedPos;
		Vector3d target = Vector3d.atLowerCornerOf(selectedPos);
		if (target.distanceTo(chasingSelectedPos) < 1 / 512f) {
			chasingSelectedPos = target;
			return;
		}

		chasingSelectedPos = chasingSelectedPos.add(target.subtract(chasingSelectedPos)
			.scale(1 / 2f));
	}

	public void updateTargetPos() {
		ClientPlayerEntity player = Minecraft.getInstance().player;

		// Select Blueprint
		if (schematicHandler.isDeployed()) {
			SchematicTransformation transformation = schematicHandler.getTransformation();
			AxisAlignedBB localBounds = schematicHandler.getBounds();

			Vector3d traceOrigin = RaycastHelper.getTraceOrigin(player);
			Vector3d start = transformation.toLocalSpace(traceOrigin);
			Vector3d end = transformation.toLocalSpace(RaycastHelper.getTraceTarget(player, 70, traceOrigin));
			RaycastHelper.PredicateTraceResult result =
				RaycastHelper.rayTraceUntil(start, end, pos -> localBounds.contains(VecHelper.getCenterOf(pos)));

			schematicSelected = !result.missed();
			selectedFace = schematicSelected ? result.getFacing() : null;
		}

		boolean snap = this.selectedPos == null;

		// Select location at distance
		if (selectIgnoreBlocks) {
			float pt = AnimationTickHolder.getPartialTicks();
			selectedPos = new BlockPos(player.getEyePosition(pt)
				.add(player.getLookAngle()
					.scale(selectionRange)));
			if (snap)
				lastChasingSelectedPos = chasingSelectedPos = Vector3d.atLowerCornerOf(selectedPos);
			return;
		}

		// Select targeted Block
		selectedPos = null;
		BlockRayTraceResult trace = RaycastHelper.rayTraceRange(player.level, player, 75);
		if (trace == null || trace.getType() != Type.BLOCK)
			return;

		BlockPos hit = new BlockPos(trace.getLocation());
		boolean replaceable = player.level.getBlockState(hit)
			.getMaterial()
			.isReplaceable();
		if (trace.getDirection()
			.getAxis()
			.isVertical() && !replaceable)
			hit = hit.relative(trace.getDirection());
		selectedPos = hit;
		if (snap)
			lastChasingSelectedPos = chasingSelectedPos = Vector3d.atLowerCornerOf(selectedPos);
	}

	@Override
	public void renderTool(MatrixStack ms, SuperRenderTypeBuffer buffer) {}

	@Override
	public void renderOverlay(MatrixStack ms, IRenderTypeBuffer buffer) {}

	@Override
	public void renderOnSchematic(MatrixStack ms, SuperRenderTypeBuffer buffer) {
		if (!schematicHandler.isDeployed())
			return;

		ms.pushPose();
		AABBOutline outline = schematicHandler.getOutline();
		if (renderSelectedFace) {
			outline.getParams()
				.highlightFace(selectedFace)
				.withFaceTextures(ModTextures.CHECKERED,
						Screen.hasControlDown() ? ModTextures.HIGHLIGHT_CHECKERED : ModTextures.CHECKERED);
		}
		outline.getParams()
			.colored(0x6886c5)
			.withFaceTexture(ModTextures.CHECKERED)
			.lineWidth(1 / 16f);
		outline.render(ms, buffer, AnimationTickHolder.getPartialTicks());
		outline.getParams().clearTextures();
		ms.popPose();
	}

}
