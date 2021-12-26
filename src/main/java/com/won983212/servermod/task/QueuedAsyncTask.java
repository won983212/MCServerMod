package com.won983212.servermod.task;

import java.util.function.Consumer;
import java.util.function.Function;

public class QueuedAsyncTask<T> {
    private int groupId;
    private IAsyncTask<T> task;
    private Consumer<Exception> exceptionHandler;
    private CompleteResultTask<T, ?> completeTask;
    private boolean completed;


    protected QueuedAsyncTask(IAsyncTask<T> task) {
        this.task = task;
        this.completed = false;
    }

    protected QueuedAsyncTask(int id) {
        this.task = null;
        this.completed = false;
        this.groupId = id;
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
            return task.tick();
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
            completeTaskChainLink.task = completeTaskSupplier.apply(result);
        }
    }
}