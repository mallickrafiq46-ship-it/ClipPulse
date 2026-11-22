package com.example.data

import com.example.model.CaptionLanguage
import com.example.model.VideoAnalysisResult
import com.example.model.VideoMetadata
import com.example.model.ViralClip
import com.example.model.WordCaption
import kotlinx.coroutines.delay

/**
 * Single AI boundary for viral video analysis.
 * Can be backed by Firebase AI Logic (Gemini) once Firebase is enabled,
 * or by [PlaceholderVideoAnalysisRepository] when running without Firebase.
 */
interface VideoAnalysisRepository {
    suspend fun analyzeVideo(
        metadata: VideoMetadata,
        onStageUpdate: (stageIndex: Int, statusText: String) -> Unit
    ): VideoAnalysisResult
}

/**
 * Placeholder implementation used when Firebase setup is declined.
 * Produces clearly labeled sample viral moments and multilingual word-by-word captions
 * scaled accurately to the selected video's actual duration so previewing, trimming,
 * 9:16 cropping, caption burn-in, and MP4 export work end-to-end on real videos.
 */
class PlaceholderVideoAnalysisRepository : VideoAnalysisRepository {

    override suspend fun analyzeVideo(
        metadata: VideoMetadata,
        onStageUpdate: (stageIndex: Int, statusText: String) -> Unit
    ): VideoAnalysisResult {
        onStageUpdate(0, "Reading video frames & audio track...")
        delay(450)
        onStageUpdate(1, "Scanning hook spikes & retention peaks...")
        delay(550)
        onStageUpdate(2, "Generating Hinglish, Hindi & English word timings...")
        delay(550)
        onStageUpdate(3, "Ranking top viral moments...")
        delay(350)

        val totalMs = metadata.usableDurationMs
        val clipTemplates = sampleTemplates()

        // Compute non-overlapping (or smartly distributed) clip windows clamped to real video length
        val clipCount = if (totalMs < 12_000L) 3 else 4
        val clips = (0 until clipCount).map { index ->
            val template = clipTemplates[index]
            val (startMs, endMs) = computeClipWindow(
                totalDurationMs = totalMs,
                index = index,
                count = clipCount
            )
            val captionsMap = mapOf(
                CaptionLanguage.HINGLISH to buildTimedCaptions(template.hinglishScript, startMs, endMs),
                CaptionLanguage.HINDI to buildTimedCaptions(template.hindiScript, startMs, endMs),
                CaptionLanguage.ENGLISH to buildTimedCaptions(template.englishScript, startMs, endMs)
            )
            ViralClip(
                id = "clip_${index + 1}",
                rank = index + 1,
                title = template.title,
                hookReason = template.hookReason,
                startMs = startMs,
                endMs = endMs,
                viralityScore = template.viralityScore,
                hookScore = template.hookScore,
                pacingScore = template.pacingScore,
                clarityScore = template.clarityScore,
                hashtags = template.hashtags,
                captionsByLanguage = captionsMap
            )
        }

        return VideoAnalysisResult(
            clips = clips,
            isPlaceholderAi = true,
            noticeMessage = "Sample AI highlights shown — enable Firebase AI Logic to connect live Gemini video analysis."
        )
    }

    private fun computeClipWindow(
        totalDurationMs: Long,
        index: Int,
        count: Int
    ): Pair<Long, Long> {
        if (totalDurationMs <= 5_000L) {
            return 0L to totalDurationMs.coerceAtLeast(1_500L)
        }
        val idealClipLength = when {
            totalDurationMs <= 20_000L -> (totalDurationMs * 0.45f).toLong().coerceIn(4_000L, 10_000L)
            totalDurationMs <= 90_000L -> 18_000L.coerceAtMost(totalDurationMs / 2)
            else -> 32_000L.coerceAtMost(totalDurationMs / count)
        }
        val maxStart = (totalDurationMs - idealClipLength).coerceAtLeast(0L)
        val fraction = if (count <= 1) 0f else index.toFloat() / (count - 1).toFloat()
        // Add slight inset so first clip starts right near a hook and last ends cleanly
        val adjustedFraction = (fraction * 0.88f + 0.04f).coerceIn(0f, 1f)
        val startMs = (maxStart * adjustedFraction).toLong().coerceIn(0L, maxStart)
        val endMs = (startMs + idealClipLength).coerceAtMost(totalDurationMs)
        return startMs to endMs
    }

    private fun buildTimedCaptions(
        script: String,
        clipStartMs: Long,
        clipEndMs: Long
    ): List<WordCaption> {
        val words = script.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val duration = (clipEndMs - clipStartMs).coerceAtLeast(1_000L)
        val leadIn = (duration * 0.03f).toLong().coerceAtMost(350L)
        val usable = (duration - leadIn - 200L).coerceAtLeast(600L)

        // Weight word duration slightly by word length for natural rhythm
        val weights = words.map { it.length.coerceIn(2, 10).toFloat() }
        val totalWeight = weights.sum().coerceAtLeast(1f)

        var cursor = clipStartMs + leadIn
        return words.mapIndexed { idx, word ->
            val slice = ((weights[idx] / totalWeight) * usable).toLong().coerceAtLeast(120L)
            val wordStart = cursor
            val wordEnd = if (idx == words.lastIndex) {
                (clipEndMs - 100L).coerceAtLeast(wordStart + 120L)
            } else {
                (wordStart + slice).coerceAtMost(clipEndMs)
            }
            cursor = wordEnd
            WordCaption(word = word, startMs = wordStart, endMs = wordEnd)
        }
    }

    private data class SampleClipTemplate(
        val title: String,
        val hookReason: String,
        val viralityScore: Int,
        val hookScore: Int,
        val pacingScore: Int,
        val clarityScore: Int,
        val hashtags: List<String>,
        val hinglishScript: String,
        val hindiScript: String,
        val englishScript: String
    )

    private fun sampleTemplates(): List<SampleClipTemplate> = listOf(
        SampleClipTemplate(
            title = "Ye 1 Mistake Aapki Growth Rok Rahi Hai!",
            hookReason = "Pattern-interrupt opener in the first 2 seconds with high emotional tension and crisp payoff.",
            viralityScore = 96,
            hookScore = 98,
            pacingScore = 95,
            clarityScore = 94,
            hashtags = listOf("#ReelsIndia", "#CreatorTips", "#ViralHook", "#Shorts"),
            hinglishScript = "Ruko! Agar aap roz video bana rahe ho par views nahi aa rahe, toh ye ek chhoti si galti sab kuch rok rahi hai. Pehle teen second mein hook strong rakho, aur magic khud dekho!",
            hindiScript = "रुको! अगर आप रोज़ वीडियो बना रहे हो पर व्यूज़ नहीं आ रहे, तो यह एक छोटी सी गलती सब कुछ रोक रही है। पहले तीन सेकंड में हुक स्ट्रॉन्ग रखो, और जादू खुद देखो!",
            englishScript = "Stop scrolling! If you post every single day and still get stuck in low views, this one simple mistake is holding everything back. Nail the first three seconds and watch retention explode!"
        ),
        SampleClipTemplate(
            title = "The Secret Framework Nobody Talks About",
            hookReason = "Curiosity gap framing with rhythmic cadence ideal for Shorts and Instagram Reels retention.",
            viralityScore = 92,
            hookScore = 94,
            pacingScore = 91,
            clarityScore = 92,
            hashtags = listOf("#Mindset", "#GrowthHacks", "#Trending", "#ContentCreator"),
            hinglishScript = "Sach bataun? Log ghanton editing mein lagate hain, lekin asli game storytelling ka hota hai. Ek clear problem bolo, phir instant solution do—audience end tak rukegi.",
            hindiScript = "सच बताऊँ? लोग घंटों एडिटिंग में लगाते हैं, लेकिन असली खेल स्टोरीटेलिंग का होता है। एक साफ़ समस्या बताओ, फिर तुरंत समाधान दो—दर्शक अंत तक रुकेंगे।",
            englishScript = "Here is the truth nobody shares. People spend hours over-editing when the real secret is tight storytelling. Name the pain clearly, deliver the payoff fast, and viewers stay till the final frame."
        ),
        SampleClipTemplate(
            title = "30 Seconds Mein Complete Game Changer",
            hookReason = "Fast-paced punchy delivery with clear takeaway that drives saves and shares.",
            viralityScore = 88,
            hookScore = 89,
            pacingScore = 90,
            clarityScore = 86,
            hashtags = listOf("#Productivity", "#ReelsViral", "#SmartTips", "#Hinglish"),
            hinglishScript = "Is trick ko abhi save kar lo kyunki baad mein dhoondhte reh jaoge. Bas apna main moment pick karo, bold captions lagao, aur vertical format mein export karke post kar do!",
            hindiScript = "इस ट्रिक को अभी सेव कर लो क्योंकि बाद में ढूँढते रह जाओगे। बस अपना मुख्य पल चुनें, बोल्ड कैप्शन लगाएँ, और वर्टिकल फॉर्मेट में एक्सपोर्ट करके पोस्ट कर दें!",
            englishScript = "Save this clip right now before you lose it. Pick your strongest moment, drop bold animated captions on screen, frame it in crisp vertical nine-sixteen, and hit publish today!"
        ),
        SampleClipTemplate(
            title = "Unfiltered Take That Sparks Comments",
            hookReason = "Contrarian insight paired with direct viewer question to boost comment engagement.",
            viralityScore = 84,
            hookScore = 86,
            pacingScore = 82,
            clarityScore = 85,
            hashtags = listOf("#HotTake", "#PodcastClip", "#ReelItFeelIt", "#Creators"),
            hinglishScript = "Har koi perfect setup ka wait karta rehta hai, par asli viral clips phone se hi shoot hote hain. Aap kya sochte ho—quality zyada important hai ya consistency? Comments mein batao!",
            hindiScript = "हर कोई परफेक्ट सेटअप का इंतज़ार करता रहता है, पर असली वायरल क्लिप्स फ़ोन से ही शूट होते हैं। आप क्या सोचते हो—क्वालिटी ज़्यादा ज़रूरी है या कंसिस्टेंसी? कमेंट्स में बताओ!",
            englishScript = "Everyone keeps waiting for a fancy studio setup, yet the biggest viral clips are filmed right on a phone. What matters more to you—studio polish or raw consistency? Drop your take below!"
        )
    )
}
