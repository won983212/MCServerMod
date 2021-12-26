package com.won983212.servermod.task;

import java.util.Arrays;

public class JobJoinTask<T> implements IAsyncTask<T> {
    private final QueuedAsyncTask<?>[] tasks;
    private T result;

    public JobJoinTask(QueuedAsyncTask<?>... tasks) {
        this.tasks = tasks;
    }

    @Override
    public boolean tick() {
        long completed = Arrays.stream(tasks)
                .filter(QueuedAsyncTask::isCompleted)
                .count();
        return completed != tasks.length;
    }

    @Override
    public T getResult() {
        return result;
    }

    public JobJoinTask<T> setResult(T result) {
        this.result = result;
        return this;
    }
}
