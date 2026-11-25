package ru.bsu.graphicslab.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.IIOImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static ru.bsu.graphicslab.parser.ImageReader.Status.*;

class ImageReaderTest {
    @TempDir
    Path directory;

    @Test
    void bmpReadsTopDownRowsAndPixelsPerMetre() throws IOException {
        ImageInfo info = valid("top-down.BMP", bitmap(24, -2, 0));
        assertGeometry(info, "BMP", 3, 2, 24);
        assertEquals("BI_RGB", info.compression());
        assertEquals(299.9994, info.dpiX(), 0.0001);
        assertNull(info.dpiY());
    }

    @Test
    void bmpDistinguishesTheTwoRleCodes() throws IOException {
        ImageInfo rle8 = valid("rle8.bmp", bitmap(8, 1, 1));
        ImageInfo rle4 = valid("rle4.bmp", bitmap(4, 1, 2));
        assertGeometry(rle8, "BMP", 3, 1, 8);
        assertGeometry(rle4, "BMP", 3, 1, 4);
        assertEquals("BI_RLE8", rle8.compression());
        assertEquals("BI_RLE4", rle4.compression());
        assertTrue(rle8.details().contains("256"));
        assertTrue(rle4.details().contains("16"));
    }

    @Test
    void bmpRejectsTruncatedPixelRows() throws IOException {
        byte[] image = bitmap(24, 2, 0);
        assertEquals(CORRUPT, read("cut.bmp", Arrays.copyOf(image, image.length - 1)).status());
    }

    @Test
    void pngCountsAllRgbaChannelsAndConvertsDpi() throws IOException {
        ImageInfo info = valid("rgba.png", png(1));
        assertGeometry(info, "PNG", 2, 1, 32);
        assertEquals(299.9994, info.dpiX(), 0.0001);
        assertEquals(119.9896, info.dpiY(), 0.0001);
        assertTrue(info.compression().contains("DEFLATE"));
        assertTrue(info.details().contains("0"));
    }

    @Test
    void pngDoesNotInferDpiFromAspectRatio() throws IOException {
        ImageInfo info = valid("aspect.png", png(0));
        assertNull(info.dpiX());
        assertNull(info.dpiY());
    }

    @Test
    void pngRequiresIendAndRejectsBrokenChunkLengths() throws IOException {
        byte[] image = png(1);
        assertEquals(CORRUPT, read("no-end.png", Arrays.copyOf(image, image.length - 12)).status());
        // Remove the IEND length's legal value without touching the rest of the file.
        image[image.length - 12] = 0x7f;
        assertEquals(CORRUPT, read("length.png", image).status());
    }

    @Test
    void gifReportsIndexedDepthAndNoDpi() throws IOException {
        byte[] image = gif();
        ImageInfo info = valid("index.gif", image);
        assertGeometry(info, "GIF", 1, 1, 1);
        assertEquals("LZW", info.compression());
        assertNull(info.dpiX());
        assertNull(info.dpiY());
        assertTrue(info.details().contains("2"));
        assertEquals(CORRUPT, read("no-end.gif", Arrays.copyOf(image, image.length - 1)).status());
    }

    @Test
    void pcxUsesInclusiveCoordinatesAndMultipliesPlanes() throws IOException {
        ImageInfo info = valid("planes.pcx", pcx(3, false));
        assertGeometry(info, "PCX", 3, 2, 24);
        assertEquals(72.0, info.dpiX(), 0.001);
        assertEquals(144.0, info.dpiY(), 0.001);
        assertEquals("RLE", info.compression());
    }

    @Test
    void pcxFindsTheTrailingPalette() throws IOException {
        ImageInfo info = valid("pal8.pcx", pcx(1, true));
        assertGeometry(info, "PCX", 3, 2, 8);
        assertTrue(info.details().contains("256"));
    }

    @Test
    void tiffReadsInlineAndOffsetValuesInBothByteOrders() throws IOException {
        for (ByteOrder order : new ByteOrder[]{ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN}) {
            ImageInfo info = valid((order == ByteOrder.LITTLE_ENDIAN ? "ii" : "mm") + ".tif", tiff(order));
            assertGeometry(info, "TIFF", 2, 3, 24);
            assertEquals(299.72, info.dpiX(), 0.0001);
            assertEquals(190.5, info.dpiY(), 0.0001);
            assertTrue(info.details().startsWith(order == ByteOrder.LITTLE_ENDIAN ? "II" : "MM"));
        }
    }

    @Test
    void tiffRejectsAnOutOfRangeTagOffset() throws IOException {
        byte[] image = tiff(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer.wrap(image).order(ByteOrder.LITTLE_ENDIAN).putInt(10 + 2 * 12 + 8, 1_000_000);
        assertEquals(CORRUPT, read("bad-offset.tiff", image).status());
    }

    @Test
    void jpegFindsProgressiveSofAndConvertsJfifPixelsPerCm() throws IOException {
        byte[] image = jpeg(true);
        image[13] = 2; // JFIF density unit = pixels/cm.
        ByteBuffer.wrap(image).putShort(14, (short) 118).putShort(16, (short) 60);
        ImageInfo info = valid("progressive.JPEG", image);
        assertGeometry(info, "JPEG", 13, 7, 24);
        assertEquals(299.72, info.dpiX(), 0.0001);
        assertEquals(152.4, info.dpiY(), 0.0001);
        assertTrue(info.compression().contains("progressive DCT"));
    }

    @Test
    void jpegUsesExifWhenJfifHasNoPhysicalUnits() throws IOException {
        byte[] image = insertExifResolution(jpeg(false));
        ImageInfo info = valid("exif.jpg", image);
        assertGeometry(info, "JPEG", 13, 7, 24);
        assertEquals(144.0, info.dpiX(), 0.0001);
        assertEquals(300.0, info.dpiY(), 0.0001);

        image[13] = 1;
        ByteBuffer.wrap(image).putShort(14, (short) 96).putShort(16, (short) 120);
        ImageInfo jfif = valid("jfif-and-exif.jpg", image);
        assertEquals(96.0, jfif.dpiX(), 0.0001);
        assertEquals(120.0, jfif.dpiY(), 0.0001);
    }

    @Test
    void jpegRejectsMissingEoiAndLeavesMissingDpiEmpty() throws IOException {
        byte[] image = jpeg(false);
        ImageInfo info = valid("baseline.jpg", image);
        assertNull(info.dpiX());
        assertNull(info.dpiY());
        assertEquals(CORRUPT, read("no-eoi.jpg", Arrays.copyOf(image, image.length - 2)).status());
        valid("trailing-padding.jpg", Arrays.copyOf(image, image.length + 4));
        image[4] = 0x7f;
        assertEquals(CORRUPT, read("bad-segment-length.jpg", image).status());
    }

    @Test
    void contentSignatureWinsOverExtension() throws IOException {
        ImageReader.Result renamed = read("renamed.jpg", png(1));
        assertEquals(WRONG_EXTENSION, renamed.status());
        assertEquals("PNG", renamed.format());
        assertEquals(32, renamed.info().depth());
        assertTrue(renamed.successful());
        assertEquals(CORRUPT, read("text.jpg", "just text".getBytes(StandardCharsets.US_ASCII)).status());
        assertEquals(UNKNOWN, read("notes.txt", new byte[]{65, 66}).status());
        assertEquals(CORRUPT, read("empty.gif", new byte[0]).status());
        assertEquals(IO_ERROR, ImageReader.read(directory.resolve("missing.png")).status());
    }

    private ImageInfo valid(String name, byte[] bytes) throws IOException {
        ImageReader.Result result = read(name, bytes);
        assertEquals(OK, result.status(), result.reason());
        return result.info();
    }

    private ImageReader.Result read(String name, byte[] bytes) throws IOException {
        return ImageReader.read(Files.write(directory.resolve(name), bytes));
    }

    private static void assertGeometry(ImageInfo info, String format, long width, long height, int depth) {
        assertEquals(format, info.format());
        assertEquals(width, info.width());
        assertEquals(height, info.height());
        assertEquals(depth, info.depth());
    }

    private static byte[] bitmap(int depth, int height, int compression) {
        int paletteSize = depth <= 8 ? (1 << depth) * 4 : 0;
        int pixelOffset = 54 + paletteSize;
        // 3 pixels of palette index zero, end of line, end of bitmap.
        byte[] pixels = compression == 0 ? new byte[12 * Math.abs(height)]
                : new byte[]{3, 0, 0, 0, 0, 1};
        ByteBuffer bmp = ByteBuffer.allocate(pixelOffset + pixels.length).order(ByteOrder.LITTLE_ENDIAN);
        bmp.put((byte) 'B').put((byte) 'M').putInt(bmp.capacity()).putInt(0).putInt(pixelOffset);
        bmp.putInt(40).putInt(3).putInt(height).putShort((short) 1).putShort((short) depth);
        bmp.putInt(compression).putInt(pixels.length).putInt(11811).putInt(0).putInt(0).putInt(0);
        bmp.position(pixelOffset);
        bmp.put(pixels);
        return bmp.array();
    }

    private static byte[] png(int units) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.write(HexFormat.of().parseHex("89504e470d0a1a0a"));
            chunk(out, "IHDR", ByteBuffer.allocate(13).putInt(2).putInt(1)
                    .put((byte) 8).put((byte) 6).array());
            chunk(out, "pHYs", ByteBuffer.allocate(9).putInt(11811).putInt(4724).put((byte) units).array());
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            try (DeflaterOutputStream zlib = new DeflaterOutputStream(data)) {
                zlib.write(new byte[9]); // Filter byte and two transparent RGBA pixels.
            }
            chunk(out, "IDAT", data.toByteArray());
            chunk(out, "IEND", new byte[0]);
        }
        return bytes.toByteArray();
    }

    private static void chunk(DataOutputStream out, String type, byte[] data) throws IOException {
        byte[] name = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(name);
        crc.update(data);
        out.writeInt(data.length);
        out.write(name);
        out.write(data);
        out.writeInt((int) crc.getValue());
    }

    private static byte[] gif() {
        // GIF89a, 1 × 1, 2 palette entries, LZW codes (clear, pixel zero, end), trailer.
        return HexFormat.of().parseHex("47494638396101000100800000000000ffffff"
                + "2c00000000010001000002024401003b");
    }

    private static byte[] pcx(int planes, boolean palette) {
        ByteBuffer pcx = ByteBuffer.allocate(128 + 4 + (palette ? 769 : 0)).order(ByteOrder.LITTLE_ENDIAN);
        pcx.put((byte) 10).put((byte) 5).put((byte) 1).put((byte) 8);
        pcx.putShort((short) 5).putShort((short) 10).putShort((short) 7).putShort((short) 11);
        pcx.putShort((short) 72).putShort((short) 144);
        pcx.position(65);
        pcx.put((byte) planes).putShort((short) 4).putShort((short) 1);
        pcx.position(128);
        pcx.put((byte) (0xc0 | (4 * planes))).put((byte) 0);
        pcx.put((byte) (0xc0 | (4 * planes))).put((byte) 0);
        if (palette) {
            pcx.put((byte) 0x0c);
        }
        return pcx.array();
    }

    private static byte[] tiff(ByteOrder order) {
        int entries = 13;
        int bitsOffset = 8 + 2 + entries * 12 + 4;
        int resolutionOffset = bitsOffset + 6;
        int pixelOffset = resolutionOffset + 16;
        ByteBuffer tiff = ByteBuffer.allocate(pixelOffset + 18).order(order);
        tiffHeader(tiff, entries);
        shortTag(tiff, 256, 2); // ImageWidth
        shortTag(tiff, 257, 3); // ImageLength
        offsetTag(tiff, 258, 3, 3, bitsOffset); // BitsPerSample
        shortTag(tiff, 259, 1); // Compression
        shortTag(tiff, 262, 2); // PhotometricInterpretation RGB
        offsetTag(tiff, 273, 4, 1, pixelOffset); // StripOffsets
        shortTag(tiff, 277, 3); // SamplesPerPixel
        offsetTag(tiff, 278, 4, 1, 3); // RowsPerStrip
        offsetTag(tiff, 279, 4, 1, 18); // StripByteCounts
        offsetTag(tiff, 282, 5, 1, resolutionOffset); // XResolution
        offsetTag(tiff, 283, 5, 1, resolutionOffset + 8); // YResolution
        shortTag(tiff, 284, 1); // PlanarConfiguration
        shortTag(tiff, 296, 3); // ResolutionUnit cm
        tiff.putInt(0); // No further directory.
        tiff.putShort((short) 8).putShort((short) 8).putShort((short) 8);
        tiff.putInt(118).putInt(1).putInt(150).putInt(2);
        return tiff.array();
    }

    private static void tiffHeader(ByteBuffer tiff, int entries) {
        byte mark = (byte) (tiff.order() == ByteOrder.LITTLE_ENDIAN ? 'I' : 'M');
        tiff.put(mark).put(mark).putShort((short) 42).putInt(8).putShort((short) entries);
    }

    private static void shortTag(ByteBuffer tiff, int tag, int value) {
        tiff.putShort((short) tag).putShort((short) 3).putInt(1);
        tiff.putShort((short) value).putShort((short) 0);
    }

    private static void offsetTag(ByteBuffer tiff, int tag, int type, int count, int value) {
        tiff.putShort((short) tag).putShort((short) type).putInt(count).putInt(value);
    }

    private static byte[] jpeg(boolean progressive) throws IOException {
        // ImageIO is only a test encoder. The application reads the resulting bytes itself.
        var writer = ImageIO.getImageWritersByFormatName("JPEG").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var stream = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(stream);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setProgressiveMode(progressive ? ImageWriteParam.MODE_DEFAULT : ImageWriteParam.MODE_DISABLED);
            writer.write(null, new IIOImage(new BufferedImage(13, 7, BufferedImage.TYPE_INT_RGB), null, null), params);
        } finally {
            writer.dispose();
        }
        byte[] image = bytes.toByteArray();
        assertEquals("JFIF", new String(image, 6, 4, StandardCharsets.US_ASCII));
        return image;
    }

    private static byte[] insertExifResolution(byte[] image) throws IOException {
        ByteBuffer exif = ByteBuffer.allocate(66).order(ByteOrder.LITTLE_ENDIAN);
        tiffHeader(exif, 3);
        offsetTag(exif, 282, 5, 1, 50);
        offsetTag(exif, 283, 5, 1, 58);
        shortTag(exif, 296, 2);
        exif.putInt(0).putInt(144).putInt(1).putInt(300).putInt(1);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.write(image, 0, 20); // SOI + the 18-byte JFIF APP0.
            out.writeShort(0xffe1);
            out.writeShort(2 + 6 + exif.capacity());
            out.write(new byte[]{'E', 'x', 'i', 'f', 0, 0});
            out.write(exif.array());
            out.write(image, 20, image.length - 20);
        }
        return bytes.toByteArray();
    }
}
