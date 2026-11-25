package com.mursis.anycalc.model

/** Источник ответа: локальные вычисления, кэш или ИИ-провайдер. */
enum class SolutionSource { LOCAL, CACHE, AI }

data class Solution(
    val short: String,
    val detailed: String,
    val confidence: Float,
    val provider: String,
    val source: SolutionSource,
    val elapsedMs: Long = 0,
)
