package com.won983212.servermod.schematic.parser;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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

    public static boolean isSupportedExtension(String ext) {
        return extensionToReaderMap.containsKey(ext);
    }

    public static Template parseSchematicFile(File file) throws IOException {
        String filePath = file.getAbsolutePath();
        try {
            return schematicCache.get(filePath, () -> cachingSchematic(file));
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof IOException) {
                throw (IOException) t;
            } else {
                throw new IOException(t);
            }
        }
    }

    private static Template cachingSchematic(File file) throws IOException {
        String fileName = file.getName();
        String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
        AbstractSchematicReader reader = extensionToReaderMap.get(fileExtension);
        if (reader != null) {
            return reader.parse(file);
        } else {
            throw new IOException("Unsupported type: " + fileExtension);
        }
    }
}
