package com.won983212.servermod.client.render;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;

import java.nio.ByteBuffer;

public class BufferBuilderReader {

    private final ByteBuffer buffer;
    private final int vertexCount;
    private final int formatSize;
    private final int size;

    public BufferBuilderReader(BufferBuilder builder) {
        VertexFormat vertexFormat = builder.getVertexFormat();
        Pair<BufferBuilder.DrawState, ByteBuffer> data = builder.popNextBuffer();
        buffer = data.getSecond();

        formatSize = vertexFormat.getVertexSize();

        vertexCount = data.getFirst()
                .vertexCount();

        size = vertexCount * formatSize;
    }

    public boolean isEmpty() {
        return vertexCount == 0;
    }

    public int vertIdx(int vertexIndex) {
        return vertexIndex * formatSize;
    }

    public float getX(int index) {
        return buffer.getFloat(vertIdx(index));
    }

    public float getY(int index) {
        return buffer.getFloat(vertIdx(index) + 4);
    }

    public float getZ(int index) {
        return buffer.getFloat(vertIdx(index) + 8);
    }

    public byte getR(int index) {
        return buffer.get(vertIdx(index) + 12);
    }

    public byte getG(int index) {
        return buffer.get(vertIdx(index) + 13);
    }

    public byte getB(int index) {
        return buffer.get(vertIdx(index) + 14);
    }

    public byte getA(int index) {
        return buffer.get(vertIdx(index) + 15);
    }

    public float getU(int index) {
        return buffer.getFloat(vertIdx(index) + 16);
    }

    public float getV(int index) {
        return buffer.getFloat(vertIdx(index) + 20);
    }

    public int getLight(int index) {
        return buffer.getInt(vertIdx(index) + 24);
    }

    public byte getNX(int index) {
        return buffer.get(vertIdx(index) + 28);
    }

    public byte getNY(int index) {
        return buffer.get(vertIdx(index) + 29);
    }

    public byte getNZ(int index) {
        return buffer.get(vertIdx(index) + 30);
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getSize() {
        return size;
    }
}
