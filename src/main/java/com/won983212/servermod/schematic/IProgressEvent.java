package com.won983212.servermod.schematic;

public interface IProgressEvent {
    void onProgress(String status, double progress);

    static void safeFire(IProgressEvent event, String status, double progress) {
        if (event != null) {
            event.onProgress(status, progress);
        }
    }
}