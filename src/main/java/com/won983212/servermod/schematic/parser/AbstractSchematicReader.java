/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.won983212.servermod.schematic.parser;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockReader;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Base class for NBT schematic readers.
 */
public abstract class AbstractSchematicReader {

    protected abstract Template parse(CompoundNBT schematic) throws IOException;

    public Template parse(File file) throws IOException {
        CompoundNBT schematicNBT = readNBT(file);
        return parse(schematicNBT);
    }

    private static CompoundNBT readNBT(File file) throws IOException {
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(file))))) {
            return CompressedStreamTools.read(stream, new NBTSizeTracker(0x20000000L));
        } catch (IOException e) {
            throw new IOException("Failed to read schematic", e);
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

    static class PriorityBlockList {
        private final List<Template.BlockInfo> plainBlocks = Lists.newArrayList();
        private final List<Template.BlockInfo> tileBlocks = Lists.newArrayList();
        private final List<Template.BlockInfo> specialBlocks = Lists.newArrayList();

        public void addBlock(BlockPos pt, BlockState state, @Nullable CompoundNBT blockNBT) {
            Template.BlockInfo tempBlock = new Template.BlockInfo(pt, state, blockNBT);
            if (tempBlock.nbt != null) {
                tileBlocks.add(tempBlock);
            } else if (!tempBlock.state.getBlock().hasDynamicShape() && tempBlock.state.isCollisionShapeFullBlock(EmptyBlockReader.INSTANCE, BlockPos.ZERO)) {
                plainBlocks.add(tempBlock);
            } else {
                specialBlocks.add(tempBlock);
            }
        }

        public void addNewPaletteTo(Template template) throws IOException {
            List<Template.BlockInfo> list = Lists.newArrayList();
            list.addAll(plainBlocks);
            list.addAll(specialBlocks);
            list.addAll(tileBlocks);

            Template.Palette palette;
            try {
                Constructor<Template.Palette> con = Template.Palette.class.getDeclaredConstructor(List.class);
                con.setAccessible(true);
                palette = con.newInstance(list);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new IOException("Can't create palette", e);
            }
            template.palettes.add(palette);
        }
    }
}
