package com.won983212.servermod.schematic.client.tools;

import com.mojang.blaze3d.matrix.MatrixStack;

import com.won983212.servermod.ModKeys;
import com.won983212.servermod.client.render.outliner.AABBOutline;
import com.won983212.servermod.client.render.SuperRenderTypeBuffer;
import com.won983212.servermod.schematic.client.SchematicTransformation;
import com.won983212.servermod.utility.AnimationTickHolder;
import com.won983212.servermod.utility.MatrixTransformStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public class DeployTool extends PlacementToolBase {

	@Override
	public void init() {
		super.init();
		selectionRange = -1;
	}

	@Override
	public void updateSelection() {
		if (schematicHandler.isActive() && selectionRange == -1) {
			selectionRange = (int) (schematicHandler.getBounds()
				.getCenter()
				.length() / 2);
			selectionRange = MathHelper.clamp(selectionRange, 1, 100);
		}
		selectIgnoreBlocks = ModKeys.KEY_ACTIVATE_TOOL.isDown();
		super.updateSelection();
	}

	@Override
	public void renderTool(MatrixStack ms, SuperRenderTypeBuffer buffer) {
		super.renderTool(ms, buffer);

		if (selectedPos == null)
			return;

		ms.pushPose();
		float pt = AnimationTickHolder.getPartialTicks();
		double x = MathHelper.lerp(pt, lastChasingSelectedPos.x, chasingSelectedPos.x);
		double y = MathHelper.lerp(pt, lastChasingSelectedPos.y, chasingSelectedPos.y);
		double z = MathHelper.lerp(pt, lastChasingSelectedPos.z, chasingSelectedPos.z);

		SchematicTransformation transformation = schematicHandler.getTransformation();
		AxisAlignedBB bounds = schematicHandler.getBounds();
		Vector3d center = bounds.getCenter();
		Vector3d rotationOffset = transformation.getRotationOffset(true);
		int centerX = (int) center.x;
		int centerZ = (int) center.z;
		double xOrigin = bounds.getXsize() / 2f;
		double zOrigin = bounds.getZsize() / 2f;
		Vector3d origin = new Vector3d(xOrigin, 0, zOrigin);

		ms.translate(x - centerX, y, z - centerZ);
		MatrixTransformStack.of(ms)
			.translate(origin)
			.translate(rotationOffset)
			.rotateY(transformation.getCurrentRotation())
			.translateBack(rotationOffset)
			.translateBack(origin);

		AABBOutline outline = schematicHandler.getOutline();
		outline.render(ms, buffer, pt);
		outline.getParams()
			.clearTextures();
		ms.popPose();
	}

	@Override
	public boolean handleMouseWheel(double delta) {
		if (!selectIgnoreBlocks)
			return super.handleMouseWheel(delta);
		selectionRange += delta;
		selectionRange = MathHelper.clamp(selectionRange, 1, 100);
		return true;
	}

	@Override
	public boolean handleRightClick() {
		if (selectedPos == null)
			return super.handleRightClick();
		Vector3d center = schematicHandler.getBounds()
			.getCenter();
		BlockPos target = selectedPos.offset(-((int) center.x), 0, -((int) center.z));

		ItemStack item = schematicHandler.getActiveSchematicItem();
		if (item != null) {
			item.getTag()
				.putBoolean("Deployed", true);
			item.getTag()
				.put("Anchor", NBTUtil.writeBlockPos(target));
		}

		schematicHandler.getTransformation()
			.moveTo(target);
		schematicHandler.markDirty();
		schematicHandler.deploy();
		return true;
	}

}
