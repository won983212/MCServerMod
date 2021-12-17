package com.won983212.servermod.task;

public interface IAsyncTask {
    /**
     * @return is this task end?
     */
    boolean tick();
}
