package com.won983212.servermod.task;

public class FinishedTask<T> implements IAsyncTask<T> {
    private final T result;

    public FinishedTask(T result) {
        this.result = result;
    }

    @Override
    public boolean tick() {
        return false;
    }

    @Override
    public T getResult() {
        return result;
    }
}
