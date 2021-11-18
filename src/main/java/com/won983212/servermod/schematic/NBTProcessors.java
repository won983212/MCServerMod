package com.won983212.servermod.schematic;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class NBTProcessors {

    private static final Map<TileEntityType<?>, UnaryOperator<CompoundNBT>> processors = new HashMap<>();

    public static synchronized void addProcessor(TileEntityType<?> type, UnaryOperator<CompoundNBT> processor) {
        processors.put(type, processor);
    }

    static {
        addProcessor(TileEntityType.SIGN, data -> {
            for (int i = 0; i < 4; ++i) {
                if (textComponentHasClickEvent(data.getString("Text" + (i + 1))))
                    return null;
            }
            return data;
        });
        addProcessor(TileEntityType.LECTERN, data -> {
            if (!data.contains("Book", Constants.NBT.TAG_COMPOUND))
                return data;
            CompoundNBT book = data.getCompound("Book");

            if (!book.contains("tag", Constants.NBT.TAG_COMPOUND))
                return data;
            CompoundNBT tag = book.getCompound("tag");

            if (!tag.contains("pages", Constants.NBT.TAG_LIST))
                return data;
            ListNBT pages = tag.getList("pages", Constants.NBT.TAG_STRING);

            for (INBT inbt : pages) {
                if (textComponentHasClickEvent(inbt.getAsString()))
                    return null;
            }
            return data;
        });
    }

    public static boolean textComponentHasClickEvent(String json) {
        ITextComponent component = ITextComponent.Serializer.fromJson(json.isEmpty() ? "\"\"" : json);
        return component != null && component.getStyle() != null && component.getStyle().getClickEvent() != null;
    }

    private NBTProcessors() {
    }

    @Nullable
    public static CompoundNBT process(TileEntity tileEntity, CompoundNBT compound) {
        if (compound == null)
            return null;
        TileEntityType<?> type = tileEntity.getType();
        if (compound != null && processors.containsKey(type))
            return processors.get(type).apply(compound);
        if (tileEntity instanceof MobSpawnerTileEntity)
            return compound;
        if (tileEntity.onlyOpCanSetNbt())
            return null;
        return compound;
    }

}
