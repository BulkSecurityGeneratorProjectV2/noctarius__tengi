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
package com.noctarius.tengi.serialization.impl;

import com.noctarius.tengi.Packet;
import com.noctarius.tengi.buffer.ReadableMemoryBuffer;
import com.noctarius.tengi.buffer.WritableMemoryBuffer;
import com.noctarius.tengi.serialization.Protocol;
import com.noctarius.tengi.serialization.TypeId;

public final class SerializationClasses {

    private SerializationClasses() {
    }


    @TypeId(1000)
    public static class SubPacketWithDefaultConstructor
            extends Packet {

        public SubPacketWithDefaultConstructor() {
            super("SubPacketWithDefaultConstructor");
        }
    }

    @TypeId(1001)
    public static class SubPacketWithoutDefaultConstructor
            extends Packet {

        public SubPacketWithoutDefaultConstructor(String packageName) {
            super(packageName);
        }
    }

    @TypeId(1002)
    public static class SubPacketMarshallException
            extends Packet {

        public SubPacketMarshallException(String packageName) {
            super(packageName);
        }

        @Override
        protected void marshall0(WritableMemoryBuffer memoryBuffer, Protocol protocol) {
            throw new NullPointerException();
        }
    }

    @TypeId(1003)
    public static class SubPacketUnmarshallException
            extends Packet {

        public SubPacketUnmarshallException(String packageName) {
            super(packageName);
        }

        @Override
        protected void unmarshall0(ReadableMemoryBuffer memoryBuffer, Protocol protocol) {
            throw new NullPointerException();
        }
    }

}
