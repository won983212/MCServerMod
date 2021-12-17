package com.won983212.servermod.task;

import java.util.LinkedList;
import java.util.Queue;

public class TaskManager {
    private static final Queue<IAsyncTask> ASYNC_TASKS = new LinkedList<>();

    public static void cancelAllTask() {
        ASYNC_TASKS.clear();
    }

    public static void addAsyncTask(IAsyncTask task) {
        ASYNC_TASKS.offer(task);
    }

    public static void tick() {
        IAsyncTask task = ASYNC_TASKS.peek();
        if (task != null && !task.tick()) {
            ASYNC_TASKS.poll();
        }
    }
}
