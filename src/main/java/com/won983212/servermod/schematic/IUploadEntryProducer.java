package com.won983212.servermod.schematic;

import java.util.Map;

public interface IUploadEntryProducer {
    Iterable<Map.Entry<String, ClientSchematicLoader.SchematicUploadEntry>> getUploadEntries();

    int size();
}