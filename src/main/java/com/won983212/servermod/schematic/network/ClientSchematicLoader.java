package com.won983212.servermod.schematic.network;

import com.won983212.servermod.Logger;
import com.won983212.servermod.network.NetworkDispatcher;
import com.won983212.servermod.schematic.IProgressEntry;
import com.won983212.servermod.schematic.IProgressEntryProducer;
import com.won983212.servermod.schematic.SchematicFile;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientSchematicLoader extends SchematicNetwork implements IProgressEntryProducer {
    private final Map<String, SchematicUploadEntry> activeUploads = new HashMap<>();
    private int packetCycle;

    public ClientSchematicLoader() {
        createFolderIfMissing("schematics");
    }

    public void tick() {
        if (activeUploads.isEmpty()) {
            return;
        }
        if (packetCycle-- > 0) {
            return;
        }
        packetCycle = PACKET_DELAY;

        for (String schematic : new HashSet<>(activeUploads.keySet())) {
            continueUpload(schematic);
        }
    }

    public void startNewUpload(SchematicFile schematic) {
        String name = schematic.getName();
        if (activeUploads.containsKey(name)) {
            Minecraft.getInstance().gui.getChat().addMessage(new TranslationTextComponent("servermod.message.uploadalready"));
            return;
        }

        Path path = Paths.get("schematics", name);

        if (!Files.exists(path)) {
            Logger.error("Missing Schematic file: " + path.toString());
            return;
        }

        InputStream in;
        try {
            long size = Files.size(path);

            // Too big
            if (!Minecraft.getInstance().hasSingleplayerServer() &&
                    isSchematicSizeTooBig(Minecraft.getInstance().player, size)) {
                Logger.warn(name + " is too big. Cancel uploading: " + size);
                return;
            }

            in = Files.newInputStream(path, StandardOpenOption.READ);
            activeUploads.put(name, new SchematicUploadEntry(name, in, size));
            NetworkDispatcher.sendToServer(CSchematicUpload.begin(schematic, size));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void continueUpload(String schematic) {
        if (activeUploads.containsKey(schematic)) {
            final int maxPacketSize = SCHEMATIC_PACKET_SIZE;
            byte[] data = new byte[maxPacketSize];
            try {
                SchematicUploadEntry ent = activeUploads.get(schematic);
                int status = ent.stream.read(data);

                if (status != -1) {
                    if (status < maxPacketSize) {
                        data = Arrays.copyOf(data, status);
                    }
                    if (Minecraft.getInstance().level != null) {
                        NetworkDispatcher.sendToServer(CSchematicUpload.write(schematic, data));
                    } else {
                        activeUploads.remove(schematic);
                        return;
                    }
                }

                if (status < maxPacketSize) {
                    finishUpload(schematic);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void finishUpload(String schematic) {
        if (activeUploads.containsKey(schematic)) {
            NetworkDispatcher.sendToServer(CSchematicUpload.finish(schematic));
            activeUploads.remove(schematic);
        }
    }

    public void handleFailState(String schematic) {
        activeUploads.remove(schematic);
    }

    public void handleServerProgress(String schematic, float serverProgress) {
        SchematicUploadEntry ent = activeUploads.get(schematic);
        if (ent != null) {
            ent.serverSideProgress = serverProgress;
            Logger.debug("Uploaded " + schematic + ": " + serverProgress);
        }
    }

    @Override
    public Iterable<SchematicUploadEntry> getProgressEntries(){
        return activeUploads.values();
    }

    @Override
    public int size() {
        return activeUploads.size();
    }

    public static class SchematicUploadEntry implements IProgressEntry {
        private static final String[] SIZE_UNITS = {"B", "KB", "MB", "GB"};
        private final String key;
        private final InputStream stream;
        private final long totalBytes;
        private float serverSideProgress;

        SchematicUploadEntry(String key, InputStream stream, long size) {
            this.key = key;
            this.stream = stream;
            this.totalBytes = size;
            this.serverSideProgress = 0;
        }

        @Override
        public String getTitle() {
            return key;
        }

        @Override
        public String getSubtitle() {
            long size = totalBytes;
            int unitIdx = 0;
            while (size >= 1024) {
                unitIdx++;
                size /= 1024;
            }
            return size + SIZE_UNITS[unitIdx];
        }

        @Override
        public double getProgress() {
            return serverSideProgress;
        }
    }
}
