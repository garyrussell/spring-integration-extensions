/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.x.netty.inbound;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToByteDecoder;
import io.netty.handler.codec.ByteToByteEncoder;

import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.mapping.OutboundMessageMapper;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class NettyRequestHandlingEndpoint extends MessagingGatewaySupport implements Runnable {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile boolean expectReply = true;

	private final int port;

	private volatile ServerBootstrap serverBootstrap;

	private volatile ChannelInitializer<?> channelInitializer;

	private volatile ChannelHandler channelHandler;

	public NettyRequestHandlingEndpoint(int port) {
		this.port = port;
	}

	public NettyRequestHandlingEndpoint(int port, ChannelInitializer<?> channelInitializer) {
		this.port = port;
		this.channelInitializer = channelInitializer;
	}

	@Override
	protected void onInit() throws Exception {
		if (this.channelHandler == null) {
			this.channelHandler = new ChannelInboundByteHandlerAdapter() {
				@Override
				protected void inboundBufferUpdated(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
					try {
						byte[] request = new byte[in.readableBytes()];
						in.readBytes(request);
						if (expectReply) {
							Object reply = NettyRequestHandlingEndpoint.this.sendAndReceive(
									request);
							if (reply != null) {
								ByteBuf out = ctx.nextOutboundByteBuffer();
								out.writeBytes(((byte[]) reply));
								ChannelFuture future = ctx.flush();
								future.sync();
							}
						}
						else {
							NettyRequestHandlingEndpoint.this.send(request);
						}
					}
					catch (Exception e) {
						logger.error("Failed to send or received", e);
					}
				}
			};
		}
		if (this.channelInitializer == null) {
			final ByteToByteDecoder decoder = new ByteToByteDecoder() {
				@Override
				protected void decode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
					for (int i = in.readerIndex(); i < in.readableBytes()-1 ;i++) {
						if (in.getByte(i) == '\r' && in.getByte(i+1) == '\n') {
							ByteBuf frame = in.readBytes(i - in.readerIndex());
							out.writeBytes(frame);
							break;
						}
					}
				}
			};
			final ByteToByteEncoder encoder = new ByteToByteEncoder() {
				@Override
				protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
					out.writeBytes(in, in.readerIndex(), in.readableBytes())
						.writeByte('\r')
						.writeByte('\n');
					ctx.flush();
				}
			};
			this.channelInitializer = new ChannelInitializer<Channel>() {
				@Override
				protected void initChannel(Channel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast("decoder", decoder);
					pipeline.addLast("encoder", encoder);
					pipeline.addLast("handler", channelHandler);
				}
			};
		}
		super.onInit();
	}

	@Override
	protected void doStart() {
		super.doStart();
		// protected by lifecycleMonitor
		if (this.serverBootstrap == null) {
			this.serverBootstrap = new ServerBootstrap();
			this.serverBootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup())
				.channel(NioServerSocketChannel.class)
				.localAddress(this.port)
				.childHandler(this.channelInitializer);
		}
		//TODO: real executor
		Executors.newSingleThreadExecutor().execute(this);
	}

	@Override
	protected void doStop() {
		super.doStop();
		this.serverBootstrap.shutdown();
	}

	public InboundNettyAdapterCallback registerChannelInitializer(ChannelInitializer<Channel> initializer) {
		this.channelInitializer = initializer;

		return new InboundNettyAdapterCallback() {

			@Override
			public Object sendAndReceive(Object object) {
				return NettyRequestHandlingEndpoint.this.sendAndReceive(object);
			}

			@Override
			public void send(Object object) {
				NettyRequestHandlingEndpoint.this.send(object);
			}

			@Override
			public boolean isExpectReply() {
				return NettyRequestHandlingEndpoint.this.expectReply;
			}

			@Override
			public void setRequestMapper(InboundMessageMapper<?> requestMapper) {
				NettyRequestHandlingEndpoint.this.setRequestMapper(requestMapper);
			}

			@Override
			public void setReplyMapper(OutboundMessageMapper<?> replyMapper) {
				NettyRequestHandlingEndpoint.this.setReplyMapper(replyMapper);
			}
		};
	}

	@Override
	public void run() {
		try {
			ChannelFuture future = this.serverBootstrap.bind().sync();
			future.channel().closeFuture().sync();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			this.serverBootstrap.shutdown();
		}
	}

	public interface InboundNettyAdapterCallback {

		void send(Object object);

		Object sendAndReceive(Object object);

		boolean isExpectReply();

		void setRequestMapper(InboundMessageMapper<?> requestMapper);

		void setReplyMapper(OutboundMessageMapper<?> replyMapper);
	}

}
