package com.direwolf.seabattle2.objects.game

class Weapons {
    var radarUsed = false
    var airstrikeUsed = false

    fun useRadar(): Boolean {
        if (!radarUsed) {
            radarUsed = true
            return true
        }
        return false
    }

    fun useAirstrike(): Boolean {
        if (!airstrikeUsed) {
            airstrikeUsed = true
            return true
        }
        return false
    }

    fun canUseRadar(): Boolean = !radarUsed
    fun canUseAirstrike(): Boolean = !airstrikeUsed
}

