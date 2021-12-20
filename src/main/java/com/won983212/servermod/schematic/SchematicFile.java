package com.won983212.servermod.schematic;

import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.network.ServerSchematicLoader;
import com.won983212.servermod.schematic.parser.SchematicFileParser;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SchematicFile {
    private final String name;
    private final String hash;

    private SchematicFile(File f) {
        this.name = f.getName();
        this.hash = loadHash(f);
    }

    public SchematicFile(PacketBuffer readBuffer) {
        this.name = readBuffer.readUtf(64);
        this.hash = readBuffer.readUtf(64);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof SchematicFile)) {
            return false;
        } else {
            SchematicFile file = (SchematicFile) obj;
            return file.hash.equals(hash) && file.name.equals(name);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, name);
    }

    @Override
    public String toString() {
        return "SchematicFile[" + name + "]";
    }

    public void writeTo(PacketBuffer buffer) {
        buffer.writeUtf(name, 64);
        buffer.writeUtf(hash, 64);
    }

    public boolean equals(Path filePath) {
        File file = filePath.toFile();
        if (!file.exists()) {
            return false;
        }
        return loadHash(file).equals(hash);
    }

    private static String loadHash(File f) {
        String hash = null;
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            hash = DigestUtils.md5Hex(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return hash;
    }

    public static List<SchematicFile> getFileList(String owner) {
        Path dir = getDirectoryPath(owner);
        File[] files = dir.toFile().listFiles();

        if (files == null) {
            Logger.warn("Can't find directory path: " + dir);
            return new ArrayList<>();
        }

        return Arrays.stream(files)
                .filter(File::isFile)
                .filter((f) -> !SchematicFileParser.isUnsupportedExtension(f.getName()))
                .map(SchematicFile::new)
                .collect(Collectors.toList());
    }

    public static Path getFilePath(String owner, String schematicFile) throws IOException {
        Path dir = getDirectoryPath(owner);
        Path path = dir.resolve(Paths.get(schematicFile)).normalize();
        if (!path.startsWith(dir)) {
            throw new IOException("Can't resolve path: " + path);
        }
        return path;
    }

    public static Path getDirectoryPath(String owner) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            return Paths.get("schematics", ServerSchematicLoader.USER_SCHEMATIC_DIR_NAME, owner).toAbsolutePath();
        } else {
            return Paths.get("schematics").toAbsolutePath();
        }
    }
}
