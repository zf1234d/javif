// Copyright (c) 2026 zf1234d
// SPDX-License-Identifier: MPL-2.0
package org.glavo.avif.internal.compat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/// Android API-24 backports for `java.base` methods introduced after Java 8.
///
/// `android.jar` at API level 24 (Android 7.0) does not expose `java.util.Objects.checkIndex`,
/// `java.util.Base64`, the Java 9/11 bulk stream helpers, or `java.util.List.copyOf`. These
/// static helpers reproduce their behaviour using only API-24 primitives so the decoder runs
/// unchanged on old Android releases. They are dependency-free and never touch `java.nio.file`.
public final class ApiCompat {
    /// A lookup table mapping base64 alphabet characters to their 6-bit values, or `-1`.
    private static final int[] BASE64 = new int[256];

    static {
        for (int i = 0; i < BASE64.length; i++) {
            BASE64[i] = -1;
        }
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        for (int i = 0; i < alphabet.length(); i++) {
            BASE64[alphabet.charAt(i)] = i;
        }
    }

    private ApiCompat() {
    }

    /// Backport of `java.util.Objects.checkIndex(int, int)`.
    ///
    /// @param index the index to validate
    /// @param length the exclusive upper bound
    /// @return the validated index
    /// @throws IndexOutOfBoundsException if the index is outside `[0, length)`
    public static int checkIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(
                    "index " + index + " is not in [0, " + length + ")");
        }
        return index;
    }

    /// Backport of `java.util.Objects.checkIndex(long, long)`.
    ///
    /// @param index the index to validate
    /// @param length the exclusive upper bound
    /// @return the validated index
    /// @throws IndexOutOfBoundsException if the index is outside `[0, length)`
    public static long checkIndex(long index, long length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(
                    "index " + index + " is not in [0, " + length + ")");
        }
        return index;
    }

    /// Backport of `java.util.Objects.checkFromIndexSize(int, int, int)`.
    ///
    /// @param fromIndex the first index of the range
    /// @param size the number of elements in the range
    /// @param length the exclusive upper bound
    /// @return `fromIndex`
    /// @throws IndexOutOfBoundsException if the range is outside `[0, length)`
    public static int checkFromIndexSize(int fromIndex, int size, int length) {
        if (fromIndex < 0 || size < 0 || fromIndex > length - size) {
            throw new IndexOutOfBoundsException(
                    "range [" + fromIndex + ", " + fromIndex + "+" + size + ") exceeds length " + length);
        }
        return fromIndex;
    }

    /// Backport of `java.util.List.copyOf(Collection)`.
    ///
    /// @param source the source collection
    /// @param <E> the element type
    /// @return an unmodifiable list containing the source elements
    public static <E> List<E> listCopyOf(Collection<? extends E> source) {
        return Collections.unmodifiableList(new ArrayList<E>(source));
    }

    /// Backport of `java.lang.Byte.toUnsignedInt(byte)`.
    ///
    /// @param value the byte to widen
    /// @return the unsigned value in `[0, 255]`
    public static int toUnsignedInt(byte value) {
        return value & 0xFF;
    }

    /// Backport of `java.lang.Short.toUnsignedInt(short)`.
    ///
    /// @param value the short to widen
    /// @return the unsigned value in `[0, 65535]`
    public static int toUnsignedInt(short value) {
        return value & 0xFFFF;
    }

    /// Backport of `java.lang.Integer.toUnsignedLong(int)`.
    ///
    /// @param value the int to widen
    /// @return the unsigned value in `[0, 4294967295]`
    public static long toUnsignedLong(int value) {
        return value & 0xFFFFFFFFL;
    }

    /// Backport of `java.lang.Integer.compareUnsigned(int, int)`.
    ///
    /// @param left the left value
    /// @param right the right value
    /// @return the unsigned comparison result
    public static int compareUnsigned(int left, int right) {
        return Long.compare(left & 0xFFFF_FFFFL, right & 0xFFFF_FFFFL);
    }

    /// Backport of `java.lang.Long.compareUnsigned(long, long)`.
    ///
    /// @param left the left value
    /// @param right the right value
    /// @return the unsigned comparison result
    public static int compareUnsigned(long left, long right) {
        return Long.compare(left ^ Long.MIN_VALUE, right ^ Long.MIN_VALUE);
    }

    /// Backport of `java.util.Base64.getMimeDecoder().decode(String)`.
    ///
    /// Non-alphabet characters (including line breaks) are ignored, matching the MIME decoder.
    ///
    /// @param text the base64 text
    /// @return the decoded bytes
    public static byte[] base64MimeDecode(String text) {
        byte[] out = new byte[text.length() / 4 * 3 + 3];
        int outLength = 0;
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int value = c < 256 ? BASE64[c] : -1;
            if (value < 0) {
                if (c == '=') {
                    break;
                }
                continue;
            }
            buffer = (buffer << 6) | value;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[outLength++] = (byte) (buffer >> bits);
            }
        }
        byte[] result = new byte[outLength];
        System.arraycopy(out, 0, result, 0, outLength);
        return result;
    }

    /// Backport of `java.io.InputStream.readAllBytes()`.
    ///
    /// @param input the input stream
    /// @return every remaining byte of the stream
    /// @throws IOException if the stream cannot be read
    public static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    /// Backport of `java.io.InputStream.transferTo(OutputStream)`.
    ///
    /// @param input the input stream
    /// @param output the destination stream
    /// @return the number of transferred bytes
    /// @throws IOException if either stream fails
    public static long transferTo(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        long transferred = 0L;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
            transferred += read;
        }
        return transferred;
    }

    /// Backport of `java.io.ByteArrayOutputStream.writeBytes(byte[])`.
    ///
    /// @param output the destination stream
    /// @param bytes the bytes to append
    public static void writeBytes(ByteArrayOutputStream output, byte[] bytes) {
        output.write(bytes, 0, bytes.length);
    }

    /// Backport of `java.io.ByteArrayOutputStream.toString(Charset)`.
    ///
    /// @param output the source stream
    /// @param charset the charset used to decode the accumulated bytes
    /// @return the decoded text
    public static String toString(ByteArrayOutputStream output, Charset charset) {
        return new String(output.toByteArray(), charset);
    }

    /// Backport of `java.nio.ShortBuffer.slice(int, int)` (Java 13).
    ///
    /// @param buffer the source buffer
    /// @param index the zero-based start index of the slice
    /// @param length the slice length
    /// @return a slice sharing the source content, positioned at `index`
    public static ShortBuffer slice(ShortBuffer buffer, int index, int length) {
        ShortBuffer duplicate = buffer.duplicate();
        duplicate.position(index).limit(index + length);
        return duplicate.slice();
    }
}
