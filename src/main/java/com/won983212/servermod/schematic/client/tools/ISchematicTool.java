package com.won983212.servermod.schematic.client.tools;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.client.render.SuperRenderTypeBuffer;
import net.minecraft.client.renderer.IRenderTypeBuffer;

public interface ISchematicTool {

	public void init();
	public void updateSelection();
	
	public boolean handleRightClick();
	public boolean handleMouseWheel(double delta);
	
	public void renderTool(MatrixStack ms, SuperRenderTypeBuffer buffer);
	public void renderOverlay(MatrixStack ms, IRenderTypeBuffer buffer);
	public void renderOnSchematic(MatrixStack ms, SuperRenderTypeBuffer buffer);
	
}
