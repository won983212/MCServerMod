package com.won983212.servermod.task;

import java.util.EnumMap;
import java.util.function.Supplier;

public class StagedTaskProcessor<L extends Enum<L>> {
    private final EnumMap<L, Supplier<Boolean>> parsePasses;
    private final Class<L> stageEnumClass;
    private L parseStage;
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

    public StagedTaskProcessor<L> stage(L current) {
        parseStage = current;
        return this;
    }

    public StagedTaskProcessor<L> finalStage(L stage) {
        lastStage = stage;
        return this;
    }

    public StagedTaskProcessor<L> completeEvent(Runnable runnable) {
        this.onComplete = runnable;
        return this;
    }

    public StagedTaskProcessor<L> nextStageEvent(Runnable runnable) {
        this.onNextStage = runnable;
        return this;
    }

    public boolean tick() {
        Supplier<Boolean> pass = parsePasses.get(parseStage);
        if (!pass.get()) {
            if (parseStage == lastStage) {
                if (onComplete != null) {
                    onComplete.run();
                }
                return false;
            }
            parseStage = stageEnumClass.getEnumConstants()[parseStage.ordinal() + 1];
            if (onNextStage != null) {
                onNextStage.run();
            }
        }
        return true;
    }
}
