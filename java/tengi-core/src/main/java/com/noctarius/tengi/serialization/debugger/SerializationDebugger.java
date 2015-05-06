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
package com.noctarius.tengi.serialization.debugger;

import com.noctarius.tengi.buffer.MemoryBuffer;
import com.noctarius.tengi.buffer.ReadableMemoryBuffer;
import com.noctarius.tengi.buffer.WritableMemoryBuffer;
import com.noctarius.tengi.serialization.Protocol;
import com.noctarius.tengi.serialization.debugger.impl.DefaultSerializationDebugger;
import com.noctarius.tengi.serialization.debugger.impl.NoopSerializationDebugger;

public interface SerializationDebugger {

    default void push(Protocol protocol, ReadableMemoryBuffer memoryBuffer) {
        push(protocol, (MemoryBuffer) memoryBuffer, Process.DESERIALIZE, null);
    }

    default void push(Protocol protocol, WritableMemoryBuffer memoryBuffer, Object value) {
        push(protocol, (MemoryBuffer) memoryBuffer, Process.SERIALIZE, value);
    }

    void push(Protocol protocol, MemoryBuffer memoryBuffer, Process process, Object value);

    void pop();

    void fixFramesToStackTrace(Throwable throwable);

    public static SerializationDebugger create() {
        if (Debugger.ENABLED) {
            return new DefaultSerializationDebugger();
        }
        return NoopSerializationDebugger.INSTANCE;
    }

    public static enum Process {
        SERIALIZE,
        DESERIALIZE
    }

    public static final class Debugger {
        public static boolean ENABLED = false;
        public static boolean STORE_VALUES = false;
    }

}