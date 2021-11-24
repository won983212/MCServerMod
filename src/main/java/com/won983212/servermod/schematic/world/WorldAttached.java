package com.won983212.servermod.schematic.world;

import net.minecraft.world.IWorld;
import net.minecraftforge.common.util.NonNullFunction;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldAttached<T> {

    static List<Map<IWorld, ?>> allMaps = new ArrayList<>();
    Map<IWorld, T> attached;
    private final NonNullFunction<IWorld, T> factory;

    public WorldAttached(NonNullFunction<IWorld, T> factory) {
        this.factory = factory;
        attached = new HashMap<>();
        allMaps.add(attached);
    }

    @Nonnull
    public T get(IWorld world) {
        T t = attached.get(world);
        if (t != null) {
            return t;
        }
        T entry = factory.apply(world);
        put(world, entry);
        return entry;
    }

    public void put(IWorld world, T entry) {
        attached.put(world, entry);
    }

}
