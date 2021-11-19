package com.won983212.servermod.schematic.parser;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ObjectIntIdentityMap;

public class VanillaPalette {
    private final ObjectIntIdentityMap<BlockState> ids = new ObjectIntIdentityMap<>(16);
    private int lastId = 0;

    public int idFor(BlockState state) {
        int i = this.ids.getId(state);
        if (i == -1) {
            i = lastId++;
            this.ids.addMapping(state, i);
        }
        return i;
    }

    public ListNBT asListNBT() {
        ListNBT list = new ListNBT();
        for (BlockState blockstate : ids) {
            list.add(NBTUtil.writeBlockState(blockstate));
        }
        return list;
    }
}
