package com.example

import java.util.LinkedList
import kotlin.math.abs

class TextHistoryManager(initialText: String, initialCursor: Int = initialText.length) {

    data class TextSnapshot(
        val text: String,
        val cursorPosition: Int
    )

    private val undoStack = LinkedList<TextSnapshot>()
    private val redoStack = LinkedList<TextSnapshot>()
    private var lastEditTime: Long = 0L

    init {
        undoStack.add(TextSnapshot(initialText, initialCursor.coerceIn(0, initialText.length)))
    }

    fun recordChange(newText: String, cursorPosition: Int) {
        val current = undoStack.lastOrNull()?.text
        if (current == newText) return

        val now = System.currentTimeMillis()
        if (undoStack.isNotEmpty() && (now - lastEditTime < 350) &&
            abs(newText.length - (current?.length ?: 0)) <= 1
        ) {
            undoStack.removeLast()
        }

        undoStack.add(TextSnapshot(newText, cursorPosition.coerceIn(0, newText.length)))
        if (undoStack.size > MAX_HISTORY_SIZE) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        lastEditTime = now
    }

    val canUndo: Boolean
        get() = undoStack.size > 1

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    fun undo(): TextSnapshot? {
        if (!canUndo) return null
        val current = undoStack.removeLast()
        redoStack.add(current)
        return undoStack.last()
    }

    fun redo(): TextSnapshot? {
        if (!canRedo) return null
        val next = redoStack.removeLast()
        undoStack.add(next)
        return next
    }

    fun reset(text: String, cursorPosition: Int = text.length) {
        undoStack.clear()
        redoStack.clear()
        undoStack.add(TextSnapshot(text, cursorPosition.coerceIn(0, text.length)))
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 60
    }
}
