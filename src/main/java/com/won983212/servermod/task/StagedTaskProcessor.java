package com.won983212.servermod.task;

import java.util.EnumMap;
import java.util.function.Supplier;

public class StagedTaskProcessor<L extends Enum<L>> {
    private final EnumMap<L, Supplier<Boolean>> parsePasses;
    private final Class<L> stageEnumClass;
    private L stage;
    private L lastStage;
    private Runnable onComplete;
    private Runnable onNextStage;

    public StagedTaskProcessor(Class<L> stageEnumClass) {
        this.parsePasses = new EnumMap<>(stageEnumClass);
        this.stageEnumClass = stageEnumClass;

        L[] enumValues = stageEnumClass.getEnumConstants();
        this.lastStage = enumValues[enumValues.length - 1];
    }

    public void addStageHandler(L stage, Supplier<Boolean> handler) {
        parsePasses.put(stage, handler);
    }

    public boolean nextStage() {
        int current = stage.ordinal();
        if (current >= stageEnumClass.getEnumConstants().length) {
            return false;
        }
        stage = stageEnumClass.getEnumConstants()[stage.ordinal() + 1];
        return true;
    }

    public StagedTaskProcessor<L> stage(L current) {
        stage = current;
        return this;
    }

    public StagedTaskProcessor<L> finalStage(L stage) {
        lastStage = stage;
        return this;
    }

    public StagedTaskProcessor<L> onComplete(Runnable runnable) {
        this.onComplete = runnable;
        return this;
    }

    public StagedTaskProcessor<L> onNextStage(Runnable runnable) {
        this.onNextStage = runnable;
        return this;
    }

    public boolean tick() {
        Supplier<Boolean> pass = parsePasses.get(stage);
        if (pass == null) {
            return false;
        }
        if (!pass.get()) {
            if (stage == lastStage) {
                if (onComplete != null) {
                    onComplete.run();
                }
                return false;
            }
            if (nextStage()) {
                if (onNextStage != null) {
                    onNextStage.run();
                }
            } else if (onComplete != null) {
                onComplete.run();
                return false;
            }
        }
        return true;
    }
}
