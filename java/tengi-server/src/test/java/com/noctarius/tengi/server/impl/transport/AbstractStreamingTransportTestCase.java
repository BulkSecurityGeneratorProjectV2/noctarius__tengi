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
package com.noctarius.tengi.server.impl.transport;

import com.noctarius.tengi.core.config.Configuration;
import com.noctarius.tengi.core.config.ConfigurationBuilder;
import com.noctarius.tengi.core.connection.Connection;
import com.noctarius.tengi.core.connection.Transport;
import com.noctarius.tengi.core.model.Message;
import com.noctarius.tengi.server.Server;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;

public abstract class AbstractStreamingTransportTestCase {

    protected static <T, C> T practice(Runner<T, C> runner,
                                       ClientFactory<C> clientFactory, boolean ssl, Transport... serverTransports)
            throws Exception {

        Configuration configuration = new ConfigurationBuilder().addTransport(serverTransports).ssl(ssl).build();
        Server server = Server.create(configuration);
        server.start(AbstractStreamingTransportTestCase::onConnection).get();

        EventLoopGroup group = new NioEventLoopGroup();

        try {
            C client = clientFactory.createClient("localhost", 8080, ssl, group);
            T result = runner.run(client);

            return result;
        } finally {
            group.shutdownGracefully();
            server.stop().get();
        }
    }

    private static void onConnection(Connection connection) {
        connection.addMessageListener(AbstractStreamingTransportTestCase::onMessage);
    }

    private static void onMessage(Connection connection, Message message) {
        try {
            connection.writeObject(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static interface Runner<T, C> {
        T run(C client)
                throws Exception;
    }

    protected static interface ChannelReader<Ctx, T> {
        void channelRead(Ctx ctx, T object)
                throws Exception;
    }

    protected static interface ClientFactory<C> {
        C createClient(String host, int port, boolean ssl, EventLoopGroup group)
                throws Exception;
    }

}
