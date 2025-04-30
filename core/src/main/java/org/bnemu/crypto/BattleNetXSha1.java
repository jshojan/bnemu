package org.bnemu.crypto;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BattleNetXSha1 {
    public static int[] calcHashBuffer(byte[] hashData) {
        int[] hashBuffer = new int[0x10 + 5]; // 5 state + 16 work buffer

        // SHA1 initial values
        hashBuffer[0] = 0x67452301;
        hashBuffer[1] = 0xEFCDAB89;
        hashBuffer[2] = 0x98BADCFE;
        hashBuffer[3] = 0x10325476;
        hashBuffer[4] = 0xC3D2E1F0;

        for (int i = 0; i < hashData.length; i += 0x40) {
            int subLength = Math.min(hashData.length - i, 0x40);

            // Insert input into hashBuffer[5..]
            for (int j = 0; j < subLength; j++) {
                int index = (j >> 2) + 5;
                int shift = (j & 3) * 8;
                hashBuffer[index] &= ~(0xFF << shift);
                hashBuffer[index] |= (hashData[i + j] & 0xFF) << shift;
            }

            // Pad remaining bytes with zeros
            for (int j = subLength; j < 0x40; j++) {
                int index = (j >> 2) + 5;
                int shift = (j & 3) * 8;
                hashBuffer[index] &= ~(0xFF << shift);
                // No need to OR 0 since default is 0
            }

            doHash(hashBuffer);
        }

        return hashBuffer;
    }

    private static void doHash(int[] hashBuffer) {
        int[] buf = new int[0x50];
        int a, b, c, d, e, dw;
        int p;

        // Copy the current 16 words
        System.arraycopy(hashBuffer, 5, buf, 0, 0x10);

        // Expand to 80 words using broken method
        for (int i = 0x10; i < 0x50; i++) {
            dw = buf[i - 0x10] ^ buf[i - 0x8] ^ buf[i - 0xE] ^ buf[i - 0x3];
            buf[i] = (1 >>> (0x20 - (byte)dw)) | (1 << (byte)dw); // << broken logic
        }

        a = hashBuffer[0];
        b = hashBuffer[1];
        c = hashBuffer[2];
        d = hashBuffer[3];
        e = hashBuffer[4];

        p = 0;

        // 0-19
        for (int i = 0; i < 20; i++) {
            dw = ((a << 5) | (a >>> 27)) + ((~b & d) | (c & b)) + e + buf[p++] + 0x5a827999;
            e = d;
            d = c;
            c = (b >>> 2) | (b << 30);
            b = a;
            a = dw;
        }
        // 20-39
        for (int i = 0; i < 20; i++) {
            dw = (d ^ c ^ b) + e + ((a << 5) | (a >>> 27)) + buf[p++] + 0x6ed9eba1;
            e = d;
            d = c;
            c = (b >>> 2) | (b << 30);
            b = a;
            a = dw;
        }
        // 40-59
        for (int i = 0; i < 20; i++) {
            dw = ((c & b) | (d & c) | (d & b)) + e + ((a << 5) | (a >>> 27)) + buf[p++] - 0x70e44324;
            e = d;
            d = c;
            c = (b >>> 2) | (b << 30);
            b = a;
            a = dw;
        }
        // 60-79
        for (int i = 0; i < 20; i++) {
            dw = ((a << 5) | (a >>> 27)) + e + (d ^ c ^ b) + buf[p++] - 0x359d3e2a;
            e = d;
            d = c;
            c = (b >>> 2) | (b << 30);
            b = a;
            a = dw;
        }

        hashBuffer[0] += a;
        hashBuffer[1] += b;
        hashBuffer[2] += c;
        hashBuffer[3] += d;
        hashBuffer[4] += e;
    }

    public static byte[] toBytes(int[] hashBuffer) {
        ByteBuffer out = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < 5; i++) {
            out.putInt(hashBuffer[i]);
        }
        return out.array();
    }
}