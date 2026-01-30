package org.bnemu.bnftp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Handles a BNFTP (Battle.net File Transfer Protocol) Version 1 connection.
 *
 * <p>The client connects to port 6112 with protocol byte 0x02, sends a file request,
 * and the server responds with the file header followed by the raw file data,
 * then closes the connection.
 */
public class BnftpHandler extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(BnftpHandler.class);
    private static final int BNFTP_V1 = 0x0100;
    private static final int MIN_HEADER_SIZE = 32; // Minimum request without filename

    private final BnftpFileProvider fileProvider;

    public BnftpHandler(BnftpFileProvider fileProvider) {
        this.fileProvider = fileProvider;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // Need at least 2 bytes to read header size
        if (in.readableBytes() < 2) {
            return;
        }

        in.markReaderIndex();

        int headerSize = in.getUnsignedShortLE(in.readerIndex());
        if (headerSize < MIN_HEADER_SIZE) {
            logger.warn("BNFTP: Invalid header size: {}", headerSize);
            ctx.close();
            return;
        }

        // Wait for full request
        if (in.readableBytes() < headerSize) {
            in.resetReaderIndex();
            return;
        }

        BnftpRequest request = BnftpRequest.decode(in);
        logger.info("BNFTP: File request for '{}' (version=0x{}, platform=0x{}, product=0x{}, pos={})",
                request.filename(),
                String.format("%04X", request.version()),
                String.format("%08X", request.platformId()),
                String.format("%08X", request.productId()),
                request.filePosition());

        if (request.version() != BNFTP_V1) {
            logger.warn("BNFTP: Unsupported version 0x{}, only v1 (0x0100) supported",
                    String.format("%04X", request.version()));
            ctx.close();
            return;
        }

        byte[] fileData = fileProvider.getFile(request.filename());
        if (fileData == null) {
            logger.warn("BNFTP: File not available, closing connection");
            ctx.close();
            return;
        }

        int totalFileSize = fileData.length;

        // Handle file position (resume support) — send remaining bytes from offset,
        // but always report the total file size in the header (per PvPGN behavior)
        int filePosition = request.filePosition();
        if (filePosition > 0 && filePosition < fileData.length) {
            byte[] remaining = new byte[fileData.length - filePosition];
            System.arraycopy(fileData, filePosition, remaining, 0, remaining.length);
            fileData = remaining;
        }

        long filetime = fileProvider.getFiletime(request.filename());

        sendResponse(ctx, request, fileData, totalFileSize, filetime);
    }

    private void sendResponse(ChannelHandlerContext ctx, BnftpRequest request,
                              byte[] fileData, int totalFileSize, long filetime) {
        byte[] filenameBytes = request.filename().getBytes(StandardCharsets.ISO_8859_1);

        // Header size: WORD + WORD + DWORD + DWORD + DWORD + FILETIME + STRING
        //            = 2 + 2 + 4 + 4 + 4 + 8 + (strlen + 1)
        int responseHeaderSize = 2 + 2 + 4 + 4 + 4 + 8 + filenameBytes.length + 1;

        ByteBuf response = Unpooled.buffer(responseHeaderSize + fileData.length);

        // Response header
        response.writeShortLE(responseHeaderSize);  // WORD: header size
        response.writeShortLE(0x0000);              // WORD: type (SERVER_FILE_REPLY)
        response.writeIntLE(totalFileSize);         // DWORD: total file size (always full, per PvPGN)
        response.writeIntLE(request.bannerId());    // DWORD: banner ID (echo)
        response.writeIntLE(request.bannerExtension()); // DWORD: banner extension (echo)
        response.writeLongLE(filetime);             // FILETIME: file timestamp
        response.writeBytes(filenameBytes);         // STRING: filename
        response.writeByte(0x00);                   // null terminator

        // File data (may be offset from start if filePosition > 0)
        response.writeBytes(fileData);

        logger.info("BNFTP: Sending '{}' ({} byte header + {} bytes file data, total size {})",
                request.filename(), responseHeaderSize, fileData.length, totalFileSize);

        ctx.writeAndFlush(response).addListener(future -> {
            if (future.isSuccess()) {
                logger.debug("BNFTP: Transfer complete for '{}'", request.filename());
            } else {
                logger.warn("BNFTP: Transfer failed for '{}': {}",
                        request.filename(), future.cause().getMessage());
            }
            ctx.close();
        });
    }
}
