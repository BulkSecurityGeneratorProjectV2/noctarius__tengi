/*
 * Copyright (c) 2015-2016, Christoph Engelbert (aka noctarius) and
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
package com.noctarius.tengi.server.impl.transport.http;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class HttpsTransportTestCase
        extends AbstractHttpTransportTestCase {

    @Override
    protected AsyncHttpClient newHttpClient() {
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build();
        return new AsyncHttpClient(config);
    }

    @Override
    protected boolean ssl() {
        return true;
    }
}
