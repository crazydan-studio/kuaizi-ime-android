package org.crazydan.studio.app.ime.kuaizi.engine.input_action

import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey

class InputActionScriptCompiler {

    fun compile(
        text: String,
        mode: InputActionMode,
    ): InputActionScript {
        val pinyinSegments = textToPinyinSegments(text)
        val actions = when (mode) {
            InputActionMode.Tap -> compileTapActions(pinyinSegments)
            InputActionMode.Swipe -> compileSwipeActions(pinyinSegments)
        }
        val totalDuration = actions.maxOfOrNull { a ->
            a.startTime + when (a) {
                is InputAction.SwipeTo -> a.duration
                is InputAction.Wait -> a.duration
                else -> 0L
            }
        } ?: 0L

        return InputActionScript(
            name = "$text-${mode.name}",
            description = "输入「$text」的${mode.name}模式脚本，共 ${actions.size} 个动作",
            inputActionMode = mode,
            actions = actions,
            totalDuration = totalDuration,
        )
    }

    private fun compileTapActions(segments: List<PinyinSegment>): List<InputAction> {
        val actions = mutableListOf<InputAction>()
        var currentTime = 0L

        for ((segmentIndex, segment) in segments.withIndex()) {
            for ((charIndex, char) in segment.letters.withIndex()) {
                val key = requireKeyForChar(char)
                actions.add(InputAction.KeyDown(currentTime, key))
                currentTime += KEY_DOWN_DURATION
                actions.add(InputAction.KeyUp(currentTime, key))
                currentTime += KEY_UP_DURATION

                val isLastChar = charIndex == segment.letters.lastIndex
                if (!isLastChar) {
                    actions.add(InputAction.Wait(currentTime, TAP_KEY_INTERVAL))
                    currentTime += TAP_KEY_INTERVAL
                }
            }

            actions.add(InputAction.SelectCandidate(currentTime, 0))
            currentTime += CANDIDATE_SELECT_DURATION

            val isLastSegment = segmentIndex == segments.lastIndex
            if (!isLastSegment) {
                actions.add(InputAction.Wait(currentTime, TAP_SYLLABLE_INTERVAL))
                currentTime += TAP_SYLLABLE_INTERVAL
            }
        }

        return actions
    }

    private fun compileSwipeActions(segments: List<PinyinSegment>): List<InputAction> {
        val actions = mutableListOf<InputAction>()
        var currentTime = 0L

        for ((segmentIndex, segment) in segments.withIndex()) {
            val keys = segment.letters.map { requireKeyForChar(it) }

            actions.add(InputAction.KeyDown(currentTime, keys.first()))
            currentTime += KEY_DOWN_DURATION

            for (i in 1 until keys.size) {
                val fromKey = keys[i - 1]
                val toKey = keys[i]
                val duration = calculateSwipeDuration(fromKey, toKey)
                actions.add(InputAction.SwipeTo(currentTime, fromKey, toKey, duration))
                currentTime += duration
            }

            actions.add(InputAction.KeyUp(currentTime, keys.last()))
            currentTime += KEY_UP_DURATION

            actions.add(InputAction.SelectCandidate(currentTime, 0))
            currentTime += CANDIDATE_SELECT_DURATION

            val isLastSegment = segmentIndex == segments.lastIndex
            if (!isLastSegment) {
                actions.add(InputAction.Wait(currentTime, SWIPE_SYLLABLE_INTERVAL))
                currentTime += SWIPE_SYLLABLE_INTERVAL
            }
        }

        return actions
    }

    private fun calculateSwipeDuration(fromKey: InputKey, toKey: InputKey): Long {
        val distance = estimateKeyDistance(fromKey, toKey)
        return (SWIPE_BASE_DURATION + (distance * SWIPE_DISTANCE_FACTOR).toLong())
            .coerceIn(SWIPE_MIN_DURATION, SWIPE_MAX_DURATION)
    }

    private fun estimateKeyDistance(fromKey: InputKey, toKey: InputKey): Float {
        if (fromKey == toKey) return 0.0f
        return 1.0f
    }

    private fun requireKeyForChar(char: String): InputKey {
        return InputKey.Char(text = char)
    }

    private fun textToPinyinSegments(text: String): List<PinyinSegment> {
        val segments = mutableListOf<PinyinSegment>()

        for (char in text) {
            when {
                char.isCJKCharacter() -> {
                    val pinyin = pinyinForChar(char)
                        ?: throw IllegalArgumentException(
                            "无法将字符「$char」转换为拼音"
                        )
                    segments.add(
                        PinyinSegment(
                            originalText = char.toString(),
                            letters = pinyin.toList().map { it.toString() },
                        )
                    )
                }

                char.isLetter() && char.code < 128 -> {
                    segments.add(
                        PinyinSegment(
                            originalText = char.toString(),
                            letters = listOf(char.lowercaseChar().toString()),
                        )
                    )
                }

                else -> {
                    throw IllegalArgumentException(
                        "不支持的字符「$char」，仅支持中文和拉丁字母"
                    )
                }
            }
        }

        return segments
    }

    private fun pinyinForChar(char: Char): String? {
        val code = char.code
        val basicPinyins = mapOf(
            0x4E2D to "zhong",
            0x56FD to "guo",
            0x4EBA to "ren",
            0x5927 to "da",
            0x5C0F to "xiao",
            0x4E00 to "yi",
            0x4E8C to "er",
            0x4E09 to "san",
        )
        return basicPinyins[code]
    }

    private fun Char.isCJKCharacter(): Boolean {
        val code = this.code
        return code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF
    }

    private data class PinyinSegment(
        val originalText: String,
        val letters: List<String>,
    )

    companion object {
        private const val KEY_DOWN_DURATION = 50L
        private const val KEY_UP_DURATION = 30L
        private const val TAP_KEY_INTERVAL = 80L
        private const val TAP_SYLLABLE_INTERVAL = 200L
        private const val SWIPE_SYLLABLE_INTERVAL = 300L
        private const val CANDIDATE_SELECT_DURATION = 100L
        private const val SWIPE_BASE_DURATION = 100L
        private const val SWIPE_DISTANCE_FACTOR = 50f
        private const val SWIPE_MIN_DURATION = 60L
        private const val SWIPE_MAX_DURATION = 300L
    }
}
