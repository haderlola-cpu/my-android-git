package com.example.util

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Self-contained ISO/IEC 18004 QR Code Generator & Decoder (Version 2-L, 25x25 modules, Byte Mode)
 * for payloads up to 32 bytes (e.g., "eb:1:48291037").
 * Produces real, standards-compliant QR codes readable by external QR scanners (OpenCV, phone cameras)
 * and decodes them directly from Bitmaps or camera YUV frames with zero external dependency risk.
 */
object QrCodec {

    private const val SIZE = 25 // Version 2 QR Code is 25x25 modules
    private const val DATA_CODEWORDS = 34
    private const val EC_CODEWORDS = 10

    /**
     * Encodes [content] (up to 32 UTF-8 bytes) into a 25x25 boolean matrix of dark modules.
     */
    fun encodeToMatrix(content: String): Array<BooleanArray> {
        val bytes = content.toByteArray(Charsets.UTF_8).take(32).toByteArray()
        val dataCw = buildDataCodewords(bytes)
        val ecCw = computeReedSolomon(dataCw, EC_CODEWORDS)
        val allCodewords = IntArray(DATA_CODEWORDS + EC_CODEWORDS)
        for (i in dataCw.indices) allCodewords[i] = dataCw[i]
        for (i in ecCw.indices) allCodewords[DATA_CODEWORDS + i] = ecCw[i]

        val matrix = Array(SIZE) { BooleanArray(SIZE) }
        val reserved = Array(SIZE) { BooleanArray(SIZE) }

        // 1. Place Finder Patterns + Separators at (0,0), (18,0), (0,18)
        placeFinderPattern(matrix, reserved, 0, 0)
        placeFinderPattern(matrix, reserved, SIZE - 7, 0)
        placeFinderPattern(matrix, reserved, 0, SIZE - 7)

        // 2. Place Alignment Pattern for Version 2 at center (18, 18) -> top-left (16, 16)
        placeAlignmentPattern(matrix, reserved, 16, 16)

        // 3. Place Timing Patterns on row 6 and col 6
        for (i in 8 until SIZE - 8) {
            if (!reserved[6][i]) {
                matrix[6][i] = (i % 2 == 0)
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = (i % 2 == 0)
                reserved[i][6] = true
            }
        }

        // 4. Dark module at (row = 4*V + 9 = 17, col = 8)
        matrix[17][8] = true
        reserved[17][8] = true

        // Reserve Format Information areas
        for (i in 0..8) {
            if (i != 6) {
                reserved[8][i] = true
                reserved[i][8] = true
            }
        }
        for (i in 0..7) {
            reserved[8][SIZE - 1 - i] = true
            reserved[SIZE - 1 - i][8] = true
        }

        // 5. Place Data + ECC bits in standard upward/downward 2-column zigzag
        var bitIndex = 0
        val totalBits = allCodewords.size * 8
        var upward = true
        var col = SIZE - 1
        while (col > 0) {
            if (col == 6) col-- // Skip vertical timing pattern column
            for (step in 0 until SIZE) {
                val row = if (upward) (SIZE - 1 - step) else step
                for (cOffset in 0..1) {
                    val c = col - cOffset
                    if (!reserved[row][c]) {
                        val bit = if (bitIndex < totalBits) {
                            val cw = allCodewords[bitIndex ushr 3]
                            ((cw ushr (7 - (bitIndex and 7))) and 1) == 1
                        } else {
                            false
                        }
                        // Apply Mask Pattern 0: (row + c) % 2 == 0
                        val mask = ((row + c) % 2 == 0)
                        matrix[row][c] = bit xor mask
                        bitIndex++
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // 6. Place Format Information for Error Correction Level L (01) + Mask 0 (000) -> 0x77C4
        val formatBits = 0x77C4
        val formatCoords1 = arrayOf(
            8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5,
            8 to 7, 8 to 8, 7 to 8, 5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8
        )
        for (i in 0 until 15) {
            val bit = ((formatBits ushr i) and 1) == 1
            val (r, c) = formatCoords1[i]
            matrix[r][c] = bit
        }
        for (i in 0 until 7) {
            val bit = ((formatBits ushr i) and 1) == 1
            matrix[SIZE - 1 - i][8] = bit
        }
        for (i in 7 until 15) {
            val bit = ((formatBits ushr i) and 1) == 1
            matrix[8][SIZE - 15 + i] = bit
        }

        return matrix
    }

    /**
     * Renders a QR Code Bitmap with a 2-module quiet zone around the 25x25 matrix.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int,
        fgColorInt: Int,
        bgColorInt: Int = Color.WHITE
    ): Bitmap {
        val safeSize = sizePx.coerceIn(116, 1600)
        val matrix = encodeToMatrix(content)
        val quietZone = 2
        val totalModules = SIZE + quietZone * 2 // 29 modules

        val pixels = IntArray(safeSize * safeSize) { bgColorInt }
        for (y in 0 until safeSize) {
            val moduleY = (y * totalModules) / safeSize - quietZone
            val rowOffset = y * safeSize
            for (x in 0 until safeSize) {
                val moduleX = (x * totalModules) / safeSize - quietZone
                if (moduleY in 0 until SIZE && moduleX in 0 until SIZE) {
                    if (matrix[moduleY][moduleX]) {
                        pixels[rowOffset + x] = fgColorInt
                    }
                }
            }
        }
        return Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, safeSize, 0, 0, safeSize, safeSize)
        }
    }

    /**
     * Decodes a Version 2-L QR code generated by [generateQrBitmap] from a Bitmap.
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val w = bitmap.width
            val h = bitmap.height
            if (w < 29 || h < 29) return null

            val quietZone = 2
            val totalModules = SIZE + quietZone * 2
            val sampled = Array(SIZE) { BooleanArray(SIZE) }

            // Measure average luminance of corners (background) vs center of top-left finder (foreground)
            val bgPixel = bitmap.getPixel(2, 2)
            val bgLum = luminance(bgPixel)

            val finderCenterX = (((quietZone + 3.5f) / totalModules) * w).toInt().coerceIn(0, w - 1)
            val finderCenterY = (((quietZone + 3.5f) / totalModules) * h).toInt().coerceIn(0, h - 1)
            val fgLum = luminance(bitmap.getPixel(finderCenterX, finderCenterY))
            val threshold = (bgLum + fgLum) / 2
            val darkIsLower = fgLum <= bgLum

            for (r in 0 until SIZE) {
                val py = (((r + quietZone + 0.5f) / totalModules) * h).toInt().coerceIn(0, h - 1)
                for (c in 0 until SIZE) {
                    val px = (((c + quietZone + 0.5f) / totalModules) * w).toInt().coerceIn(0, w - 1)
                    val lum = luminance(bitmap.getPixel(px, py))
                    sampled[r][c] = if (darkIsLower) (lum <= threshold) else (lum >= threshold)
                }
            }

            decodeMatrix(sampled)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decodes a Version 2-L QR code from a grayscale luminance buffer (center square crop of camera frame).
     */
    fun decodeFromLuminance(luma: ByteArray, width: Int, height: Int): String? {
        return try {
            val cropSize = minOf(width, height) * 6 / 10
            val startX = (width - cropSize) / 2
            val startY = (height - cropSize) / 2
            val quietZone = 2
            val totalModules = SIZE + quietZone * 2

            var minL = 255
            var maxL = 0
            for (y in startY until startY + cropSize step 4) {
                val rowOff = y * width
                for (x in startX until startX + cropSize step 4) {
                    val v = luma[rowOff + x].toInt() and 0xFF
                    if (v < minL) minL = v
                    if (v > maxL) maxL = v
                }
            }
            if (maxL - minL < 35) return null
            val threshold = (minL + maxL) / 2

            val sampled = Array(SIZE) { BooleanArray(SIZE) }
            for (r in 0 until SIZE) {
                val py = startY + (((r + quietZone + 0.5f) / totalModules) * cropSize).toInt()
                for (c in 0 until SIZE) {
                    val px = startX + (((c + quietZone + 0.5f) / totalModules) * cropSize).toInt()
                    val v = luma[py * width + px].toInt() and 0xFF
                    sampled[r][c] = v < threshold
                }
            }
            decodeMatrix(sampled)
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeMatrix(matrix: Array<BooleanArray>): String? {
        // Verify top-left finder center is dark
        if (!matrix[3][3] || !matrix[3][SIZE - 4] || !matrix[SIZE - 4][3]) return null

        val reserved = Array(SIZE) { BooleanArray(SIZE) }
        val dummy = Array(SIZE) { BooleanArray(SIZE) }
        placeFinderPattern(dummy, reserved, 0, 0)
        placeFinderPattern(dummy, reserved, SIZE - 7, 0)
        placeFinderPattern(dummy, reserved, 0, SIZE - 7)
        placeAlignmentPattern(dummy, reserved, 16, 16)
        for (i in 8 until SIZE - 8) {
            reserved[6][i] = true
            reserved[i][6] = true
        }
        reserved[17][8] = true
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
        }
        for (i in 0..7) {
            reserved[8][SIZE - 1 - i] = true
            reserved[SIZE - 1 - i][8] = true
        }

        val bits = BooleanArray((DATA_CODEWORDS + EC_CODEWORDS) * 8)
        var bitIndex = 0
        var upward = true
        var col = SIZE - 1
        while (col > 0) {
            if (col == 6) col--
            for (step in 0 until SIZE) {
                val row = if (upward) (SIZE - 1 - step) else step
                for (cOffset in 0..1) {
                    val c = col - cOffset
                    if (!reserved[row][c] && bitIndex < bits.size) {
                        val mask = ((row + c) % 2 == 0)
                        bits[bitIndex++] = matrix[row][c] xor mask
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        fun readBits(startBit: Int, count: Int): Int {
            var v = 0
            for (i in 0 until count) {
                v = (v shl 1) or if (bits[startBit + i]) 1 else 0
            }
            return v
        }

        val mode = readBits(0, 4)
        if (mode != 0b0100) return null // Byte mode
        val length = readBits(4, 8)
        if (length <= 0 || length > 32) return null

        val outBytes = ByteArray(length)
        for (i in 0 until length) {
            outBytes[i] = readBits(12 + i * 8, 8).toByte()
        }
        return String(outBytes, Charsets.UTF_8)
    }

    private fun luminance(color: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    private fun placeFinderPattern(
        matrix: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        top: Int,
        left: Int
    ) {
        for (r in -1..7) {
            for (c in -1..7) {
                val rr = top + r
                val cc = left + c
                if (rr in 0 until SIZE && cc in 0 until SIZE) {
                    val isDark = if (r in 0..6 && c in 0..6) {
                        r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4)
                    } else {
                        false
                    }
                    matrix[rr][cc] = isDark
                    reserved[rr][cc] = true
                }
            }
        }
    }

    private fun placeAlignmentPattern(
        matrix: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        top: Int,
        left: Int
    ) {
        for (r in 0..4) {
            for (c in 0..4) {
                val rr = top + r
                val cc = left + c
                val isDark = r == 0 || r == 4 || c == 0 || c == 4 || (r == 2 && c == 2)
                matrix[rr][cc] = isDark
                reserved[rr][cc] = true
            }
        }
    }

    private fun buildDataCodewords(bytes: ByteArray): IntArray {
        val bits = BooleanArray(DATA_CODEWORDS * 8)
        var pos = 0
        fun pushBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                if (pos < bits.size) {
                    bits[pos++] = ((value ushr i) and 1) == 1
                }
            }
        }

        // Mode indicator for Byte Mode: 0100
        pushBits(0b0100, 4)
        // Character count indicator (8 bits for Version 2 Byte mode)
        pushBits(bytes.size, 8)
        // Payload bytes
        for (b in bytes) {
            pushBits(b.toInt() and 0xFF, 8)
        }
        // Terminator up to 4 zero bits
        pushBits(0, 4)
        // Pad to byte boundary
        while (pos % 8 != 0 && pos < bits.size) {
            pos++
        }
        // Pad codewords alternating 0xEC and 0x11
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (pos < bits.size) {
            pushBits(padBytes[padIdx % 2], 8)
            padIdx++
        }

        val codewords = IntArray(DATA_CODEWORDS)
        for (i in 0 until DATA_CODEWORDS) {
            var cw = 0
            for (b in 0..7) {
                cw = (cw shl 1) or if (bits[i * 8 + b]) 1 else 0
            }
            codewords[i] = cw
        }
        return codewords
    }

    // GF(256) Reed-Solomon Error Correction with primitive polynomial 0x11D
    private val gfExp = IntArray(512)
    private val gfLog = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            gfExp[i] = x
            gfLog[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) {
                x = x xor 0x11D
            }
        }
        for (i in 255 until 512) {
            gfExp[i] = gfExp[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return gfExp[gfLog[a] + gfLog[b]]
    }

    private fun computeReedSolomon(data: IntArray, ecCount: Int): IntArray {
        // Build generator polynomial for ecCount roots
        var gen = intArrayOf(1)
        for (i in 0 until ecCount) {
            val next = IntArray(gen.size + 1)
            val root = gfExp[i]
            for (j in gen.indices) {
                next[j] = next[j] xor gen[j]
                next[j + 1] = next[j + 1] xor gfMul(gen[j], root)
            }
            gen = next
        }

        val remainder = IntArray(ecCount)
        for (byte in data) {
            val factor = byte xor remainder[0]
            for (i in 0 until ecCount - 1) {
                remainder[i] = remainder[i + 1] xor gfMul(gen[i + 1], factor)
            }
            remainder[ecCount - 1] = gfMul(gen[ecCount], factor)
        }
        return remainder
    }
}
