/*
 * Copyright (c) 2015, Christoph Engelbert (aka noctarius) and
 * contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.noctarius.tengi.serialization.codec.impl;

import com.noctarius.tengi.buffer.MemoryBuffer;
import com.noctarius.tengi.buffer.ReadableMemoryBuffer;
import com.noctarius.tengi.buffer.WritableMemoryBuffer;
import com.noctarius.tengi.serialization.Protocol;
import com.noctarius.tengi.serialization.codec.Codec;
import com.noctarius.tengi.serialization.codec.impl.utf8.UTF8Codec;
import com.noctarius.tengi.serialization.debugger.SerializationDebugger;

public class DefaultCodec
        implements Codec {

    private final SerializationDebugger debugger = SerializationDebugger.create();

    private final Protocol protocol;

    private MemoryBuffer memoryBuffer;

    public DefaultCodec setMemoryBuffer(MemoryBuffer memoryBuffer) {
        this.memoryBuffer = memoryBuffer;
        return this;
    }

    public DefaultCodec(Protocol protocol) {
        this(protocol, null);
    }

    public DefaultCodec(Protocol protocol, MemoryBuffer memoryBuffer) {
        this.protocol = protocol;
        this.memoryBuffer = memoryBuffer;
    }

    @Override
    public int readBytes(byte[] bytes) {
        return memoryBuffer.readBytes(bytes);
    }

    @Override
    public int readBytes(byte[] bytes, int offset, int length) {
        return memoryBuffer.readBytes(bytes, offset, length);
    }

    @Override
    public boolean readBoolean() {
        return memoryBuffer.readBoolean();
    }

    @Override
    public boolean[] readBitSet() {
        return BitSetCompressor.readBitSet(memoryBuffer);
    }

    @Override
    public byte readByte() {
        return memoryBuffer.readByte();
    }

    @Override
    public short readUnsignedByte() {
        return memoryBuffer.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return ByteOrderUtils.getShort(memoryBuffer);
    }

    @Override
    public char readChar() {
        return (char) readShort();
    }

    @Override
    public int readInt32() {
        return ByteOrderUtils.getInt(memoryBuffer);
    }

    @Override
    public int readCompressedInt32() {
        return Int32Compressor.readInt32(memoryBuffer);
    }

    @Override
    public long readInt64() {
        return ByteOrderUtils.getLong(memoryBuffer);
    }

    @Override
    public long readCompressedInt64() {
        return Int64Compressor.readInt64(memoryBuffer);
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readInt32());
    }

    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readInt64());
    }

    @Override
    public String readString() {
        try {
            // TODO Pool buffers
            return UTF8Codec.readUTF(this, new byte[1024]);
        } catch (Exception e) {
            RuntimeException ex = new IndexOutOfBoundsException(e.getLocalizedMessage());
            ex.setStackTrace(e.getStackTrace());
            throw ex;
        }
    }

    @Override
    public <O> O readObject()
            throws Exception {

        if (SerializationDebugger.Debugger.ENABLED) {
            debugger.push(protocol, this);
        }
        O object = protocol.readObject(this);
        if (SerializationDebugger.Debugger.ENABLED) {
            debugger.pop();
        }
        return object;
    }

    @Override
    public <O> O readNullableObject()
            throws Exception {

        return protocol.readNullable(this, (d, p) -> {
            if (SerializationDebugger.Debugger.ENABLED) {
                debugger.push(protocol, d);
            }
            O object = readObject();
            if (SerializationDebugger.Debugger.ENABLED) {
                debugger.pop();
            }
            return object;
        });
    }

    @Override
    public ReadableMemoryBuffer getReadableMemoryBuffer() {
        return memoryBuffer;
    }

    @Override
    public void writeBytes(String fieldName, byte[] bytes) {
        memoryBuffer.writeBytes(bytes);
    }

    @Override
    public void writeBytes(String fieldName, byte[] bytes, int offset, int length) {
        memoryBuffer.writeBytes(bytes, offset, length);
    }

    @Override
    public void writeBoolean(String fieldName, boolean value) {
        memoryBuffer.writeBoolean(value);
    }

    @Override
    public void writeBitSet(String fieldName, boolean[] values) {
        BitSetCompressor.writeBitSet(values, memoryBuffer);
    }

    @Override
    public void writeByte(String fieldName, int value) {
        memoryBuffer.writeByte(value);
    }

    @Override
    public void writeUnsignedByte(String fieldName, short value) {
        memoryBuffer.writeUnsignedByte(value);
    }

    @Override
    public void writeShort(String fieldName, short value) {
        ByteOrderUtils.putShort(value, memoryBuffer);
    }

    @Override
    public void writeChar(String fieldName, char value) {
        writeShort(fieldName, (short) value);
    }

    @Override
    public void writeInt32(String fieldName, int value) {
        ByteOrderUtils.putInt(value, memoryBuffer);
    }

    @Override
    public void writeCompressedInt32(String fieldName, int value) {
        Int32Compressor.writeInt32(value, memoryBuffer);
    }

    @Override
    public void writeInt64(String fieldName, long value) {
        ByteOrderUtils.putLong(value, memoryBuffer);
    }

    @Override
    public void writeCompressedInt64(String fieldName, long value) {
        Int64Compressor.writeInt64(value, memoryBuffer);
    }

    @Override
    public void writeFloat(String fieldName, float value) {
        writeInt32(fieldName, Float.floatToIntBits(value));
    }

    @Override
    public void writeDouble(String fieldName, double value) {
        writeInt64(fieldName, Double.doubleToLongBits(value));
    }

    @Override
    public void writeString(String fieldName, String value) {
        try {
            // TODO Pool buffers
            UTF8Codec.writeUTF(this, value, new byte[1024]);
        } catch (Exception e) {
            RuntimeException ex = new IndexOutOfBoundsException(e.getLocalizedMessage());
            ex.setStackTrace(e.getStackTrace());
            throw ex;
        }
    }

    @Override
    public void writeObject(String fieldName, Object object)
            throws Exception {

        if (SerializationDebugger.Debugger.ENABLED) {
            debugger.push(protocol, this, object);
        }
        protocol.writeObject(fieldName, object, this);
        if (SerializationDebugger.Debugger.ENABLED) {
            debugger.pop();
        }
    }

    @Override
    public void writeNullableObject(String fieldName, Object object)
            throws Exception {

        protocol.writeNullable(fieldName, object, this, (n, o, e, p) -> {
            if (SerializationDebugger.Debugger.ENABLED) {
                debugger.push(protocol, this, object);
            }
            writeObject(fieldName, object);
            if (SerializationDebugger.Debugger.ENABLED) {
                debugger.pop();
            }
        });
    }

    @Override
    public WritableMemoryBuffer getWritableMemoryBuffer() {
        return memoryBuffer;
    }

}