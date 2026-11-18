package com.example

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.regex.Pattern

class PdfNotesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun getSafeKey(pdfIdentifier: String?): String {
        if (pdfIdentifier == null) return "unknown_pdf"
        return KEY_PREFIX + pdfIdentifier.hashCode()
    }

    fun getNoteFontSize(): Float = prefs.getFloat("note_font_size", 12.5f)

    fun saveNoteFontSize(sp: Float) {
        prefs.edit().putFloat("note_font_size", sp).apply()
    }

    fun isTwoColumnMode(): Boolean = prefs.getBoolean("note_two_column_mode", true)

    fun setTwoColumnMode(twoCol: Boolean) {
        prefs.edit().putBoolean("note_two_column_mode", twoCol).apply()
    }

    fun getUnlimitedNoteForPdf(pdfIdentifier: String?): String {
        if (pdfIdentifier == null) return ""
        val key = KEY_UNLIMITED_PREFIX + pdfIdentifier.hashCode()
        if (prefs.contains(key)) {
            return prefs.getString(key, "") ?: ""
        }
        if (pdfIdentifier.contains("/") || pdfIdentifier.contains("\\")) {
            val name = File(pdfIdentifier).name
            val altKey = KEY_UNLIMITED_PREFIX + name.hashCode()
            if (prefs.contains(altKey)) {
                return prefs.getString(altKey, "") ?: ""
            }
        }
        return ""
    }

    fun saveUnlimitedNoteForPdf(pdfIdentifier: String?, noteText: String?) {
        if (pdfIdentifier == null) return
        val key = KEY_UNLIMITED_PREFIX + pdfIdentifier.hashCode()
        val safeText = noteText ?: ""
        prefs.edit()
            .putString(key, safeText)
            .putString("id_map_$key", pdfIdentifier)
            .apply()
        if (pdfIdentifier.contains("/") || pdfIdentifier.contains("\\")) {
            val name = File(pdfIdentifier).name
            val nameKey = KEY_UNLIMITED_PREFIX + name.hashCode()
            prefs.edit().putString(nameKey, safeText).apply()
        }
    }

    fun getLastUnlimitedPageForPdf(pdfIdentifier: String?): Int {
        if (pdfIdentifier == null) return 0
        val key = KEY_PAGE_COUNTER_PREFIX + pdfIdentifier.hashCode()
        if (prefs.contains(key)) {
            return prefs.getInt(key, 0)
        }
        if (pdfIdentifier.contains("/") || pdfIdentifier.contains("\\")) {
            val name = File(pdfIdentifier).name
            val altKey = KEY_PAGE_COUNTER_PREFIX + name.hashCode()
            if (prefs.contains(altKey)) {
                return prefs.getInt(altKey, 0)
            }
        }
        return 0
    }

    fun saveLastUnlimitedPageForPdf(pdfIdentifier: String?, pageNumber: Int) {
        if (pdfIdentifier == null) return
        val key = KEY_PAGE_COUNTER_PREFIX + pdfIdentifier.hashCode()
        prefs.edit().putInt(key, pageNumber).apply()
        if (pdfIdentifier.contains("/") || pdfIdentifier.contains("\\")) {
            val name = File(pdfIdentifier).name
            val nameKey = KEY_PAGE_COUNTER_PREFIX + name.hashCode()
            prefs.edit().putInt(nameKey, pageNumber).apply()
        }
    }

    fun getPastedPages(pdfIdentifier: String?): MutableSet<Int> {
        val result = mutableSetOf<Int>()
        if (pdfIdentifier == null) return result
        val raw = prefs.getString(KEY_PASTED_PAGES_PREFIX + pdfIdentifier.hashCode(), "") ?: ""
        if (raw.isEmpty()) return result
        for (part in raw.split(",")) {
            part.trim().toIntOrNull()?.let { result.add(it) }
        }
        return result
    }

    fun isPagePasted(pdfIdentifier: String?, pageIndex: Int): Boolean {
        return getPastedPages(pdfIdentifier).contains(pageIndex)
    }

    private fun savePastedPages(pdfIdentifier: String, pages: Set<Int>) {
        val joined = pages.joinToString(",")
        prefs.edit().putString(KEY_PASTED_PAGES_PREFIX + pdfIdentifier.hashCode(), joined).apply()
    }

    fun clearPastedPages(pdfIdentifier: String?) {
        if (pdfIdentifier == null) return
        prefs.edit()
            .remove(KEY_PASTED_PAGES_PREFIX + pdfIdentifier.hashCode())
            .remove(KEY_AGENT_LAST_PREFIX + pdfIdentifier.hashCode())
            .remove(KEY_AGENT_RUNNING_PREFIX + pdfIdentifier.hashCode())
            .apply()
    }

    fun saveAgentProgress(pdfIdentifier: String?, lastCompletedIndex: Int, running: Boolean) {
        if (pdfIdentifier == null) return
        prefs.edit()
            .putInt(KEY_AGENT_LAST_PREFIX + pdfIdentifier.hashCode(), lastCompletedIndex)
            .putBoolean(KEY_AGENT_RUNNING_PREFIX + pdfIdentifier.hashCode(), running)
            .apply()
    }

    fun wasAgentInterrupted(pdfIdentifier: String?): Boolean {
        return pdfIdentifier != null &&
            prefs.getBoolean(KEY_AGENT_RUNNING_PREFIX + pdfIdentifier.hashCode(), false)
    }

    fun getAgentLastCompleted(pdfIdentifier: String?): Int {
        if (pdfIdentifier == null) return -1
        return prefs.getInt(KEY_AGENT_LAST_PREFIX + pdfIdentifier.hashCode(), -1)
    }

    fun appendPageToUnlimitedNote(pdfIdentifier: String?, pageIndex: Int, pageText: String?): Boolean {
        if (pdfIdentifier == null || pageText == null) return false
        val clean = pageText.trim()
        if (clean.isEmpty()) return false
        synchronized(APPEND_LOCK) {
            val existing = getUnlimitedNoteForPdf(pdfIdentifier)
            val pasted = getPastedPages(pdfIdentifier)
            if (existing.trim().isEmpty() && pasted.isNotEmpty()) {
                pasted.clear()
            }
            if (pasted.contains(pageIndex)) return false

            val base = stripTrailingWhitespace(existing)
            val updated = if (base.isEmpty()) clean else base + PAGE_SEPARATOR + clean
            saveUnlimitedNoteForPdf(pdfIdentifier, updated)

            val verify = getUnlimitedNoteForPdf(pdfIdentifier)
            if (verify != updated) {
                saveUnlimitedNoteForPdf(pdfIdentifier, existing)
                return false
            }
            pasted.add(pageIndex)
            savePastedPages(pdfIdentifier, pasted)
            return true
        }
    }

    fun hasCustomNotes(pdfIdentifier: String?): Boolean {
        if (pdfIdentifier == null) return false
        val baseKey = getSafeKey(pdfIdentifier)
        if (prefs.contains("${baseKey}_col1") || prefs.contains("${baseKey}_col2") || prefs.contains(baseKey)) {
            return true
        }
        if (pdfIdentifier.contains("/") || pdfIdentifier.contains("\\")) {
            val name = File(pdfIdentifier).name
            val alt = getSafeKey(name)
            return prefs.contains("${alt}_col1") || prefs.contains("${alt}_col2") || prefs.contains(alt)
        }
        return false
    }

    fun getNoteCol1ForPdf(pdfIdentifier: String?): String {
        if (pdfIdentifier == null) return generateChapters(1, 5)
        val key = getSafeKey(pdfIdentifier) + "_col1"
        if (prefs.contains(key)) {
            return prefs.getString(key, generateChapters(1, 5)) ?: generateChapters(1, 5)
        }
        if (pdfIdentifier.contains("/") || pdfIdentifier.contains("\\")) {
            val name = File(pdfIdentifier).name
            val altKey = getSafeKey(name) + "_col1"
            if (prefs.contains(altKey)) {
                return prefs.getString(altKey, generateChapters(1, 5)) ?: generateChapters(1, 5)
            }
        }
        val legacy = getLegacyNote(pdfIdentifier)
        if (legacy != null && legacy.contains(COLUMN_SPLIT_DELIMITER)) {
            return legacy.split(Pattern.quote(COLUMN_SPLIT_DELIMITER), limit = 2)[0]
        } else if (legacy != null && legacy.trim().isNotEmpty() && legacy != DEFAULT_NOTE_TEMPLATE_CH1) {
            return legacy
        }
        return generateChapters(1, 5)
    }

    fun getNoteCol2ForPdf(pdfIdentifier: String?): String {
        if (pdfIdentifier == null) return generateChapters(6, 5)
        val key = getSafeKey(pdfIdentifier) + "_col2"
        if (prefs.contains(key)) {
            return prefs.getString(key, generateChapters(6, 5)) ?: generateChapters(6, 5)
        }
        if (pdfIdentifier.contains("/") || pdfIdentifier.contains("\\")) {
            val name = File(pdfIdentifier).name
            val altKey = getSafeKey(name) + "_col2"
            if (prefs.contains(altKey)) {
                return prefs.getString(altKey, generateChapters(6, 5)) ?: generateChapters(6, 5)
            }
        }
        val legacy = getLegacyNote(pdfIdentifier)
        if (legacy != null && legacy.contains(COLUMN_SPLIT_DELIMITER)) {
            val parts = legacy.split(Pattern.quote(COLUMN_SPLIT_DELIMITER), limit = 2)
            return if (parts.size > 1) parts[1] else ""
        }
        return generateChapters(6, 5)
    }

    private fun getLegacyNote(pdfIdentifier: String): String? {
        return prefs.getString(getSafeKey(pdfIdentifier), null)
    }

    fun getNoteForPdf(pdfIdentifier: String?): String {
        val key = getSafeKey(pdfIdentifier)
        if (prefs.contains(key)) {
            return prefs.getString(key, generateDefault10Chapters()) ?: generateDefault10Chapters()
        }
        val c1 = prefs.getString("${key}_col1", null)
        val c2 = prefs.getString("${key}_col2", null)
        if (c1 != null || c2 != null) {
            return "${c1 ?: ""}\n\n${c2 ?: ""}"
        }
        return generateDefault10Chapters()
    }

    fun saveNoteForPdf(pdfIdentifier: String?, noteText: String?) {
        val key = getSafeKey(pdfIdentifier)
        prefs.edit()
            .putString(key, noteText)
            .putString("id_map_$key", pdfIdentifier ?: "")
            .apply()
    }

    fun saveTwoColumnNotes(pdfIdentifier: String?, col1: String?, col2: String?) {
        val baseKey = getSafeKey(pdfIdentifier)
        val safeCol1 = col1 ?: ""
        val safeCol2 = col2 ?: ""
        prefs.edit()
            .putString("${baseKey}_col1", safeCol1)
            .putString("${baseKey}_col2", safeCol2)
            .putString(baseKey, safeCol1 + COLUMN_SPLIT_DELIMITER + safeCol2)
            .putString("id_map_$baseKey", pdfIdentifier ?: "")
            .apply()
        if (pdfIdentifier != null && (pdfIdentifier.contains("/") || pdfIdentifier.contains("\\"))) {
            val name = File(pdfIdentifier).name
            val nameKey = getSafeKey(name)
            prefs.edit()
                .putString("${nameKey}_col1", safeCol1)
                .putString("${nameKey}_col2", safeCol2)
                .putString(nameKey, safeCol1 + COLUMN_SPLIT_DELIMITER + safeCol2)
                .apply()
        }
    }

    fun clearNoteForPdf(pdfIdentifier: String?) {
        val baseKey = getSafeKey(pdfIdentifier)
        prefs.edit()
            .remove(baseKey)
            .remove("${baseKey}_col1")
            .remove("${baseKey}_col2")
            .remove("id_map_$baseKey")
            .apply()
    }

    fun exportAllNotesAsJson(): JSONArray {
        val notesArray = JSONArray()
        try {
            val all = prefs.all
            val processedBases = hashSetOf<String>()
            for (key in all.keys) {
                if (key.startsWith(KEY_PREFIX) && !key.endsWith("_col1") && !key.endsWith("_col2")) {
                    if (processedBases.add(key)) {
                        val noteObj = JSONObject()
                        noteObj.put("baseKey", key)
                        val identifier = prefs.getString("id_map_$key", "") ?: ""
                        noteObj.put("pdfIdentifier", identifier)
                        noteObj.put("col1", prefs.getString("${key}_col1", "") ?: "")
                        noteObj.put("col2", prefs.getString("${key}_col2", "") ?: "")
                        noteObj.put("notes", prefs.getString(key, "") ?: "")
                        if (identifier.isNotEmpty()) {
                            val unlim = getUnlimitedNoteForPdf(identifier)
                            if (unlim.isNotEmpty()) noteObj.put("unlimited_note", unlim)
                            val pageCounter = getLastUnlimitedPageForPdf(identifier)
                            if (pageCounter > 0) noteObj.put("last_page_counter", pageCounter)
                        }
                        notesArray.put(noteObj)
                    }
                } else if (key.startsWith(KEY_UNLIMITED_PREFIX)) {
                    val identifier = prefs.getString("id_map_$key", "") ?: ""
                    val baseKey = getSafeKey(identifier)
                    if (processedBases.add(baseKey)) {
                        val noteObj = JSONObject()
                        noteObj.put("baseKey", baseKey)
                        noteObj.put("pdfIdentifier", identifier)
                        noteObj.put("unlimited_note", prefs.getString(key, "") ?: "")
                        if (identifier.isNotEmpty()) {
                            val pageCounter = getLastUnlimitedPageForPdf(identifier)
                            if (pageCounter > 0) noteObj.put("last_page_counter", pageCounter)
                        }
                        notesArray.put(noteObj)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return notesArray
    }

    fun restoreNotesFromJson(notesArray: JSONArray?) {
        if (notesArray == null) return
        val editor = prefs.edit()
        for (i in 0 until notesArray.length()) {
            try {
                val noteObj = notesArray.getJSONObject(i)
                var baseKey = noteObj.optString("baseKey", "")
                val pdfId = noteObj.optString("pdfIdentifier", "")
                val col1 = noteObj.optString("col1", "")
                val col2 = noteObj.optString("col2", "")
                val combined = noteObj.optString("notes", "")
                val unlim = noteObj.optString("unlimited_note", "")
                val lastPage = noteObj.optInt("last_page_counter", 0)

                if (baseKey.isEmpty() && pdfId.isNotEmpty()) {
                    baseKey = getSafeKey(pdfId)
                }

                if (baseKey.isNotEmpty()) {
                    if (col1.isNotEmpty()) editor.putString("${baseKey}_col1", col1)
                    if (col2.isNotEmpty()) editor.putString("${baseKey}_col2", col2)
                    if (combined.isNotEmpty()) {
                        editor.putString(baseKey, combined)
                    } else if (col1.isNotEmpty() || col2.isNotEmpty()) {
                        editor.putString(baseKey, col1 + COLUMN_SPLIT_DELIMITER + col2)
                    }
                    if (pdfId.isNotEmpty()) {
                        editor.putString("id_map_$baseKey", pdfId)
                        val altKey = getSafeKey(pdfId)
                        if (altKey != baseKey) {
                            if (col1.isNotEmpty()) editor.putString("${altKey}_col1", col1)
                            if (col2.isNotEmpty()) editor.putString("${altKey}_col2", col2)
                            editor.putString(altKey, col1 + COLUMN_SPLIT_DELIMITER + col2)
                        }
                        if (pdfId.contains("/") || pdfId.contains("\\")) {
                            val fName = File(pdfId).name
                            val nameKey = getSafeKey(fName)
                            if (col1.isNotEmpty()) editor.putString("${nameKey}_col1", col1)
                            if (col2.isNotEmpty()) editor.putString("${nameKey}_col2", col2)
                            editor.putString(nameKey, col1 + COLUMN_SPLIT_DELIMITER + col2)
                        }
                        if (unlim.isNotEmpty()) saveUnlimitedNoteForPdf(pdfId, unlim)
                        if (lastPage > 0) saveLastUnlimitedPageForPdf(pdfId, lastPage)
                    }
                }
            } catch (_: Exception) {
            }
        }
        editor.apply()
    }

    companion object {
        private const val PREF_NAME = "pdf_user_notes_prefs"
        private const val KEY_PREFIX = "note_"
        private const val KEY_UNLIMITED_PREFIX = "unlimited_note_"
        private const val KEY_PAGE_COUNTER_PREFIX = "page_counter_"
        const val COLUMN_SPLIT_DELIMITER = "\n===COLUMN_BREAK===\n"

        const val PAGE_SEPARATOR = "\n\n\n"
        private val APPEND_LOCK = Any()
        private const val KEY_PASTED_PAGES_PREFIX = "lens_pasted_pages_"
        private const val KEY_AGENT_LAST_PREFIX = "lens_agent_last_"
        private const val KEY_AGENT_RUNNING_PREFIX = "lens_agent_running_"

        const val DEFAULT_NOTE_TEMPLATE_CH1 =
            "অধ্যায়:০১ name= here type  পৃষ্ঠা= Type\n" +
                "ক প্রশ্ন: পৃষ্ঠা= Type\n" +
                "খ প্রশ্ন : পৃষ্ঠা= Type\n" +
                "সৃজনশীল প্রশ্ন : পৃষ্ঠা= Type\n" +
                "Extra পৃষ্ঠা= Type"

        private fun stripTrailingWhitespace(s: String): String {
            var end = s.length
            while (end > 0 && Character.isWhitespace(s[end - 1])) end--
            return s.substring(0, end)
        }

        fun generateChapterBlock(chapterNumber: Int): String {
            val bnNum = toBengaliDigits(chapterNumber)
            return "অধ্যায়:$bnNum name= here type  পৃষ্ঠা= Type\n" +
                "ক প্রশ্ন: পৃষ্ঠা= Type\n" +
                "খ প্রশ্ন : পৃষ্ঠা= Type\n" +
                "সৃজনশীল প্রশ্ন : পৃষ্ঠা= Type\n" +
                "Extra পৃষ্ঠা= Type"
        }

        fun generateChapters(startChapter: Int, count: Int): String {
            val sb = StringBuilder()
            for (i in startChapter until startChapter + count) {
                if (sb.isNotEmpty()) sb.append("\n\n")
                sb.append(generateChapterBlock(i))
            }
            return sb.toString()
        }

        fun generateDefault10Chapters(): String = generateChapters(1, 10)

        fun generateChapterTemplate(chapterNumber: Int): String =
            "\n\n" + generateChapterBlock(chapterNumber)

        fun formatAgentPageText(displayPageNumber: Int, rawText: String): String {
            val bnNum = toBengaliDigits(displayPageNumber)
            return "[ পৃষ্ঠা : $bnNum ]\n${rawText.trim()}"
        }

        fun toBengaliDigits(number: Int): String {
            val numStr = String.format(Locale.US, "%02d", number)
            return PdfUtils.toBengaliDigits(numStr)
        }
    }
}
