package com.won983212.servermod.schematic.parser;

import com.won983212.servermod.Logger;
import com.won983212.servermod.schematic.IProgressEvent;
import com.won983212.servermod.schematic.container.SchematicContainer;
import com.won983212.servermod.task.IAsyncTask;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import java.io.*;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

//TODO litemetic 파일도 parser 만들자
@SuppressWarnings("unchecked")
public abstract class AbstractSchematicReader implements IAsyncTask<SchematicContainer> {
    public static final int BATCH_COUNT = 1000;
    private IProgressEvent progressEvent;
    private Consumer<SchematicContainer> completeEvent;
    protected CompoundNBT schematic;
    protected SchematicContainer result;


    public AbstractSchematicReader(File file) {
        schematic = readNBT(file);
    }

    protected abstract BlockPos parseSize();

    protected abstract boolean parsePartial();

    @Override
    public SchematicContainer getResult() {
        return result;
    }

    @Override
    public boolean tick() {
        boolean isContinue;
        isContinue = parsePartial();
        if (!isContinue) {
            if (result != null && completeEvent != null) {
                completeEvent.accept(result);
            }
            return false;
        }
        return true;
    }

    private static CompoundNBT readNBT(File file) {
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(file))))) {
            return CompressedStreamTools.read(stream, new NBTSizeTracker(0x20000000L));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read schematic", e);
        }
    }

    public void setProgressEvent(IProgressEvent event) {
        this.progressEvent = event;
    }

    public void setCompletedEvent(Consumer<SchematicContainer> event) {
        this.completeEvent = event;
    }

    protected void notifyProgress(String status, double progress) {
        IProgressEvent.safeFire(progressEvent, status, progress);
        Logger.debug(status + ": " + progress);
    }

    protected static <T extends INBT> T checkTag(CompoundNBT nbt, String key, Class<T> expected) {
        byte typeId = getNBTTypeFromClass(expected);
        if (!nbt.contains(key, typeId)) {
            throw new IllegalArgumentException(key + " tag is not found or is not of tag type " + expected);
        }
        return (T) nbt.get(key);
    }

    protected static <T extends INBT> T getTag(CompoundNBT nbt, String key, Class<T> expected) {
        byte typeId = getNBTTypeFromClass(expected);
        if (!nbt.contains(key, typeId)) {
            return null;
        }
        return (T) nbt.get(key);
    }

    private static byte getNBTTypeFromClass(Class<?> cls) {
        if (cls == ByteNBT.class) {
            return Constants.NBT.TAG_BYTE;
        }
        if (cls == ShortNBT.class) {
            return Constants.NBT.TAG_SHORT;
        }
        if (cls == IntNBT.class) {
            return Constants.NBT.TAG_INT;
        }
        if (cls == LongNBT.class) {
            return Constants.NBT.TAG_LONG;
        }
        if (cls == FloatNBT.class) {
            return Constants.NBT.TAG_FLOAT;
        }
        if (cls == DoubleNBT.class) {
            return Constants.NBT.TAG_DOUBLE;
        }
        if (cls == ByteArrayNBT.class) {
            return Constants.NBT.TAG_BYTE_ARRAY;
        }
        if (cls == StringNBT.class) {
            return Constants.NBT.TAG_STRING;
        }
        if (cls == ListNBT.class) {
            return Constants.NBT.TAG_LIST;
        }
        if (cls == CompoundNBT.class) {
            return Constants.NBT.TAG_COMPOUND;
        }
        if (cls == IntArrayNBT.class) {
            return Constants.NBT.TAG_INT_ARRAY;
        }
        if (cls == LongArrayNBT.class) {
            return Constants.NBT.TAG_LONG_ARRAY;
        }
        if (cls == NumberNBT.class) {
            return Constants.NBT.TAG_ANY_NUMERIC;
        }
        throw new IllegalArgumentException("Invaild type: " + cls);
    }
}
