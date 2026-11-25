package com.mursis.anycalc

import com.mursis.anycalc.calc.LocalCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LocalCalculatorTest {
    private val calculator = LocalCalculator()

    @Test
    fun arithmetic() {
        assertEquals("8", calculator.solve("2+2*3")?.short)
        assertEquals("25", calculator.solve("(2+3)^2")?.short)
        assertEquals("4", calculator.solve("sqrt(16)")?.short)
    }

    @Test
    fun quadraticEquation() {
        val result = calculator.solve("2x^2 - 8x + 6 = 0")
        assertNotNull(result)
        assertEquals("x₁ = 3, x₂ = 1", result?.short)
    }

    @Test
    fun percentages() {
        assertEquals("30", calculator.solve("20% от 150")?.short)
    }

    @Test
    fun physics() {
        assertEquals("6", calculator.solve("m=2, a=3, F=ma")?.short)
    }

    @Test
    fun systemOfEquations() {
        val result = calculator.solve("2x + 3y = 7, x - y = 1")
        assertNotNull(result)
        assertEquals("x = 2, y = 1", result?.short)
    }

    @Test
    fun derivativeAndIntegral() {
        assertEquals("3x^2 + 4x", calculator.solve("производная x^3 + 2x^2")?.short)
        assertEquals("0.333333x^3", calculator.solve("интеграл x^2")?.short)
    }

    @Test
    fun unitsAndWordProblem() {
        assertEquals("2.777778 м/с", calculator.solve("10 км/ч в м/с")?.short)
        assertEquals("120 км", calculator.solve("скорость 60 км/ч, время 2 ч")?.short)
    }

    @Test
    fun chalkboardExpression() {
        assertEquals("17", calculator.solve("(18-4×3)+24÷(7-3)×2-(15-6×2)÷3=?")?.short)
    }

    @Test
    fun garbledOcrExpression() {
        assertEquals(
            "17",
            calculator.solve("— (18 -- 4 ›‹ 3) + 24 -:- (7-3) ›‹2 - (15—6›‹2)—:-3 =?")?.short,
        )
    }

    @Test
    fun irrelevantTextIsNotSolved() {
        assertNull(calculator.solve("Привет, как дела?"))
    }
}
