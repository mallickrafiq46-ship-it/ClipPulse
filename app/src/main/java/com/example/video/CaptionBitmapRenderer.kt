package com.example.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.CaptionConfig
import com.example.model.CaptionLanguage
import com.example.model.CaptionStylePreset
import com.example.model.WordCaption

data class CaptionChunk(
    val words: List<WordCaption>,
    val startMs: Long,
    val endMs: Long
)

data class ActiveCaptionFrame(
    val chunkIndex: Int,
    val activeWordInChunk: Int,
    val chunk: CaptionChunk
)

object CaptionTimelineHelper {

    /**
     * Groups word-by-word captions into punchy 2-3 word kinetic phrases
     * (OpusClip / Hormozi viral caption style).
     */
    fun buildChunks(words: List<WordCaption>, maxWordsPerChunk: Int = 3): List<CaptionChunk> {
        if (words.isEmpty()) return emptyList()
        val chunks = mutableListOf<CaptionChunk>()
        val buffer = mutableListOf<WordCaption>()

        for (word in words) {
            buffer.add(word)
            val endsSentence = word.word.endsWith(".") ||
                word.word.endsWith("!") ||
                word.word.endsWith("?") ||
                word.word.endsWith("।") ||
                word.word.endsWith(",")
            val totalChars = buffer.sumOf { it.word.length }
            if (buffer.size >= maxWordsPerChunk || endsSentence || totalChars >= 22) {
                chunks.add(
                    CaptionChunk(
                        words = buffer.toList(),
                        startMs = buffer.first().startMs,
                        endMs = buffer.last().endMs
                    )
                )
                buffer.clear()
            }
        }
        if (buffer.isNotEmpty()) {
            chunks.add(
                CaptionChunk(
                    words = buffer.toList(),
                    startMs = buffer.first().startMs,
                    endMs = buffer.last().endMs
                )
            )
        }
        return chunks
    }

    fun findActiveFrame(chunks: List<CaptionChunk>, positionMs: Long): ActiveCaptionFrame? {
        chunks.forEachIndexed { chunkIdx, chunk ->
            if (positionMs in chunk.startMs..chunk.endMs) {
                val activeWordIdx = chunk.words.indexOfLast { positionMs >= it.startMs }
                    .coerceAtLeast(0)
                return ActiveCaptionFrame(
                    chunkIndex = chunkIdx,
                    activeWordInChunk = activeWordIdx,
                    chunk = chunk
                )
            }
        }
        return null
    }
}

/**
 * Draws bold, stylish kinetic captions onto a transparent ARGB_8888 Bitmap for
 * Media3 Transformer's BitmapOverlay during 9:16 vertical MP4 export.
 */
class CaptionBitmapRenderer(
    context: Context,
    private val frameWidth: Int,
    private val frameHeight: Int
) {
    private val antonTypeface: Typeface =
        ResourcesCompat.getFont(context, R.font.anton) ?: Typeface.DEFAULT_BOLD
    private val boldSansTypeface: Typeface =
        ResourcesCompat.getFont(context, R.font.space_grotesk) ?: Typeface.DEFAULT_BOLD
    private val devanagariBoldTypeface: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun renderFrame(
        activeFrame: ActiveCaptionFrame?,
        config: CaptionConfig
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)
        if (!config.enabled || activeFrame == null) return bitmap

        val canvas = Canvas(bitmap)
        val useDevanagari = config.language == CaptionLanguage.HINDI
        val baseTypeface = when {
            useDevanagari -> devanagariBoldTypeface
            config.style == CaptionStylePreset.HORMOZI_POP -> antonTypeface
            else -> boldSansTypeface
        }

        val textSizePx = when (config.style) {
            CaptionStylePreset.HORMOZI_POP -> if (useDevanagari) 52f else 64f
            CaptionStylePreset.NEON_PULSE -> if (useDevanagari) 46f else 54f
            CaptionStylePreset.STUDIO_MINIMAL -> if (useDevanagari) 44f else 50f
        }

        val displayWords = activeFrame.chunk.words.map { word ->
            if (!useDevanagari && config.style == CaptionStylePreset.HORMOZI_POP) {
                word.word.uppercase()
            } else {
                word.word
            }
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = baseTypeface
            textSize = textSizePx
            isFakeBoldText = useDevanagari || config.style != CaptionStylePreset.HORMOZI_POP
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = baseTypeface
            textSize = textSizePx
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeWidth = if (config.style == CaptionStylePreset.HORMOZI_POP) 14f else 8f
            color = Color.argb(245, 5, 5, 10)
            isFakeBoldText = fillPaint.isFakeBoldText
        }

        val spaceWidth = fillPaint.measureText(" ")
        val wordWidths = displayWords.map { fillPaint.measureText(it) }
        val maxLineWidth = frameWidth * 0.86f

        // Wrap into 1 or 2 clean centered lines
        val lines = mutableListOf<MutableList<Int>>()
        var currentLine = mutableListOf<Int>()
        var currentWidth = 0f
        displayWords.indices.forEach { idx ->
            val w = wordWidths[idx]
            val projected = if (currentLine.isEmpty()) w else currentWidth + spaceWidth + w
            if (currentLine.isNotEmpty() && projected > maxLineWidth) {
                lines.add(currentLine)
                currentLine = mutableListOf(idx)
                currentWidth = w
            } else {
                currentLine.add(idx)
                currentWidth = projected
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        val lineHeight = textSizePx * 1.28f
        val blockHeight = lines.size * lineHeight
        val centerY = frameHeight * config.position.verticalBias
        val topY = centerY - blockHeight / 2f

        // Optional backdrop container for NEON_PULSE or STUDIO_MINIMAL
        if (config.style == CaptionStylePreset.NEON_PULSE || config.style == CaptionStylePreset.STUDIO_MINIMAL) {
            val widestLine = lines.maxOf { line ->
                line.sumOf { wordWidths[it].toDouble() }.toFloat() + (line.size - 1).coerceAtLeast(0) * spaceWidth
            }
            val padX = 34f
            val padY = 22f
            val rect = RectF(
                (frameWidth - widestLine) / 2f - padX,
                topY - padY,
                (frameWidth + widestLine) / 2f + padX,
                topY + blockHeight + padY * 0.6f
            )
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (config.style == CaptionStylePreset.NEON_PULSE) {
                    Color.argb(205, 12, 12, 22)
                } else {
                    Color.argb(175, 8, 8, 12)
                }
            }
            canvas.drawRoundRect(rect, 28f, 28f, bgPaint)

            if (config.style == CaptionStylePreset.NEON_PULSE) {
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3.5f
                    color = Color.argb(190, 0, 229, 255)
                }
                canvas.drawRoundRect(rect, 28f, 28f, borderPaint)
            }
        }

        val fontMetrics = fillPaint.fontMetrics
        lines.forEachIndexed { lineIdx, wordIndices ->
            val lineWidth = wordIndices.sumOf { wordWidths[it].toDouble() }.toFloat() +
                (wordIndices.size - 1).coerceAtLeast(0) * spaceWidth
            var drawX = (frameWidth - lineWidth) / 2f
            val baselineY = topY + lineIdx * lineHeight - fontMetrics.ascent

            wordIndices.forEach { wordIdx ->
                val text = displayWords[wordIdx]
                val w = wordWidths[wordIdx]
                val isActive = wordIdx == activeFrame.activeWordInChunk

                when (config.style) {
                    CaptionStylePreset.HORMOZI_POP -> {
                        if (isActive) {
                            val pillRect = RectF(
                                drawX - 10f,
                                baselineY + fontMetrics.ascent - 6f,
                                drawX + w + 10f,
                                baselineY + fontMetrics.descent + 4f
                            )
                            val activePillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.rgb(204, 255, 0) // AcidLime
                            }
                            canvas.drawRoundRect(pillRect, 14f, 14f, activePillPaint)
                            fillPaint.color = Color.rgb(10, 13, 0)
                            canvas.drawText(text, drawX, baselineY, fillPaint)
                        } else {
                            fillPaint.color = Color.WHITE
                            canvas.drawText(text, drawX, baselineY, strokePaint)
                            canvas.drawText(text, drawX, baselineY, fillPaint)
                        }
                    }

                    CaptionStylePreset.NEON_PULSE -> {
                        if (isActive) {
                            val pillRect = RectF(
                                drawX - 10f,
                                baselineY + fontMetrics.ascent - 4f,
                                drawX + w + 10f,
                                baselineY + fontMetrics.descent + 4f
                            )
                            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.rgb(0, 229, 255) // CyanSpark
                            }
                            canvas.drawRoundRect(pillRect, 14f, 14f, pillPaint)
                            fillPaint.color = Color.rgb(0, 24, 32)
                            canvas.drawText(text, drawX, baselineY, fillPaint)
                        } else {
                            fillPaint.color = Color.rgb(240, 244, 255)
                            canvas.drawText(text, drawX, baselineY, fillPaint)
                        }
                    }

                    CaptionStylePreset.STUDIO_MINIMAL -> {
                        fillPaint.color = if (isActive) Color.rgb(204, 255, 0) else Color.WHITE
                        canvas.drawText(text, drawX, baselineY, strokePaint)
                        canvas.drawText(text, drawX, baselineY, fillPaint)
                    }
                }

                drawX += w + spaceWidth
            }
        }

        return bitmap
    }
}
