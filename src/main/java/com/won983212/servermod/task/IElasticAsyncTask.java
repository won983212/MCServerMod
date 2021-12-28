package com.won983212.servermod.task;

public interface IElasticAsyncTask<T> extends IAsyncTask<T> {
    /**
     * for milliseconds
     */
    long criteriaTime();

    boolean elasticTick(int count);

    default boolean tick() {
        return elasticTick(1);
    }

    default int initialBatchCount() {
        return 10000;
    }
}
