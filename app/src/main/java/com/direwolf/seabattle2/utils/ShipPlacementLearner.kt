package com.direwolf.seabattle2.utils

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.max

class ShipPlacementLearner(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ship_placements", Context.MODE_PRIVATE)
    
    fun recordPlayerPlacement(ships: Array<Int>) {
        // Record where player placed ships
        val key = "placement_${System.currentTimeMillis() % 10000}"
        val placement = ships.joinToString(",")
        prefs.edit().putString(key, placement).apply()
        
        // Keep only last 10 placements
        val allKeys = prefs.all.keys.filter { it.startsWith("placement_") }
        if (allKeys.size > 10) {
            val sortedKeys = allKeys.sorted()
            for (i in 0 until (allKeys.size - 10)) {
                prefs.edit().remove(sortedKeys[i]).apply()
            }
        }
    }
    
    fun getPredictedHotspots(): Array<Array<Int>> {
        val hotspots = Array(10) { Array(10) { 0 } }
        val allPlacements = prefs.all.values.filterIsInstance<String>()
        
        for (placement in allPlacements) {
            val ships = placement.split(",").map { it.toInt() }
            // Parse ship positions and mark hotspots
            for (i in 0 until ships.size step 4) {
                if (i + 3 < ships.size) {
                    val x = ships[i]
                    val y = ships[i + 1]
                    if (x in 0..9 && y in 0..9) {
                        hotspots[x][y] += 1
                        // Also mark adjacent cells
                        for (dx in -1..1) {
                            for (dy in -1..1) {
                                val nx = x + dx
                                val ny = y + dy
                                if (nx in 0..9 && ny in 0..9) {
                                    hotspots[nx][ny] += 1
                                }
                            }
                        }
                    }
                }
            }
        }
        return hotspots
    }
}

