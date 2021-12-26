package com.won983212.servermod.task;

public interface IAsyncTask<T> {
    /**
     * @return is this task end?
     */
    boolean tick();

    T getResult();
}
