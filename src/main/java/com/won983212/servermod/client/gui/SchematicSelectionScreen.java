package com.won983212.servermod.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.servermod.ModTextures;
import com.won983212.servermod.client.ClientDist;
import com.won983212.servermod.client.gui.component.HoveringCover;
import com.won983212.servermod.client.gui.component.ScrollSelector;
import com.won983212.servermod.network.NetworkDispatcher;
import com.won983212.servermod.schematic.packet.CSchematicFileDelete;
import com.won983212.servermod.server.command.SchematicCommand;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SchematicSelectionScreen extends PanelScreen {
    private final List<String> uploadedFiles = new ArrayList<>();
    private final ScrollSelector<String> fileSelector = new ScrollSelector<>(45, 21, 139, 18, uploadedFiles);
    private HoveringCover uploadBtn;
    private HoveringCover deleteBtn;

    public SchematicSelectionScreen(List<String> serverSideFiles) {
        super(new StringTextComponent("Schematic Selection Screen"));

        Set<String> clientFileSet = new HashSet<>(SchematicCommand.getFileList(null));
        for (String serverFile : serverSideFiles) {
            if (!clientFileSet.contains(serverFile)) {
                continue;
            }
            uploadedFiles.add(serverFile);
            clientFileSet.remove(serverFile);
        }
        uploadedFiles.addAll(clientFileSet.stream()
                .map((name) -> "§6" + name)
                .collect(Collectors.toList()));
        uploadedFiles.sort(String::compareTo);
    }

    private void onAccept(HoveringCover btn) {
        String fileName = uploadedFiles.get(fileSelector.getSelectedIndex());
        if (btn == uploadBtn) {
            fileName = StringUtils.stripColor(fileName);
            ClientDist.SCHEMATIC_SENDER.startNewUpload(fileName);
            onClose();
        } else if (btn == deleteBtn) {
            if (fileName.startsWith("§")) {
                alert("서버에 업로드된 파일만 삭제할 수 있습니다!");
                return;
            }
            NetworkDispatcher.sendToServer(new CSchematicFileDelete(fileName));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.uploadBtn = new HoveringCover(178, 55, 18, 18, SchematicSelectionScreen.this::onAccept);
        this.children.add(uploadBtn);
        this.deleteBtn = new HoveringCover(152, 55, 18, 18, SchematicSelectionScreen.this::onAccept);
        this.children.add(deleteBtn);
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
        int index = uploadedFiles.indexOf(fileName);
        if (index >= 0) {
            uploadedFiles.set(index, "§6" + fileName);
        }
    }
}
