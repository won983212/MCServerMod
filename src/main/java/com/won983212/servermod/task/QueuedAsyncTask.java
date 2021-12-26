package com.won983212.servermod.task;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class QueuedAsyncTask {
    private int groupId;
    private IAsyncTask task;
    private Consumer<Exception> exceptionHandler;
    private Supplier<IAsyncTask> completeTaskSupplier;
    private QueuedAsyncTask completeTaskChainLink;
    private boolean completed;


    protected QueuedAsyncTask(IAsyncTask task) {
        this.task = task;
        this.completed = false;
    }

    protected QueuedAsyncTask(int id) {
        this.task = null;
        this.completed = false;
        this.groupId = id;
    }

    public QueuedAsyncTask exceptionally(Consumer<Exception> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public QueuedAsyncTask then(Runnable whenComplete) {
        return then(() -> {
            whenComplete.run();
            return null;
        });
    }

    public QueuedAsyncTask groupId(int id) {
        this.groupId = id;
        return this;
    }

    public QueuedAsyncTask then(Supplier<IAsyncTask> nextAsyncTaskSupplier) {
        this.completeTaskSupplier = nextAsyncTaskSupplier;
        this.completeTaskChainLink = new QueuedAsyncTask(groupId);
        return completeTaskChainLink;
    }

    public QueuedAsyncTask complete() {
        completed = true;
        if (task != null && completeTaskSupplier != null) {
            if (completeTaskChainLink.exceptionHandler == null) {
                completeTaskChainLink.exceptionHandler = exceptionHandler;
            }
            completeTaskChainLink.task = completeTaskSupplier.get();
            return completeTaskChainLink;
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


    public static class ExceptionTask extends QueuedAsyncTask {
        private final Exception exception;

        public ExceptionTask(Exception exception) {
            super(null);
            this.exception = exception;
        }

        public QueuedAsyncTask exceptionally(Consumer<Exception> exceptionHandler) {
            if (exceptionHandler != null) {
                exceptionHandler.accept(exception);
            }
            return this;
        }

        public boolean tick() {
            return false;
        }
    }
}