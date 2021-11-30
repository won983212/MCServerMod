package com.won983212.servermod.schematic.parser;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.won983212.servermod.schematic.IProgressEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.Template;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SchematicFileParser {

    private static final Cache<String, Template> schematicCache;
    private static final HashMap<String, AbstractSchematicReader> extensionToReaderMap = new HashMap<>();

    static {
        schematicCache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build();
        extensionToReaderMap.put("schematic", new MCEditSchematicReader());
        extensionToReaderMap.put("schem", new SpongeSchematicReader());
        extensionToReaderMap.put("nbt", new VanillaSchematicReader());
    }

    public static boolean isSupportedExtension(String fileName) {
        return extensionToReaderMap.containsKey(fileName.substring(fileName.lastIndexOf('.') + 1));
    }

    public static Template parseSchematicFile(File file, IProgressEvent event) throws IOException {
        String filePath = file.getAbsolutePath();
        try {
            return schematicCache.get(filePath, () -> {
                AbstractSchematicReader reader = getSupportedReader(file);
                reader.setProgressEvent(event);
                return reader.parse(file);
            });
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof IOException) {
                throw (IOException) t;
            } else {
                throw new IOException(t);
            }
        }
    }

    public static BlockPos parseSchematicBounds(File file) throws IOException {
        String filePath = file.getAbsolutePath();
        Template t = schematicCache.getIfPresent(filePath);
        if (t != null) {
            return t.getSize();
        }
        return getSupportedReader(file).parseSize(file);
    }

    private static AbstractSchematicReader getSupportedReader(File file) throws IOException {
        String fileName = file.getName();
        String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
        AbstractSchematicReader reader = extensionToReaderMap.get(fileExtension);
        if (reader != null) {
            return reader;
        } else {
            throw new IOException("Unsupported type: " + fileExtension);
        }
    }
}
