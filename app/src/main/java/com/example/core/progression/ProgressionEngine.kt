package com.example.core.progression

import com.example.domain.model.AuthUser

enum class XpSource(val xpGained: Long, val displayName: String) {
    VALID_RUN(50, "Valid Run Completed"),
    DISTANCE_KM(10, "Distance Bonus (per km)"),
    NEW_TERRITORY(100, "New Territory Secured"),
    EXPANSION(25, "Territory Expanded"),
    CAPTURE(200, "Enemy Territory Captured"),
    DEFENSE(150, "Territory Defended"),
    RECORD(75, "Personal Record Beaten"),
    CHALLENGE(50, "Challenge Completed")
}

enum class Achievement(val id: String, val title: String, val description: String) {
    FIRST_TERRITORY("ACH_FIRST_TERRITORY", "First Territory", "Secured your first sector"),
    FIRST_RUN("ACH_FIRST_RUN", "First Run", "Completed your first valid run"),
    FIRST_CAPTURE("ACH_FIRST_CAPTURE", "First Capture", "Captured your first enemy territory"),
    FIRST_DEFENSE("ACH_FIRST_DEFENSE", "First Defense", "Successfully defended a territory"),
    EXPLORER("ACH_EXPLORER", "Explorer", "Covered a total distance of 10km"),
    CONQUEROR("ACH_CONQUEROR", "Conqueror", "Captured 10 territories"),
    EMPIRE("ACH_EMPIRE", "Empire", "Control a total area of 100,000 sqm")
}

class ProgressionEngine {

    fun calculateLevel(currentXp: Long): Pair<Int, Long> {
        // Simple formula for level: Level = sqrt(xp / 100) + 1
        // XP required for level N = 100 * (N-1)^2
        var level = 1
        var nextLevelXp = 100L
        while (currentXp >= nextLevelXp) {
            level++
            nextLevelXp = 100L * level * level
        }
        return Pair(level, nextLevelXp)
    }

    fun applyXp(user: AuthUser, sources: List<Pair<XpSource, Int>>): AuthUser {
        var totalXpGained = 0L
        sources.forEach { (source, multiplier) ->
            totalXpGained += source.xpGained * multiplier
        }
        
        val newXp = user.xp + totalXpGained
        val (newLevel, newNextLevelXp) = calculateLevel(newXp)
        
        return user.copy(
            xp = newXp,
            level = newLevel,
            nextLevelXp = newNextLevelXp
        )
    }

    fun checkAchievements(user: AuthUser): AuthUser {
        val unlocked = user.achievements.toMutableSet()
        
        if (user.territoriesCount > 0 && !unlocked.contains(Achievement.FIRST_TERRITORY.id)) {
            unlocked.add(Achievement.FIRST_TERRITORY.id)
        }
        if (user.totalDistanceMeters > 0 && !unlocked.contains(Achievement.FIRST_RUN.id)) {
            unlocked.add(Achievement.FIRST_RUN.id)
        }
        if (user.territoriesCapturedCount >= 1 && !unlocked.contains(Achievement.FIRST_CAPTURE.id)) {
            unlocked.add(Achievement.FIRST_CAPTURE.id)
        }
        // Assuming FIRST_DEFENSE is handled elsewhere or via an event flag
        if (user.totalDistanceMeters >= 10000 && !unlocked.contains(Achievement.EXPLORER.id)) {
            unlocked.add(Achievement.EXPLORER.id)
        }
        if (user.territoriesCapturedCount >= 10 && !unlocked.contains(Achievement.CONQUEROR.id)) {
            unlocked.add(Achievement.CONQUEROR.id)
        }
        if (user.totalAreaSqMeters >= 100000 && !unlocked.contains(Achievement.EMPIRE.id)) {
            unlocked.add(Achievement.EMPIRE.id)
        }

        return user.copy(achievements = unlocked.toList())
    }
}
