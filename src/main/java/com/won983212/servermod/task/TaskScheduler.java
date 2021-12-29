package com.won983212.servermod.task;

import com.won983212.servermod.Logger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class TaskScheduler {
    private final Stack<Integer> groupIdContext = new Stack<>();
    private final Queue<QueuedAsyncTask<?>> taskWaitingQueue = new LinkedList<>();
    private int count = 0;
    private int current = 0;
    private QueuedAsyncTask<?>[] tasks = new QueuedAsyncTask<?>[1 << 4];


    public void cancelAllTask() {
        Arrays.fill(tasks, null);
        count = 0;
        taskWaitingQueue.clear();
    }

    public void cancelGroupTask(int groupId) {
        for (int i = 0; i < tasks.length; i++) {
            if (tasks[i] != null && tasks[i].getGroupId() == groupId) {
                tasks[i] = null;
                count--;
                Logger.debug("Canceled one: " + i);
            }
        }
    }

    public void pushGroupIdContext(int id) {
        groupIdContext.push(id);
    }

    public void popGroupIdContext() {
        groupIdContext.pop();
    }

    public <T> QueuedAsyncTask<T> addAsyncTask(IAsyncTask<T> task) {
        if (tasks.length <= count) {
            grow();
        }
        task.setScheduler(this);
        QueuedAsyncTask<T> gTask = new QueuedAsyncTask<T>(task);
        if (!groupIdContext.isEmpty()) {
            gTask.groupId(groupIdContext.peek());
        }
        taskWaitingQueue.offer(gTask);
        return gTask;
    }

    private void grow() {
        QueuedAsyncTask<?>[] newTasks = new QueuedAsyncTask<?>[tasks.length << 1];
        System.arraycopy(tasks, 0, newTasks, 0, tasks.length);
        tasks = newTasks;
    }

    public void tick() {
        pushWaitingTasks();
        if (count == 0) {
            return;
        }

        int cur = next();
        QueuedAsyncTask<?> ent = tasks[cur];
        if (ent == null || ent.tick()) {
            return;
        }

        QueuedAsyncTask<?> nextTask = ent.complete();
        if (nextTask != null) {
            tasks[cur] = nextTask;
        } else {
            tasks[cur] = null;
            count--;
        }
    }

    private void pushWaitingTasks() {
        if (tasks.length <= count) {
            return;
        }
        for (int i = 0; i < tasks.length && !taskWaitingQueue.isEmpty(); i++) {
            if (tasks[i] == null) {
                tasks[i] = taskWaitingQueue.poll();
                count++;
            }
        }
    }

    private int next() {
        if (count == 1 && tasks[current] != null) {
            return current;
        }

        int cur = current;
        int i;
        for (i = 0; i < tasks.length; i++) {
            current = (current + 1) % tasks.length;
            if (tasks[current] != null) {
                break;
            }
        }
        if (i == tasks.length) {
            Logger.warn("Can't find active async task! It's a bug!");
        }
        return cur;
    }
}
