package com.won983212.servermod.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.ModTextures;
import com.won983212.servermod.client.ClientDist;
import com.won983212.servermod.client.gui.component.HoveringCover;
import com.won983212.servermod.client.gui.component.ScrollSelector;
import com.won983212.servermod.server.command.SchematicCommand;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SchematicSelectionScreen extends PanelScreen {
    private final List<String> uploadedFiles = new ArrayList<>();
    private final ScrollSelector<String> fileSelector = new ScrollSelector<>(45, 21, 139, 18, uploadedFiles);

    public SchematicSelectionScreen(List<String> uploadedFiles) {
        super(new StringTextComponent("Schematic Selection Screen"));

        Set<String> fileNameSet = new HashSet<>();
        fileNameSet.addAll(uploadedFiles);
        fileNameSet.addAll(SchematicCommand.getFileList(null));
        this.uploadedFiles.addAll(fileNameSet);
    }

    private void onAccept(HoveringCover btn){
        String fileName = uploadedFiles.get(fileSelector.getSelectedIndex());
        ClientDist.SCHEMATIC_SENDER.startNewUpload(fileName);
        onClose();
    }

    @Override
    protected void init() {
        super.init();
        this.children.add(new HoveringCover(178, 55, 18, 18, SchematicSelectionScreen.this::onAccept));
        this.children.add(fileSelector);
        applyBackgroundOffset(ModTextures.SCHEMATIC_SELECT_BACKGROUND);
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTime) {
        this.renderBackground(ms);
        drawTexturedBackground(ms, ModTextures.SCHEMATIC_SELECT_BACKGROUND);
        super.render(ms, mouseX, mouseY, partialTime);
    }
}
