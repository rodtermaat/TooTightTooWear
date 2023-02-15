package com.example.wear.tiles

import kotlinx.coroutines.delay

class CalorieCalculator {

    companion object {
        const val CALORIE_GOAL = 2000
    }

    private var calorieCount = 0f

    fun incrementCalories() {
        calorieCount += 25f
    }

    fun decrementCalories() {
        if (calorieCount != 0f) {
            calorieCount -= 25f
        }
    }

    suspend fun getCalories(): Int {
        delay(50)
        return calorieCount.toInt()
    }

    suspend fun getCalPercentage(): Float {
        delay(50)
        return getCalories().toFloat()/ CALORIE_GOAL.toFloat()
    }

    suspend fun resetCalories(): Int {
        delay(50)
        return 0
    }
}