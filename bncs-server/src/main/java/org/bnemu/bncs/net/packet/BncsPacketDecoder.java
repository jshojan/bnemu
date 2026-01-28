package org.bnemu.bncs.net.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BncsPacketDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(BncsPacketDecoder.class);
    private boolean protocolByteRead = false;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // Step 1: Read protocol byte ONCE at the beginning of the connection
        if (!protocolByteRead) {
            if (in.readableBytes() < 1) {
                return;
            }
            byte protocolByte = in.readByte();
            logger.debug("Protocol byte received: 0x{}", String.format("%02X", protocolByte));
            protocolByteRead = true;
        }

        // Step 2: Process BNCS packets
        while (true) {
            if (in.readableBytes() < 4) {
                return; // Not enough to read header
            }

            in.markReaderIndex(); // Mark start position

            byte headerByte = in.readByte();
            if (headerByte != (byte) 0xFF) {
                logger.warn("Invalid BNCS header byte: 0x{}", String.format("%02X", headerByte));
                ctx.close();
                return;
            }

            byte packetId = in.readByte();       // 1 byte: Packet ID
            int length = in.readUnsignedShortLE(); // 2 bytes: Little-endian packet length

            if (length < 4) {
                logger.error("Invalid BNCS packet length: {}", length);
                ctx.close();
                return;
            }

            if (in.readableBytes() < length - 4) {
                // Not enough payload yet
                in.resetReaderIndex();
                return;
            }

            // Length includes header (4 bytes), so read payload of (length - 4)
            ByteBuf payload = in.readRetainedSlice(length - 4);

            logger.debug("Decoded BNCS packet: ID=0x{} Length={} PayloadSize={}",
                    String.format("%02X", packetId), length, payload.readableBytes());

            out.add(new BncsPacket(BncsPacketId.fromCode(packetId), packetId, new BncsPacketBuffer(payload)));
        }
    }
}
