package com.won983212.servermod.schematic.parser;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.IProgressEvent;
import com.won983212.servermod.schematic.SchematicFile;
import com.won983212.servermod.schematic.container.SchematicContainer;
import com.won983212.servermod.task.FinishedTask;
import com.won983212.servermod.task.IAsyncTask;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class SchematicFileParser {

    private static final Cache<String, SchematicContainer> schematicCache;
    private static final HashMap<String, Class<? extends AbstractSchematicReader>> extensionToReaderMap = new HashMap<>();

    static {
        schematicCache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build();
        extensionToReaderMap.put("schematic", MCEditSchematicReader.class);
        extensionToReaderMap.put("schem", SpongeSchematicReader.class);
    }

    public static boolean isUnsupportedExtension(String fileName) {
        return !extensionToReaderMap.containsKey(fileName.substring(fileName.lastIndexOf('.') + 1));
    }

    public static BlockPos parseSchematicSizeFromItem(ItemStack blueprint) {
        try {
            File file = getSchematicPath(blueprint).toFile();
            SchematicContainer t = schematicCache.getIfPresent(file.getAbsolutePath());
            if (t != null) {
                return t.getSize();
            }
            return createSupportedReader(file).parseSize();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BlockPos.ZERO;
    }

    public static IAsyncTask<SchematicContainer> parseSchematicFromItemAsync(ItemStack blueprint, IProgressEvent event) {
        try {
            Path path = getSchematicPath(blueprint);
            return SchematicFileParser.parseSchematicFileAsync(path.toFile(), event);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new FinishedTask<>(new SchematicContainer(BlockPos.ZERO));
    }

    public static IAsyncTask<SchematicContainer> parseSchematicFileAsync(File file, IProgressEvent event) throws IOException {
        String filePath = file.getAbsolutePath();
        SchematicContainer container = schematicCache.getIfPresent(filePath);

        if (container != null) {
            return new FinishedTask<>(container);
        }

        AbstractSchematicReader reader = createSupportedReader(file);
        reader.setProgressEvent(event);
        reader.setCompletedEvent((c) -> schematicCache.put(filePath, c));
        return reader;
    }

    public static void clearCache() {
        schematicCache.invalidateAll();
    }

    private static Path getSchematicPath(ItemStack blueprint) throws IOException {
        String owner = blueprint.getTag().getString("Owner");
        String schematic = blueprint.getTag().getString("File");
        if (SchematicFileParser.isUnsupportedExtension(schematic)) {
            throw new IOException("Unsupported file!");
        }
        return SchematicFile.getFilePath(owner, schematic);
    }

    private static AbstractSchematicReader createSupportedReader(File file) throws IOException {
        String fileName = file.getName();
        String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
        AbstractSchematicReader reader = null;
        try {
            reader = extensionToReaderMap.get(fileExtension)
                    .getConstructor(File.class)
                    .newInstance(file);
        } catch (InstantiationException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException e) {
            Logger.error(e);
        }
        if (reader != null) {
            return reader;
        } else {
            throw new IOException("Unsupported type: " + fileExtension);
        }
    }
}
