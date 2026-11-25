package com.mursis.anycalc.calc

import com.mursis.anycalc.model.Solution
import com.mursis.anycalc.model.SolutionSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import java.util.Locale

/**
 * Гибридный калькулятор: простые задачи считаем на устройстве, не отправляя в ИИ.
 * Поддерживает арифметику, функции, квадратные уравнения, проценты и базовую физику.
 */
@Singleton
class LocalCalculator @Inject constructor() {

    fun solve(raw: String): Solution? {
        val text = normalize(raw)
        if (text.length > 400) return null

        quadratic(text)?.let { return it }
        system(text)?.let { return it }
        polynomialOperation(text)?.let { return it }
        units(text)?.let { return it }
        percentage(text)?.let { return it }
        formula(text)?.let { return it }

        val expression = extractExpression(text) ?: return null
        val parser = Parser(expression, emptyMap())
        val value = try {
            parser.parse()
        } catch (_: Exception) {
            return null
        }
        if (!value.isFinite()) return null

        val short = format(value)
        val steps = buildList {
            add("1. Выражение: $expression")
            parser.trace.take(6).forEachIndexed { i, s -> add("${i + 2}. $s") }
            add("${parser.trace.size + 2}. Результат: $short")
        }.joinToString("\n")
        return Solution(short, steps, 0.94f, "Local engine", SolutionSource.LOCAL)
    }

    private fun normalize(s: String) = s
        .replace("²", "^2").replace("³", "^3")
        .replace("—:-", "/").replace("-:-", "/")
        .replace("›‹", "*").replace("‹", "*").replace("›", "*")
        .replace("×", "*").replace("÷", "/").replace("−", "-").replace(":", "/")
        .replace("—", "-").replace("--", "-")
        .replace(Regex("^\\s*-\\s*\\("), "(")
        .replace("–", "-").replace("·", "*").replace("х", "x")
        .replace("ё", "е").replace(" ", " ")

    private fun quadratic(text: String): Solution? {
        val re = Regex(
            """(?:(-?\d+(?:\.\d+)?)\s*\*?\s*x\s*\^\s*2)?\s*([+-]\s*\d+(?:\.\d+)?\s*x|x)\s*([+-]\s*\d+(?:\.\d+)?)\s*=\s*0""",
            RegexOption.IGNORE_CASE,
        )
        val m = re.find(text) ?: return null
        val a = m.groupValues[1].toDoubleOrNull() ?: 1.0
        if (a == 0.0) return null
        val bPart = m.groupValues[2].replace(" ", "")
        val b = if (bPart.equals("x", true)) 1.0
        else bPart.removeSuffix("x").toDoubleOrNull() ?: return null
        val c = m.groupValues[3].replace(" ", "").toDoubleOrNull() ?: return null

        val d = b * b - 4 * a * c
        val short: String
        val steps: String
        if (d >= -1e-12) {
            val dd = max(0.0, d)
            val x1 = (-b + sqrt(dd)) / (2 * a)
            val x2 = (-b - sqrt(dd)) / (2 * a)
            short = if (abs(x1 - x2) < 1e-9) "x = ${format(x1)}" else "x₁ = ${format(x1)}, x₂ = ${format(x2)}"
            steps = listOf(
                "1. Приводим к виду ax² + bx + c = 0: a = ${format(a)}, b = ${format(b)}, c = ${format(c)}",
                "2. Дискриминант: D = b² − 4ac = ${format(d)}",
                "3. x = (−b ± √D) / 2a",
                "4. Ответ: $short",
            ).joinToString("\n")
        } else {
            short = "действительных корней нет"
            steps = listOf(
                "1. a = ${format(a)}, b = ${format(b)}, c = ${format(c)}",
                "2. D = b² − 4ac = ${format(d)} < 0",
                "3. Действительных корней нет (D отрицательный).",
            ).joinToString("\n")
        }
        return Solution(short, steps, 0.97f, "Local engine", SolutionSource.LOCAL)
    }

    private fun system(text: String): Solution? {
        val re = Regex(
            """([+-]?\d*\.?\d*)\s*\*?\s*x\s*([+-]\s*\d*\.?\d*)\s*\*?\s*y\s*=\s*([+-]?\d*\.?\d*)""",
        )
        val equations = re.findAll(text).take(2).toList()
        if (equations.size != 2) return null
        fun coeff(s: String, default: Double): Double = when {
            s.isBlank() -> default
            s.trim() == "+" -> 1.0
            s.trim() == "-" -> -1.0
            else -> s.replace(" ", "").toDoubleOrNull() ?: return Double.NaN
        }
        val a1 = coeff(equations[0].groupValues[1], 1.0)
        val b1 = coeff(equations[0].groupValues[2], 1.0)
        val c1 = equations[0].groupValues[3].replace(" ", "").toDoubleOrNull() ?: return null
        val a2 = coeff(equations[1].groupValues[1], 1.0)
        val b2 = coeff(equations[1].groupValues[2], 1.0)
        val c2 = equations[1].groupValues[3].replace(" ", "").toDoubleOrNull() ?: return null
        val det = a1 * b2 - a2 * b1
        if (abs(det) < 1e-9) return null
        val x = (c1 * b2 - c2 * b1) / det
        val y = (a1 * c2 - a2 * c1) / det
        return Solution(
            "x = ${format(x)}, y = ${format(y)}",
            listOf(
                "1. Система: ${equations[0].value}; ${equations[1].value}",
                "2. Определитель: Δ = a₁b₂ − a₂b₁ = ${format(det)}",
                "3. x = (c₁b₂ − c₂b₁)/Δ = ${format(x)}",
                "4. y = (a₁c₂ − a₂c₁)/Δ = ${format(y)}",
            ).joinToString("\n"),
            0.96f,
            "Local engine",
            SolutionSource.LOCAL,
        )
    }

    private fun polynomialOperation(text: String): Solution? {
        val isDerivative = text.contains("производн", true) || text.contains("d/dx", true) || text.contains("дифференц", true)
        val isIntegral = text.contains("интеграл", true) || text.contains("∫", true)
        if (!isDerivative && !isIntegral) return null
        val terms = Regex("""([+-]?\d*\.?\d*)\s*\*?\s*x(?:\s*\^\s*(\d+))?""").findAll(text).toList()
        if (terms.isEmpty()) return null

        data class Term(val coef: Double, val power: Int)
        val parsed = terms.mapNotNull { m ->
            val raw = m.groupValues[1].replace(" ", "")
            val c = when {
                raw.isBlank() || raw == "+" -> 1.0
                raw == "-" -> -1.0
                else -> raw.toDoubleOrNull() ?: return@mapNotNull null
            }
            Term(c, m.groupValues[2].toIntOrNull() ?: 1)
        }
        val result = parsed.map { t ->
            if (isDerivative) {
                if (t.power == 0) Term(0.0, 0)
                else Term(t.coef * t.power, t.power - 1)
            } else {
                Term(t.coef / (t.power + 1), t.power + 1)
            }
        }.filter { abs(it.coef) > 1e-9 }
        val expression = buildString {
            result.forEachIndexed { i, t ->
                if (i > 0) append(if (t.coef >= 0) " + " else " − ")
                val magnitude = abs(t.coef)
                if (t.power == 0) append(format(magnitude))
                else {
                    if (magnitude != 1.0) append(format(magnitude))
                    append("x")
                    if (t.power > 1) append("^${t.power}")
                }
            }
        }.ifBlank { "0" }
        val label = if (isDerivative) "Производная" else "Неопределённый интеграл"
        return Solution(
            expression,
            listOf(
                "1. Задача: $label многочлена",
                "2. Члены: ${parsed.joinToString(", ") { "${it.coef}x^${it.power}" }}",
                "3. ${if (isDerivative) "Правило: (xⁿ)' = n·xⁿ⁻¹" else "Правило: ∫xⁿ dx = xⁿ⁺¹/(n+1)"}",
                "4. Результат: $expression${if (isIntegral) " + C" else ""}",
            ).joinToString("\n"),
            0.94f,
            "Local engine",
            SolutionSource.LOCAL,
        )
    }

    private fun units(text: String): Solution? {
        val conversions = listOf(
            Triple(Regex("""(-?\d+(?:\.\d+)?)\s*км/ч\s*(?:в|to|->)\s*м/с""", RegexOption.IGNORE_CASE), 1.0 / 3.6, "м/с"),
            Triple(Regex("""(-?\d+(?:\.\d+)?)\s*м/с\s*(?:в|to|->)\s*км/ч""", RegexOption.IGNORE_CASE), 3.6, "км/ч"),
            Triple(Regex("""(-?\d+(?:\.\d+)?)\s*км\s*(?:в|to|->)\s*мил""", RegexOption.IGNORE_CASE), 0.621371, "миль"),
            Triple(Regex("""(-?\d+(?:\.\d+)?)\s*кг\s*(?:в|to|->)\s*фунт""", RegexOption.IGNORE_CASE), 2.20462, "фунтов"),
        )
        for ((regex, factor, target) in conversions) {
            val m = regex.find(text) ?: continue
            val value = m.groupValues[1].toDouble() * factor
            return Solution(
                "${format(value)} $target",
                listOf("1. Перевод единиц", "2. Множитель: ${format(factor)}", "3. Результат: ${format(value)} $target").joinToString("\n"),
                0.97f,
                "Local engine",
                SolutionSource.LOCAL,
            )
        }
        val speed = Regex("""(?:скорость|v)\s*(\d+(?:\.\d+)?)\s*км/ч""", RegexOption.IGNORE_CASE).find(text)
        val time = Regex("""(?:время|t)\s*(\d+(?:\.\d+)?)\s*ч""", RegexOption.IGNORE_CASE).find(text)
        if (speed != null && time != null) {
            val s = speed.groupValues[1].toDouble()
            val t = time.groupValues[1].toDouble()
            val d = s * t
            return Solution(
                "${format(d)} км",
                listOf("1. Дано: v = ${format(s)} км/ч, t = ${format(t)} ч", "2. S = v · t", "3. S = ${format(d)} км").joinToString("\n"),
                0.96f,
                "Local engine",
                SolutionSource.LOCAL,
            )
        }
        return null
    }

    private fun percentage(text: String): Solution? {
        val m = Regex("""(-?\d+(?:\.\d+)?)\s*%\s*(?:от|of|из)\s*(-?\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            .find(text) ?: return null
        val p = m.groupValues[1].toDouble()
        val base = m.groupValues[2].toDouble()
        val res = base * p / 100.0
        return Solution(
            format(res),
            listOf(
                "1. Процент от числа: P = основа × процент / 100",
                "2. P = ${format(base)} × ${format(p)} / 100",
                "3. Результат: ${format(res)}",
            ).joinToString("\n"),
            0.97f,
            "Local engine",
            SolutionSource.LOCAL,
        )
    }

    private fun formula(text: String): Solution? {
        val vars = Regex("""\b([a-zA-Z])\s*=\s*(-?\d+(?:\.\d+)?)\b""").findAll(text)
            .associate { it.groupValues[1] to it.groupValues[2].toDouble() }
        if (vars.isEmpty()) return null
        val expr = when {
            text.contains("F=ma", true) || text.contains("F = m*a", true) -> "m*a"
            text.contains("v=s/t", true) || text.contains("v = s/t", true) -> "s/t"
            text.contains("E=mc", true) -> "m*c*c"
            text.contains("P=F/S", true) -> "F/S"
            text.contains("I=U/R", true) -> "U/R"
            text.contains("a=(v-v0)/t", true) -> "(v-v0)/t"
            else -> return null
        }
        val value = try {
            Parser(expr, vars).parse()
        } catch (_: Exception) {
            return null
        }
        if (!value.isFinite()) return null
        return Solution(
            format(value),
            listOf(
                "1. Дано: ${vars.entries.joinToString(", ") { "${it.key} = ${format(it.value)}" }}",
                "2. Формула: $expr",
                "3. Подставляем значения: ${format(value)}",
            ).joinToString("\n"),
            0.95f,
            "Local engine",
            SolutionSource.LOCAL,
        )
    }

    private fun extractExpression(text: String): String? {
        var cleaned = text
            .replace(Regex("""=\s*\?"""), " ")
            .replace(Regex("""(?i)(реши|вычисли|посчитай|найди|calcul|solve|compute|what is|сколько|чему равно)"""), " ")
            .replace(Regex("""[^\dA-Za-z+\-*/^().,\s=]"""), " ")
            .trim()
        val eq = cleaned.indexOf('=')
        if (eq >= 0) cleaned = cleaned.substring(eq + 1).trim()
        if (!cleaned.any { it.isDigit() }) return null
        val hasOperator = cleaned.any { it in "+-*/^" }
        val hasFunction = Regex("[A-Za-z]+\\s*\\(").containsMatchIn(cleaned)
        if (!hasOperator && !hasFunction) return null
        if (cleaned.length > 120) return null
        return cleaned
    }

    private fun format(v: Double): String {
        if (v == v.toLong().toDouble() && abs(v) < 1e12) return v.toLong().toString()
        val s = String.format(Locale.US, "%.6f", v)
        return s.trimEnd('0').trimEnd('.')
    }

    private class Parser(private val src: String, private val vars: Map<String, Double>) {
        private val tokens = tokenize(src)
        private var pos = 0
        val trace = mutableListOf<String>()

        fun parse(): Double {
            val v = expr()
            if (pos < tokens.size) throw IllegalArgumentException("unexpected token: ${tokens[pos]}")
            return v
        }

        private fun expr(): Double {
            var v = term()
            while (pos < tokens.size && (tokens[pos] == "+" || tokens[pos] == "-")) {
                val op = tokens[pos++]
                val r = term()
                val next = if (op == "+") v + r else v - r
                trace.add("${fmt(v)} $op ${fmt(r)} = ${fmt(next)}")
                v = next
            }
            return v
        }

        private fun term(): Double {
            var v = power()
            while (pos < tokens.size && tokens[pos] in setOf("*", "/", "%")) {
                val op = tokens[pos++]
                val r = power()
                val next = when (op) {
                    "*" -> v * r
                    "/" -> v / r
                    else -> v % r
                }
                trace.add("${fmt(v)} $op ${fmt(r)} = ${fmt(next)}")
                v = next
            }
            return v
        }

        private fun power(): Double {
            var v = unary()
            while (pos < tokens.size && tokens[pos] == "^") {
                pos++
                val r = power()
                val next = v.pow(r)
                trace.add("${fmt(v)}^${fmt(r)} = ${fmt(next)}")
                v = next
            }
            return v
        }

        private fun unary(): Double {
            if (pos < tokens.size && tokens[pos] == "-") {
                pos++
                return -unary()
            }
            if (pos < tokens.size && tokens[pos] == "+") {
                pos++
                return unary()
            }
            return atom()
        }

        private fun atom(): Double {
            if (pos >= tokens.size) throw IllegalArgumentException("end of expression")
            val t = tokens[pos++]
            if (t == "(") {
                val v = expr()
                if (pos >= tokens.size || tokens[pos] != ")") throw IllegalArgumentException("missing )")
                pos++
                return v
            }
            val num = t.toDoubleOrNull()
            if (num != null) return num
            if (pos < tokens.size && tokens[pos] == "(") {
                pos++
                val args = mutableListOf<Double>()
                if (pos < tokens.size && tokens[pos] != ")") {
                    args.add(expr())
                    while (pos < tokens.size && tokens[pos] == ",") {
                        pos++
                        args.add(expr())
                    }
                }
                if (pos >= tokens.size || tokens[pos] != ")") throw IllegalArgumentException("missing )")
                pos++
                return call(t, args)
            }
            return vars[t] ?: constants[t] ?: throw IllegalArgumentException("unknown variable: $t")
        }

        private fun call(name: String, a: List<Double>): Double = when (name.lowercase()) {
            "sqrt" -> sqrt(a[0])
            "cbrt" -> Math.cbrt(a[0])
            "abs" -> abs(a[0])
            "sin" -> sin(a[0])
            "cos" -> cos(a[0])
            "tan" -> tan(a[0])
            "ln" -> ln(a[0])
            "log" -> if (a.size > 1) log10(a[0]) / log10(a[1]) else log10(a[0])
            "log10" -> log10(a[0])
            "exp" -> exp(a[0])
            "round" -> round(a[0])
            "floor" -> floor(a[0])
            "ceil" -> ceil(a[0])
            "pow" -> a[0].pow(a[1])
            "min" -> min(a[0], a[1])
            "max" -> max(a[0], a[1])
            else -> throw IllegalArgumentException("unknown function: $name")
        }

        private fun fmt(v: Double) = if (v == v.toLong().toDouble()) v.toLong().toString() else String.format(Locale.US, "%.4f", v)

        companion object {
            val constants = mapOf("pi" to Math.PI, "e" to Math.E, "π" to Math.PI)

            private val tokenRegex = Regex("""\d+(?:\.\d+)?|[A-Za-zπ_][A-Za-z0-9_]*|[+\-*/^%(),=]""")

            fun tokenize(s: String): List<String> {
                val out = mutableListOf<String>()
                var i = 0
                while (i < s.length) {
                    if (s[i].isWhitespace()) {
                        i++
                        continue
                    }
                    val m = tokenRegex.matchAt(s, i)
                    if (m == null) {
                        i++
                        continue
                    }
                    out.add(m.value)
                    i = m.range.last + 1
                }
                return out
            }
        }
    }
}
