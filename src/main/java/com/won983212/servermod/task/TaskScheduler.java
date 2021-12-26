package com.won983212.servermod.task;

import com.won983212.servermod.Logger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class TaskScheduler {
    private static final Stack<Integer> GROUP_ID_CONTEXT = new Stack<>();
    private static final Queue<QueuedAsyncTask> TASK_WAITING_QUEUE = new LinkedList<>();
    private static int count = 0;
    private static int current = 0;
    private static QueuedAsyncTask[] tasks = new QueuedAsyncTask[1 << 4];


    public static void cancelAllTask() {
        Arrays.fill(tasks, null);
        count = 0;
        TASK_WAITING_QUEUE.clear();
    }

    public static void cancelGroupTask(int groupId) {
        for (int i = 0; i < tasks.length; i++) {
            if (tasks[i] != null && tasks[i].getGroupId() == groupId) {
                tasks[i] = null;
                count--;
                Logger.debug("Canceled one: " + i);
            }
        }
    }

    public static void pushGroupIdContext(int id) {
        GROUP_ID_CONTEXT.push(id);
    }

    public static void popGroupIdContext() {
        GROUP_ID_CONTEXT.pop();
    }

    public static QueuedAsyncTask addAsyncTask(IAsyncTask task) {
        if (tasks.length <= count) {
            grow();
        }
        QueuedAsyncTask gTask = new QueuedAsyncTask(task);
        if (!GROUP_ID_CONTEXT.isEmpty()) {
            gTask.groupId(GROUP_ID_CONTEXT.peek());
        }
        TASK_WAITING_QUEUE.offer(gTask);
        return gTask;
    }

    private static void grow() {
        QueuedAsyncTask[] newTasks = new QueuedAsyncTask[tasks.length << 1];
        System.arraycopy(tasks, 0, newTasks, 0, tasks.length);
        tasks = newTasks;
    }

    public static void tick() {
        pushWaitingTasks();
        if (count == 0) {
            return;
        }

        int cur = next();
        QueuedAsyncTask ent = tasks[cur];
        if (ent == null || ent.tick()) {
            return;
        }

        QueuedAsyncTask nextTask = ent.complete();
        if (nextTask != null) {
            tasks[cur] = nextTask;
        } else {
            tasks[cur] = null;
            count--;
        }
    }

    private static void pushWaitingTasks() {
        if (tasks.length <= count) {
            return;
        }
        for (int i = 0; i < tasks.length && !TASK_WAITING_QUEUE.isEmpty(); i++) {
            if (tasks[i] == null) {
                tasks[i] = TASK_WAITING_QUEUE.poll();
                count++;
            }
        }
    }

    private static int next() {
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
