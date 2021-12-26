package com.won983212.servermod.task;

public interface IAsyncNoResultTask extends IAsyncTask<Void> {
    /**
     * @return is this task end?
     */
    boolean tick();

    default Void getResult() {
        return null;
    }
}
