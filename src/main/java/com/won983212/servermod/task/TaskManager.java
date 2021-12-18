package com.won983212.servermod.task;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Supplier;

public class TaskManager {
    private static final Queue<MinecraftTickAsyncTask> ASYNC_TASKS = new LinkedList<>();

    public static void cancelAllTask() {
        ASYNC_TASKS.clear();
    }

    public static MinecraftTickAsyncTask addAsyncTask(IAsyncTask task) {
        MinecraftTickAsyncTask ret = new MinecraftTickAsyncTask(task);
        ASYNC_TASKS.offer(ret);
        return ret;
    }

    public static void tick() {
        MinecraftTickAsyncTask ent = ASYNC_TASKS.peek();
        if (ent != null && !ent.task.tick()) {
            if (ent.thenTask != null) {
                MinecraftTickAsyncTask thenTask = ent.thenTask.get();
                if (thenTask != null) {
                    ASYNC_TASKS.offer(thenTask);
                }
            }
            ASYNC_TASKS.poll();
        }
    }

    public static class MinecraftTickAsyncTask {
        private final IAsyncTask task;
        private Supplier<MinecraftTickAsyncTask> thenTask;

        private MinecraftTickAsyncTask(IAsyncTask task) {
            this.task = task;
        }

        public void thenAsync(Supplier<IAsyncTask> thenTask) {
            this.thenTask = () -> new MinecraftTickAsyncTask(thenTask.get());
        }
    }
}
