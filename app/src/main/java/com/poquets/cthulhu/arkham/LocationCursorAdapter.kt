package com.poquets.cthulhu.arkham

import android.database.AbstractCursor

/**
 * Compatibility adapter for LocationCursor
 * Provides cursor interface for location list (pairs in rows)
 */
class LocationCursorAdapter(private val locations: List<LocationAdapter>) : AbstractCursor() {
    
    override fun getColumnNames(): Array<String> {
        return arrayOf("_ID", "Left", "Right")
    }
    
    override fun getCount(): Int {
        return (locations.size + 1) / 2
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
        return if (columnIndex == 0) {
            position
        } else {
            0
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
    
    fun getLocation(columnIdx: Int): LocationAdapter? {
        val pos = position
        return when (columnIdx) {
            1 -> {
                val index = pos * 2
                if (index < locations.size) locations[index] else null
            }
            2 -> {
                val index = pos * 2 + 1
                if (index < locations.size) locations[index] else null
            }
            else -> null
        }
    }
    
    override fun getString(columnIndex: Int): String {
        return when (columnIndex) {
            1 -> {
                val index = position * 2
                if (index < locations.size) locations[index].toString() else ""
            }
            2 -> {
                val index = position * 2 + 1
                if (index < locations.size) locations[index].toString() else ""
            }
            else -> position.toString()
        }
    }
    
    override fun isNull(columnIndex: Int): Boolean {
        return when (columnIndex) {
            1 -> {
                val index = position * 2
                index >= locations.size
            }
            2 -> {
                val index = position * 2 + 1
                index >= locations.size
            }
            else -> false
        }
    }
}

