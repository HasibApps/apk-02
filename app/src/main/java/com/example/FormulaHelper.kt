package com.example

import java.util.regex.Matcher
import java.util.regex.Pattern

object FormulaHelper {

    private val SUBSCRIPT_DIGITS = charArrayOf('₀', '₁', '₂', '₃', '₄', '₅', '₆', '₇', '₈', '₉')
    private val SUPERSCRIPT_DIGITS = charArrayOf('⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹')

    fun toSubscript(digits: String?): String {
        if (digits == null) return ""
        val sb = StringBuilder()
        for (c in digits) {
            when {
                c in '0'..'9' -> sb.append(SUBSCRIPT_DIGITS[c - '0'])
                c == '+' -> sb.append('₊')
                c == '-' -> sb.append('₋')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun toSuperscript(digits: String?): String {
        if (digits == null) return ""
        val sb = StringBuilder()
        for (c in digits) {
            when {
                c in '0'..'9' -> sb.append(SUPERSCRIPT_DIGITS[c - '0'])
                c == '+' -> sb.append('⁺')
                c == '-' -> sb.append('⁻')
                c == 'n' -> sb.append('ⁿ')
                c == 'x' -> sb.append('ˣ')
                c == 't' -> sb.append('ᵗ')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun formatScientificText(input: String?): String {
        if (input == null || input.trim().isEmpty()) return input ?: ""
        var res = input

        // 1. Clean LaTeX delimiters
        res = res.replace(Regex("\\$\\$([^$]+)\\$\\$"), "$1")
        res = res.replace(Regex("\\$([^$]+)\\$"), "$1")

        // 2. Arrows
        res = res.replace(Regex("<==>|<=>|<->|<-->"), " ⇌ ")
        res = res.replace(Regex("-->|->"), " → ")
        res = res.replace(Regex("<--|<-"), " ← ")
        res = res.replace("\\\\rightarrow", " → ")
        res = res.replace("\\\\leftarrow", " ← ")
        res = res.replace("\\\\rightleftharpoons", " ⇌ ")
        res = res.replace("\\\\uparrow", " ↑ ")
        res = res.replace("\\\\downarrow", " ↓ ")

        // 3. Operators & relations
        res = res.replace(Regex("\\\\times|\\s*\\*\\s*"), " × ")
        res = res.replace("\\\\div", " ÷ ")
        res = res.replace(Regex("\\\\pm|\\+/-"), " ± ")
        res = res.replace(Regex("\\\\approx|~="), " ≈ ")
        res = res.replace(Regex("\\\\neq|!="), " ≠ ")
        res = res.replace(Regex("\\\\leq|<="), " ≤ ")
        res = res.replace(Regex("\\\\geq|>="), " ≥ ")
        res = res.replace("\\\\infty", " ∞ ")
        res = res.replace(Regex("\\\\sqrt\\{([^}]+)\\}"), "√($1)")
        res = res.replace("\\\\sqrt", "√")
        res = res.replace("\\\\int", "∫")
        res = res.replace("\\\\sum", "∑")
        res = res.replace(Regex("\\\\degree|\\\\deg"), "°")

        // 4. Fractions
        res = res.replace(Regex("\\\\frac\\{([^}]+)\\}\\{([^}]+)\\}"), "($1 / $2)")
        res = res.replace(Regex("\\b1/2\\b"), "½")
        res = res.replace(Regex("\\b1/4\\b"), "¼")
        res = res.replace(Regex("\\b3/4\\b"), "¾")
        res = res.replace(Regex("\\b1/3\\b"), "⅓")
        res = res.replace(Regex("\\b2/3\\b"), "⅔")

        // 5. Greek letters
        res = res.replace(Regex("\\\\alpha|\\balpha\\b"), "α")
        res = res.replace(Regex("\\\\beta|\\bbeta\\b"), "β")
        res = res.replace(Regex("\\\\gamma|\\bgamma\\b"), "γ")
        res = res.replace(Regex("\\\\Delta|\\bDelta\\b"), "Δ")
        res = res.replace(Regex("\\\\delta|\\bdelta\\b"), "δ")
        res = res.replace(Regex("\\\\theta|\\btheta\\b"), "θ")
        res = res.replace(Regex("\\\\lambda|\\blambda\\b"), "λ")
        res = res.replace(Regex("\\\\mu|\\bmu\\b"), "μ")
        res = res.replace(Regex("\\\\pi|\\bpi\\b"), "π")
        res = res.replace(Regex("\\\\rho|\\brho\\b"), "ρ")
        res = res.replace(Regex("\\\\sigma|\\bsigma\\b"), "σ")
        res = res.replace(Regex("\\\\tau|\\btau\\b"), "τ")
        res = res.replace(Regex("\\\\phi|\\bphi\\b"), "φ")
        res = res.replace(Regex("\\\\omega|\\bomega\\b"), "ω")
        res = res.replace(Regex("\\\\Omega|\\bOmega\\b"), "Ω")
        res = res.replace(Regex("\\\\epsilon|\\bepsilon\\b"), "ε")

        // 6. Explicit superscripts
        res = replacePattern(res, "\\^\\{([0-9+\\-nxt]+)\\}") { m -> toSuperscript(m.group(1)) }
        res = replacePattern(res, "\\^([0-9+\\-nxt])") { m -> toSuperscript(m.group(1)) }

        // 7. Explicit subscripts
        res = replacePattern(res, "_\\{([0-9+\\-]+)\\}") { m -> toSubscript(m.group(1)) }
        res = replacePattern(res, "_([0-9])") { m -> toSubscript(m.group(1)) }

        res = res.replace(Regex("([vVuUaAfFmMpPrRsStT])_0"), "$1₀")
        res = res.replace(Regex("([vVuUaAfFmMpPrRsStT])_1"), "$1₁")
        res = res.replace(Regex("([vVuUaAfFmMpPrRsStT])_2"), "$1₂")
        res = res.replace(Regex("([vVuUaAfFmMpPrRsStT])_i"), "$1ᵢ")
        res = res.replace(Regex("([vVuUaAfFmMpPrRsStT])_f"), "$1𝒻")

        // 8. Chemistry
        res = formatCommonChemicalFormulas(res)
        res = formatGeneralChemicalSubscripts(res)

        res = res.replace(Regex("  +"), " ")
        return res
    }

    private fun replacePattern(input: String, regex: String, transform: (Matcher) -> String): String {
        val matcher = Pattern.compile(regex).matcher(input)
        val sb = StringBuffer()
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(transform(matcher)))
        }
        matcher.appendTail(sb)
        return sb.toString()
    }

    private fun formatCommonChemicalFormulas(text: String): String {
        var res = text
        val replacements = listOf(
            "\\bH2O\\b" to "H₂O",
            "\\bCO2\\b" to "CO₂",
            "\\bO2\\b" to "O₂",
            "\\bH2\\b" to "H₂",
            "\\bN2\\b" to "N₂",
            "\\bCl2\\b" to "Cl₂",
            "\\bNH3\\b" to "NH₃",
            "\\bCH4\\b" to "CH₄",
            "\\bSO2\\b" to "SO₂",
            "\\bSO3\\b" to "SO₃",
            "\\bNO2\\b" to "NO₂",
            "\\bH2SO4\\b" to "H₂SO₄",
            "\\bHNO3\\b" to "HNO₃",
            "\\bH3PO4\\b" to "H₃PO₄",
            "\\bCH3COOH\\b" to "CH₃COOH",
            "\\bCa\\(OH\\)2\\b" to "Ca(OH)₂",
            "\\bMg\\(OH\\)2\\b" to "Mg(OH)₂",
            "\\bAl\\(OH\\)3\\b" to "Al(OH)₃",
            "\\bCaCO3\\b" to "CaCO₃",
            "\\bNa2CO3\\b" to "Na₂CO₃",
            "\\bNaHCO3\\b" to "NaHCO₃",
            "\\bCuSO4\\b" to "CuSO₄",
            "\\bFe2O3\\b" to "Fe₂O₃",
            "\\bAl2O3\\b" to "Al₂O₃",
            "\\bKMnO4\\b" to "KMnO₄",
            "\\bK2Cr2O7\\b" to "K₂Cr₂O₇",
            "\\bC6H12O6\\b" to "C₆H₁₂O₆",
            "\\bC2H5OH\\b" to "C₂H₅OH",
            "\\bBaSO4\\b" to "BaSO₄"
        )
        for ((pat, rep) in replacements) {
            res = res.replace(Regex(pat), rep)
        }
        res = res.replace(Regex("([A-Za-z0-9\\]\\)])\\^?(2\\+|\\+2)"), "$1²⁺")
        res = res.replace(Regex("([A-Za-z0-9\\]\\)])\\^?(3\\+|\\+3)"), "$1³⁺")
        res = res.replace(Regex("([A-Za-z0-9\\]\\)])\\^?(2-|\\-2)"), "$1²⁻")
        res = res.replace(Regex("([A-Za-z0-9\\]\\)])\\^\\+"), "$1⁺")
        res = res.replace(Regex("([A-Za-z0-9\\]\\)])\\^-"), "$1⁻")
        return res
    }

    private fun formatGeneralChemicalSubscripts(text: String): String {
        return replacePattern(text, "(?<=[A-Z][a-z]?|\\))([0-9]+)") { m ->
            toSubscript(m.group(1))
        }
    }

    fun generateHtmlForFormulas(content: String?, isDarkMode: Boolean): String {
        val bgColor = if (isDarkMode) "#121212" else "#FFFFFF"
        val textColor = if (isDarkMode) "#E0E0E0" else "#1A1A1A"
        val cardBg = if (isDarkMode) "#1E1E1E" else "#F8F9FA"
        val cardBorder = if (isDarkMode) "#333333" else "#E2E8F0"
        val formulaColor = if (isDarkMode) "#90CAF9" else "#1565C0"
        val badgeBg = if (isDarkMode) "#0D47A1" else "#E3F2FD"
        val badgeText = if (isDarkMode) "#BBDEFB" else "#0D47A1"

        var safeContent = (content ?: "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        safeContent = safeContent.replace(
            Regex("(\\((?:পৃষ্ঠা|Page)\\s*=\\s*[0-9০-৯]+\\))"),
            "<span class='page-badge'>$1</span>"
        )
        safeContent = safeContent.replace("\n", "<br/>")

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes"/>
                <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css"/>
                <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
                <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js"
                        onload="renderMathInElement(document.body);"></script>
                <style>
                    body {
                        background-color: $bgColor;
                        color: $textColor;
                        font-family: 'Noto Serif Bengali', 'Times New Roman', serif, sans-serif;
                        font-size: 16px;
                        line-height: 1.8;
                        padding: 16px;
                        margin: 0;
                        word-wrap: break-word;
                    }
                    .content-card {
                        background-color: $cardBg;
                        border: 1px solid $cardBorder;
                        border-radius: 12px;
                        padding: 16px;
                        margin-bottom: 24px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.06);
                    }
                    .page-badge {
                        background-color: $badgeBg;
                        color: $badgeText;
                        font-weight: bold;
                        padding: 2px 8px;
                        border-radius: 6px;
                        font-size: 14px;
                        display: inline-block;
                        margin: 4px 2px;
                    }
                    .formula-highlight {
                        color: $formulaColor;
                        font-family: 'Cambria Math', 'Times New Roman', serif;
                        font-weight: 500;
                    }
                    .banner {
                        font-size: 12px;
                        color: #888888;
                        text-align: right;
                        margin-bottom: 8px;
                    }
                </style>
            </head>
            <body>
                <div class="banner">সমীকরণ ও সূত্র ভিউ (LaTeX & KaTeX Supported)</div>
                <div class="content-card formula-highlight">
                $safeContent
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
