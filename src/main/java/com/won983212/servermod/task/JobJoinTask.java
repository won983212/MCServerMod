package com.won983212.servermod.task;

import java.util.Arrays;

public class JobJoinTask implements IAsyncTask {
    private final QueuedAsyncTask[] tasks;

    public JobJoinTask(QueuedAsyncTask... tasks) {
        this.tasks = tasks;
    }

    @Override
    public boolean tick() {
        long completed = Arrays.stream(tasks)
                .filter(QueuedAsyncTask::isCompleted)
                .count();
        return completed != tasks.length;
    }
}
