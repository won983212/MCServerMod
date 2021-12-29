package com.won983212.servermod.server;

import com.mojang.brigadier.CommandDispatcher;
import com.won983212.servermod.server.command.ReloadSkinCommand;
import net.minecraft.command.CommandSource;

import java.util.function.Consumer;

public enum Commands {
    RELOAD_SKIN(ReloadSkinCommand::register);

    private final Consumer<CommandDispatcher<CommandSource>> registerFunc;

    Commands(Consumer<CommandDispatcher<CommandSource>> registerFunc) {
        this.registerFunc = registerFunc;
    }

    public void reigster(CommandDispatcher<CommandSource> dispatcher) {
        this.registerFunc.accept(dispatcher);
    }
}
