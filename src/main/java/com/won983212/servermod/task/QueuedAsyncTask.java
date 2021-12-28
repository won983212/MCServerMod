package com.won983212.servermod.task;

import com.won983212.servermod.Logger;

import java.util.function.Consumer;
import java.util.function.Function;

public class QueuedAsyncTask<T> {
    private int groupId;
    private IAsyncTask<T> task;
    private Consumer<Exception> exceptionHandler;
    private CompleteResultTask<T, ?> completeTask;
    private boolean completed;
    private int batchCount;
    private IElasticAsyncTask<T> elasticTask;


    protected QueuedAsyncTask(IAsyncTask<T> task) {
        this.setTask(task);
        this.completed = false;
    }

    protected QueuedAsyncTask(int id) {
        this(null);
        this.groupId = id;
    }

    public QueuedAsyncTask<T> setTask(IAsyncTask<T> task) {
        this.task = task;
        if (task instanceof IElasticAsyncTask) {
            this.elasticTask = (IElasticAsyncTask<T>) task;
            this.batchCount = elasticTask.initialBatchCount();
        } else {
            this.elasticTask = null;
        }
        return this;
    }

    public QueuedAsyncTask<T> groupId(int id) {
        this.groupId = id;
        return this;
    }

    public QueuedAsyncTask<T> exceptionally(Consumer<Exception> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public QueuedAsyncTask<Void> whenComplete(Runnable runnable) {
        return then((c) -> {
            runnable.run();
            return null;
        });
    }

    public QueuedAsyncTask<Void> thenAccept(Consumer<T> consumer) {
        return then((c) -> {
            consumer.accept(c);
            return null;
        });
    }

    public <R> QueuedAsyncTask<R> then(Function<T, IAsyncTask<R>> nextAsyncTaskSupplier) {
        QueuedAsyncTask<R> task = new QueuedAsyncTask<>(groupId);
        completeTask = new CompleteResultTask<>(nextAsyncTaskSupplier, task);
        return task;
    }

    public QueuedAsyncTask<?> complete() {
        completed = true;
        if (task != null && completeTask != null) {
            completeTask.overrideExceptionHandler(this);
            try {
                completeTask.apply(task.getResult());
            } catch (Exception e) {
                if (exceptionHandler != null) {
                    exceptionHandler.accept(e);
                }
            }
            return completeTask.completeTaskChainLink;
        }
        return null;
    }

    public boolean tick() {
        if (task == null) {
            return false;
        }
        try {
            if (elasticTask != null) {
                long lastElapsedTime = System.currentTimeMillis();
                boolean success = elasticTask.elasticTick(batchCount);
                lastElapsedTime = System.currentTimeMillis() - lastElapsedTime;
                if (lastElapsedTime < elasticTask.criteriaTime()) {
                    if ((batchCount << 1) > 0) {
                        batchCount <<= 1;
                    }
                } else if (batchCount > 1) {
                    batchCount >>= 1;
                }
                return success;
            } else {
                return task.tick();
            }
        } catch (Exception e) {
            if (exceptionHandler != null) {
                exceptionHandler.accept(e);
            }
            cancel();
            return true;
        }
    }

    public void cancel() {
        task = null;
    }

    public boolean isCompleted() {
        return completed;
    }

    public int getGroupId() {
        return groupId;
    }

    private static class CompleteResultTask<T, R> {
        private final Function<T, IAsyncTask<R>> completeTaskSupplier;
        private final QueuedAsyncTask<R> completeTaskChainLink;

        public CompleteResultTask(Function<T, IAsyncTask<R>> supplier, QueuedAsyncTask<R> chainLink) {
            this.completeTaskSupplier = supplier;
            this.completeTaskChainLink = chainLink;
        }

        public void overrideExceptionHandler(QueuedAsyncTask<?> from) {
            if (completeTaskChainLink.exceptionHandler == null) {
                completeTaskChainLink.exceptionHandler = from.exceptionHandler;
            }
        }

        public void apply(T result) {
            completeTaskChainLink.setTask(completeTaskSupplier.apply(result));
        }
    }
}