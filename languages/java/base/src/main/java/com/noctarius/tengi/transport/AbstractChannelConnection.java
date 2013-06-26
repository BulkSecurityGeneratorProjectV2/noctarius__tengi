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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import com.noctarius.tengi.CompletionFuture;
import com.noctarius.tengi.Connection;
import com.noctarius.tengi.ConnectionConstants;
import com.noctarius.tengi.Message;
import com.noctarius.tengi.MessageFrameListener;
import com.noctarius.tengi.MessageListener;
import com.noctarius.tengi.Protocol;
import com.noctarius.tengi.RawFrameListener;
import com.noctarius.tengi.Streamable;
import com.noctarius.tengi.UniqueId;
import com.noctarius.tengi.buffer.ByteBufMemoryBuffer;
import com.noctarius.tengi.buffer.MemoryBuffer;
import com.noctarius.tengi.buffer.MemoryBufferPool;
import com.noctarius.tengi.utils.ChannelMessageHandler;

public abstract class AbstractChannelConnection
    implements Connection
{

    private final ChannelMessageHandler<ByteBuf> messageDecoder = new TengiMemoryBufferDecoder();

    protected final Protocol protocol;

    protected final MemoryBufferPool memoryBufferPool;

    private final UniqueId connectionId;

    private final Channel channel;

    private volatile MessageListener messageListener = null;

    protected AbstractChannelConnection( UniqueId connectionId, Channel channel, MemoryBufferPool memoryBufferPool,
                                         Protocol protocol )
    {
        this.protocol = protocol;
        this.memoryBufferPool = memoryBufferPool;
        this.connectionId = connectionId;
        this.channel = channel;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends Message> void sendObject( Streamable body, CompletionFuture<T> completionFuture )
    {
        Message message = prepareMessage( body );
        sendMessage( (T) message, completionFuture );
    }

    @Override
    public <T extends Message> void sendMessage( T message )
    {
        sendMessage( message, null );
    }

    @Override
    public <T extends Message> void sendObject( Streamable body )
    {
        sendObject( body, null );
    }

    @Override
    public <T extends Streamable> void sendRawData( MemoryBuffer rawBuffer, T metadata )
    {
        sendRawData( rawBuffer, metadata, null );
    }

    public Channel getUnderlyingChannel()
    {
        return channel;
    }

    public ChannelMessageHandler<ByteBuf> getMessageDecoder()
    {
        return messageDecoder;
    }

    @Override
    public UniqueId getConnectionId()
    {
        return connectionId;
    }

    @Override
    public void setMessageListener( MessageListener messageListener )
    {
        this.messageListener = messageListener;
    }

    @Override
    public void setMessageListener( MessageFrameListener messageFrameListener )
    {
        this.messageListener = new LambdaMessageListenerAdapter( messageFrameListener, null );
    }

    @Override
    public void setMessageListener( RawFrameListener rawFrameListener )
    {
        this.messageListener = new LambdaMessageListenerAdapter( null, rawFrameListener );
    }

    @Override
    public void setMessageListener( MessageFrameListener messageFrameListener, RawFrameListener rawFrameListener )
    {
        this.messageListener = new LambdaMessageListenerAdapter( messageFrameListener, rawFrameListener );
    }

    @Override
    public void clearMessageListener()
    {
        messageListener = null;
    }

    @Override
    public Message prepareMessage( Streamable body )
    {
        return new Message( this, body, UniqueId.randomUniqueId(), Message.MESSAGE_TYPE_DEFAULT );
    }

    protected ChannelFuture writeMemoryBuffer( MemoryBuffer memoryBuffer )
    {
        ByteBuf byteBuf = null;
        if ( memoryBuffer instanceof ByteBufMemoryBuffer )
        {
            ( (ByteBufMemoryBuffer) memoryBuffer ).getByteBuffer();
        }
        else
        {
            MemoryBuffer temp = memoryBufferPool.pop( memoryBuffer.readableBytes() );
            try
            {
                temp.writeBuffer( memoryBuffer );
                byteBuf = ( (ByteBufMemoryBuffer) temp ).getByteBuffer();
            }
            finally
            {
                memoryBufferPool.push( temp );
            }
        }

        if ( byteBuf != null )
        {
            return channel.write( byteBuf );
        }
        return null;
    }

    protected void prepareMessageBuffer( Message message, MemoryBuffer memoryBuffer )
    {
        memoryBuffer.writeByte( ConnectionConstants.DATA_TYPE_MESSAGE );
        Message.write( memoryBuffer, protocol, message );
    }

    protected void prepareMessageBuffer( MemoryBuffer rawBuffer, Streamable metadata, MemoryBuffer memoryBuffer )
    {
        memoryBuffer.writeByte( ConnectionConstants.DATA_TYPE_RAW );
        writeNullableObject( metadata, memoryBuffer );
        memoryBuffer.writeInt( rawBuffer.writerIndex() );
        memoryBuffer.writeBuffer( rawBuffer, 0, rawBuffer.writerIndex() );
    }

    protected <S extends Streamable> void writeNullableObject( S streamable, MemoryBuffer memoryBuffer )
    {
        if ( streamable == null )
        {
            memoryBuffer.writeByte( (byte) 0 );
        }
        else
        {
            memoryBuffer.writeByte( (byte) 1 );
            memoryBuffer.writeShort( protocol.getClassIdentifier( streamable ) );
            streamable.writeStream( memoryBuffer, protocol );
        }
    }

    @SuppressWarnings( "unchecked" )
    protected <S extends Streamable> S readNullableObject( MemoryBuffer memoryBuffer )
    {
        if ( memoryBuffer.readByte() == 1 )
        {
            S streamable = (S) protocol.instantiate( memoryBuffer.readShort() );
            streamable.readStream( memoryBuffer, protocol );
            return streamable;
        }
        return null;
    }

    private class TengiMemoryBufferDecoder
        extends ChannelMessageHandler<ByteBuf>
    {

        private final Connection connection = AbstractChannelConnection.this;

        @Override
        public void messageReceived( ChannelHandlerContext ctx, ByteBuf msg )
            throws Exception
        {
            MemoryBuffer memoryBuffer = memoryBufferPool.wrap( msg );
            try
            {
                byte frameType = memoryBuffer.readByte();
                switch ( frameType )
                {
                    case ConnectionConstants.DATA_TYPE_MESSAGE:
                        decodeMessageFrame( memoryBuffer );
                        break;

                    case ConnectionConstants.DATA_TYPE_RAW:
                        decodeRawDataFrame( memoryBuffer );
                        break;

                    default:
                        throw new IllegalStateException( "Illegal frame type: " + frameType );
                }
            }
            finally
            {
                memoryBufferPool.push( memoryBuffer );
            }
        }

        private void decodeMessageFrame( MemoryBuffer memoryBuffer )
        {
            Message message = Message.read( memoryBuffer, protocol, connection );
            if ( messageListener != null )
            {
                messageListener.messageReceived( message, connection );
            }
        }

        private void decodeRawDataFrame( MemoryBuffer memoryBuffer )
        {
            Streamable metadata = readNullableObject( memoryBuffer );
            int length = memoryBuffer.readInt();
            MemoryBuffer rawBuffer = memoryBufferPool.pop( length );
            rawBuffer.writeBuffer( memoryBuffer, 0, length );
            if ( messageListener != null )
            {
                messageListener.rawDataReceived( rawBuffer, metadata, connection );
            }
        }
    }

    private class LambdaMessageListenerAdapter
        implements MessageListener
    {

        private final MessageFrameListener messageFrameListener;

        private final RawFrameListener rawFrameListener;

        private LambdaMessageListenerAdapter( MessageFrameListener messageFrameListener,
                                              RawFrameListener rawFrameListener )
        {
            this.messageFrameListener = messageFrameListener;
            this.rawFrameListener = rawFrameListener;
        }

        @Override
        public void messageReceived( Message message, Connection connection )
        {
            if ( messageFrameListener != null )
            {
                messageFrameListener.messageReceived( message, connection );
            }
        }

        @Override
        public void rawDataReceived( MemoryBuffer rawBuffer, Streamable metadata, Connection connection )
        {
            if ( rawFrameListener != null )
            {
                rawFrameListener.rawDataReceived( rawBuffer, metadata, connection );
            }
        }
    }

}
