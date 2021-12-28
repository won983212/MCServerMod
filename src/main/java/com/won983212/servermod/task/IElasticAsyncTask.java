package com.won983212.servermod.task;

public interface IElasticAsyncTask<T> extends IAsyncTask<T> {
    /**
     * for milliseconds
     */
    long getCriteriaTime();

    boolean elasticTick(int count);

    default boolean tick() {
        return elasticTick(1);
    }

    default int getInitialBatchCount() {
        return 10000;
    }

    default int getMaxBatchCount(){
        return Integer.MAX_VALUE;
    }
}
