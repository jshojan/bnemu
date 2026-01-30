package org.bnemu.bnftp;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * Parsed BNFTP Version 1 file request.
 *
 * <p>Wire format (after 0x02 protocol byte):
 * <pre>
 * WORD     headerSize
 * WORD     version (0x0100)
 * DWORD    platformId
 * DWORD    productId
 * DWORD    bannerId
 * DWORD    bannerExtension
 * DWORD    filePosition
 * FILETIME filetime (8 bytes)
 * STRING   filename (null-terminated)
 * </pre>
 */
public record BnftpRequest(int headerSize, int version, int platformId, int productId,
                           int bannerId, int bannerExtension, int filePosition,
                           long filetime, String filename) {

    public static BnftpRequest decode(ByteBuf buf) {
        int headerSize = buf.readUnsignedShortLE();
        int version = buf.readUnsignedShortLE();
        int platformId = buf.readIntLE();
        int productId = buf.readIntLE();
        int bannerId = buf.readIntLE();
        int bannerExtension = buf.readIntLE();
        int filePosition = buf.readIntLE();
        long filetime = buf.readLongLE();
        String filename = readNullTermString(buf);

        return new BnftpRequest(headerSize, version, platformId, productId,
                bannerId, bannerExtension, filePosition, filetime, filename);
    }

    private static String readNullTermString(ByteBuf buf) {
        var sb = new StringBuilder();
        while (buf.isReadable()) {
            byte b = buf.readByte();
            if (b == 0x00) break;
            sb.append((char) (b & 0xFF));
        }
        return sb.toString();
    }
}
