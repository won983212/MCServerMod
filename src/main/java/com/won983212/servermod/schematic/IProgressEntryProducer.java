package com.won983212.servermod.schematic;

public interface IProgressEntryProducer {
    Iterable<? extends IProgressEntry> getProgressEntries();

    int size();
}