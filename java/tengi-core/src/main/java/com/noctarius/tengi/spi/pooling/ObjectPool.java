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
package com.noctarius.tengi.spi.pooling;

import java.util.function.Consumer;

public interface ObjectPool<T> {

    default PooledObject<T> acquire() {
        return acquire(null);
    }

    PooledObject<T> acquire(Consumer<T> activator);

    default void release(PooledObject<T> object) {
        release(object, null);
    }

    void release(PooledObject<T> object, Consumer<T> passivator);

    void close();

}
