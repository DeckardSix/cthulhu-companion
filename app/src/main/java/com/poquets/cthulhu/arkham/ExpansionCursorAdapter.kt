package com.poquets.cthulhu.arkham

import android.database.AbstractCursor

/**
 * Compatibility adapter for ExpansionCursor
 * Provides cursor interface for expansion list
 */
class ExpansionCursorAdapter(private val expansions: List<ExpansionAdapter>) : AbstractCursor() {
    
    override fun getColumnNames(): Array<String> {
        return arrayOf("_ID", "Name", "Checked")
    }
    
    override fun getCount(): Int {
        return expansions.size
    }
    
    override fun getDouble(columnIndex: Int): Double {
        return if (columnIndex == 0) {
            position.toDouble()
        } else {
            0.0
        }
    }
    
    override fun getFloat(columnIndex: Int): Float {
        return if (columnIndex == 0) {
            position.toFloat()
        } else {
            0f
        }
    }
    
    override fun getInt(columnIndex: Int): Int {
        return when (columnIndex) {
            0 -> position
            2 -> if (getExpansion().getApplied()) 1 else 0
            else -> 0
        }
    }
    
    override fun getLong(columnIndex: Int): Long {
        return if (columnIndex == 0) {
            position.toLong()
        } else {
            0L
        }
    }
    
    override fun getShort(columnIndex: Int): Short {
        return if (columnIndex == 0) {
            position.toShort()
        } else {
            0
        }
    }
    
    fun getExpansion(): ExpansionAdapter {
        return expansions[position]
    }
    
    override fun getString(columnIndex: Int): String {
        return when (columnIndex) {
            1 -> getExpansion().toString()
            else -> position.toString()
        }
    }
    
    override fun isNull(columnIndex: Int): Boolean {
        return position >= expansions.size || position < 0
    }
}

