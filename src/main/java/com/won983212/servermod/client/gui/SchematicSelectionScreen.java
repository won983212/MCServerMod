package com.won983212.servermod.client.gui;

import com.google.common.collect.Iterables;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.ModTextures;
import com.won983212.servermod.client.ClientDist;
import com.won983212.servermod.client.gui.component.HoveringCover;
import com.won983212.servermod.client.gui.component.ScrollSelector;
import com.won983212.servermod.network.NetworkDispatcher;
import com.won983212.servermod.schematic.SchematicFile;
import com.won983212.servermod.schematic.network.CSchematicFileDelete;
import net.minecraft.util.text.StringTextComponent;

import java.util.*;
import java.util.stream.Collectors;

public class SchematicSelectionScreen extends PanelScreen {
    private final List<SchematicFileValue> schematicFiles = new ArrayList<>();
    private ScrollSelector fileSelector;
    private HoveringCover uploadBtn;
    private HoveringCover deleteBtn;

    public SchematicSelectionScreen(List<SchematicFile> serverSideFiles) {
        super(new StringTextComponent("Schematic Selection Screen"));

        Set<SchematicFile> clientFileSet = new HashSet<>(SchematicFile.getFileList(null));
        for (SchematicFile serverFile : serverSideFiles) {
            if (!clientFileSet.contains(serverFile)) {
                continue;
            }
            schematicFiles.add(new SchematicFileValue(false, serverFile));
            clientFileSet.remove(serverFile);
        }
        schematicFiles.addAll(clientFileSet.stream()
                .map((t) -> new SchematicFileValue(true, t))
                .collect(Collectors.toList()));
        Collections.sort(schematicFiles);
    }

    private void onAccept(HoveringCover btn) {
        SchematicFileValue file = schematicFiles.get(fileSelector.getSelectedIndex());
        if (btn == uploadBtn) {
            ClientDist.SCHEMATIC_SENDER.startNewUpload(file.schematicFile);
            onClose();
        } else if (btn == deleteBtn) {
            if (file.needsUpload) {
                alert("서버에 업로드된 파일만 삭제할 수 있습니다!");
                return;
            }
            NetworkDispatcher.sendToServer(new CSchematicFileDelete(file.getName()));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.children.clear();
        this.uploadBtn = new HoveringCover(178, 55, 18, 18, SchematicSelectionScreen.this::onAccept);
        this.children.add(uploadBtn);
        this.deleteBtn = new HoveringCover(152, 55, 18, 18, SchematicSelectionScreen.this::onAccept);
        this.children.add(deleteBtn);
        this.fileSelector = new ScrollSelector(45, 21, 139, 18, schematicFiles);
        this.children.add(fileSelector);
        applyBackgroundOffset(ModTextures.SCHEMATIC_SELECT_BACKGROUND);
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTime) {
        this.renderBackground(ms);
        drawTexturedBackground(ms, ModTextures.SCHEMATIC_SELECT_BACKGROUND);
        super.render(ms, mouseX, mouseY, partialTime);
        drawCenteredString(ms, font, "§6[노란색]§r 으로 된 파일은 서버로 업로드가 필요한 파일입니다.", width / 2, (height + ModTextures.SCHEMATIC_SELECT_BACKGROUND.height) / 2 + 6, 0xffffffff);
        drawCenteredString(ms, font, "이미 업로드된 파일은 [삭제]버튼을 눌러 삭제할 수 있습니다.", width / 2, (height + ModTextures.SCHEMATIC_SELECT_BACKGROUND.height) / 2 + 20, 0xffffffff);
    }

    public void onResponseFileDeletion(String fileName) {
        alert(fileName + "이 서버에서 삭제되었습니다.", 1000);
        int index = Iterables.indexOf(schematicFiles, (t) -> t.getName().equals(fileName));
        if (index >= 0) {
            schematicFiles.get(index).needsUpload = true;
        }
    }

    private static class SchematicFileValue implements Comparable<SchematicFileValue> {
        public boolean needsUpload;
        public final SchematicFile schematicFile;

        public SchematicFileValue(boolean needsUpload, SchematicFile schematicFile) {
            this.needsUpload = needsUpload;
            this.schematicFile = schematicFile;
        }

        public String getName() {
            return schematicFile.getName();
        }

        @Override
        public int compareTo(SchematicFileValue o) {
            return schematicFile.getName().compareTo(o.schematicFile.getName());
        }

        @Override
        public String toString() {
            return needsUpload ? ("§6" + getName()) : getName();
        }
    }
}
