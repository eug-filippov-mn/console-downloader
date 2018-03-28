package com.eug.md.utils.format

import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment

data class Column(val value: Any, val colspan: Int = 1)

fun AsciiTable.addHeaders(headers: Array<String>) {
    appendRow(headers)
}

fun <T> AsciiTable.addRows(rows: List<T>, columns: (T) -> Array<Column>) {
    rows.forEach { row ->
        appendRow(columns.invoke(row))
    }
}

fun AsciiTable.addTotal(vararg columns: Column) {
    appendRow(columns)
}

private fun AsciiTable.appendRow(columns: Array<out Column>) {
    val columnsContent = mutableListOf<Any?>()
    columns.forEach {
        for (i in 1 until it.colspan) {
            columnsContent.add(null)
        }
        columnsContent.add(it.value)
    }

    appendRow(columnsContent.toTypedArray())
}

private fun AsciiTable.appendRow(columnsContent: Array<out Any?>) {
    this.addRule()
    this.addRow(*columnsContent)
}

fun AsciiTable.setAlignment(alignment: TextAlignment) {
    this.addRule()
    this.setTextAlignment(alignment)
}