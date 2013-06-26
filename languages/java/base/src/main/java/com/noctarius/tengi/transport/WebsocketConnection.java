package com.noctarius.tengi.transport;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import com.noctarius.tengi.CompletionFuture;
import com.noctarius.tengi.Message;
import com.noctarius.tengi.Protocol;
import com.noctarius.tengi.Streamable;
import com.noctarius.tengi.TransportType;
import com.noctarius.tengi.UniqueId;
import com.noctarius.tengi.buffer.MemoryBuffer;
import com.noctarius.tengi.buffer.MemoryBufferPool;

public class WebsocketConnection
    extends AbstractChannelConnection
{

    WebsocketConnection( UniqueId connectionId, Channel channel, MemoryBufferPool memoryBufferPool, Protocol protocol )
    {
        super( connectionId, channel, memoryBufferPool, protocol );
    }

    @Override
    public TransportType getTransportType()
    {
        return TransportType.WebSocket;
    }

    @Override
    public <T extends Message> void sendMessage( T message, CompletionFuture<T> completionFuture )
    {
        MemoryBuffer memoryBuffer = memoryBufferPool.pop( 100 );
        try
        {
            prepareMessageBuffer( message, memoryBuffer );
            getUnderlyingChannel().write( memoryBuffer );

            if ( completionFuture != null )
            {
                completionFuture.onCompletion( message, this, null );
            }
        }
        catch ( Exception e )
        {
            memoryBufferPool.push( memoryBuffer );
            if ( completionFuture != null )
            {
                completionFuture.onCompletion( message, this, e );
            }
        }

        ChannelFuture channelFuture = getUnderlyingChannel().write( message );
        channelFuture.addListener( new CompletionFutureAdapter<T>( completionFuture, message, this ) );
    }

    @Override
    public <T extends Streamable> void sendRawData( MemoryBuffer rawBuffer, final T metadata,
                                                    final CompletionFuture<T> completionFuture )
    {
        MemoryBuffer memoryBuffer = memoryBufferPool.pop( rawBuffer.writerIndex() + 20 );
        try
        {
            prepareMessageBuffer( rawBuffer, metadata, memoryBuffer );
            getUnderlyingChannel().write( memoryBuffer );

            if ( completionFuture != null )
            {
                completionFuture.onCompletion( null, this, null );
            }
        }
        catch ( Exception e )
        {
            memoryBufferPool.push( rawBuffer );
            if ( completionFuture != null )
            {
                completionFuture.onCompletion( null, this, e );
            }
        }
    }

    @Override
    public void close()
    {
        // TODO Auto-generated method stub

    }

}
