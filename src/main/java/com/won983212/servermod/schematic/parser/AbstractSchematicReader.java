package com.won983212.servermod.schematic.parser;

import com.won983212.servermod.schematic.IProgressEvent;
import com.won983212.servermod.schematic.parser.container.SchematicContainer;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import java.io.*;
import java.util.zip.GZIPInputStream;

public abstract class AbstractSchematicReader {
    private IProgressEvent progressEvent;

    protected abstract SchematicContainer parse(CompoundNBT schematic) throws IOException;

    protected abstract BlockPos parseSize(CompoundNBT schematic) throws IOException;

    public SchematicContainer parse(File file) throws IOException {
        CompoundNBT schematicNBT = readNBT(file);
        notifyProgress("NBT 데이터 읽는 중...", 0);
        SchematicContainer t = parse(schematicNBT);
        notifyProgress("완료", 1);
        return t;
    }

    public BlockPos parseSize(File file) throws IOException {
        CompoundNBT schematicNBT = readNBT(file);
        notifyProgress("NBT 데이터 읽는 중...", 0);
        BlockPos pos = parseSize(schematicNBT);
        notifyProgress("완료", 1);
        return pos;
    }

    private static CompoundNBT readNBT(File file) throws IOException {
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(file))))) {
            return CompressedStreamTools.read(stream, new NBTSizeTracker(0x20000000L));
        } catch (IOException e) {
            throw new IOException("Failed to read schematic", e);
        }
    }

    public void setProgressEvent(IProgressEvent event) {
        this.progressEvent = event;
    }

    protected void notifyProgress(String status, double progress) {
        if (progressEvent != null) {
            progressEvent.onProgress(status, progress);
        }
    }

    protected static <T extends INBT> T checkTag(CompoundNBT nbt, String key, Class<T> expected) throws IOException {
        byte typeId = getNBTTypeFromClass(expected);
        if (!nbt.contains(key, typeId)) {
            throw new IOException(key + " tag is not found or is not of tag type " + expected);
        }
        return (T) nbt.get(key);
    }

    protected static <T extends INBT> T getTag(CompoundNBT nbt, String key, Class<T> expected) throws IOException {
        byte typeId = getNBTTypeFromClass(expected);
        if (!nbt.contains(key, typeId)) {
            return null;
        }
        return (T) nbt.get(key);
    }

    private static byte getNBTTypeFromClass(Class<?> cls) throws IOException {
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
        throw new IOException("Invaild type: " + cls);
    }
}
