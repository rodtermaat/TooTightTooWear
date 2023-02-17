/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.wear.tiles

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
import androidx.wear.tiles.DimensionBuilders.degrees
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.DimensionBuilders.expand
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.LayoutElementBuilders.ARC_ANCHOR_START
import androidx.wear.tiles.LayoutElementBuilders.Arc
import androidx.wear.tiles.LayoutElementBuilders.ArcLine
import androidx.wear.tiles.LayoutElementBuilders.Box
import androidx.wear.tiles.LayoutElementBuilders.Column
import androidx.wear.tiles.LayoutElementBuilders.Row
import androidx.wear.tiles.LayoutElementBuilders.FontStyles
import androidx.wear.tiles.LayoutElementBuilders.Image
import androidx.wear.tiles.LayoutElementBuilders.Layout
import androidx.wear.tiles.LayoutElementBuilders.Spacer
import androidx.wear.tiles.LayoutElementBuilders.Text
import androidx.wear.tiles.ModifiersBuilders.Background
import androidx.wear.tiles.ModifiersBuilders.Clickable
import androidx.wear.tiles.ModifiersBuilders.Corner
import androidx.wear.tiles.ModifiersBuilders.Modifiers
import androidx.wear.tiles.ModifiersBuilders.Padding
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.ResourceBuilders.AndroidImageResourceByResId
import androidx.wear.tiles.ResourceBuilders.ImageResource
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
//import android.util.Log
//import androidx.core.content.ContentProviderCompat.requireContext
//import kotlinx.coroutines.delay
import android.content.SharedPreferences


private const val RESOURCES_VERSION = "1"

// dimensions
private val PROGRESS_BAR_THICKNESS = dp(6f)
private val BUTTON_SIZE = dp(48f)
private val BUTTON_RADIUS = dp(24f)
private val BUTTON_PADDING = dp(0f) //4
// used for the reset button
private val BABY_BUTTON_SIZE = dp(24f)
private val BABY_BUTTON_RADIUS = dp(12f)
private val BABY_BUTTON_PADDING = dp(0f) //4

private val VERTICAL_SPACING_HEIGHT = dp(8f)
private val HORIZONTAL_SPACING_WIDTH = dp(16f)

private const val ARC_TOTAL_DEGREES = 360f

private const val ID_IMAGE_ADD_CALS = "image_add_cals"
private const val ID_IMAGE_SUB_CALS = "image_sub_cals"
private const val ID_IMAGE_RESET_CALS = "image_reset_cals"


var globalCalCount = 0f
var globalCalGoal = 2000f

class CalorieTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onTileRequest(requestParams: TileRequest) = serviceScope.future {

        val deviceParameters = requestParams.deviceParameters!!

        globalCalCount = loadData()

        //println(requestParams.state?.lastClickableId.toString())
        //Log.d("WTF",requestParams.state?.lastClickableId.toString() )


        when(requestParams.state?.lastClickableId){
            ID_IMAGE_ADD_CALS -> addCalories()
            ID_IMAGE_SUB_CALS -> minusCalories()
            ID_IMAGE_RESET_CALS -> resetCalories()
        }

        Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)

            .setTimeline(
                Timeline.Builder().addTimelineEntry(
                    TimelineEntry.Builder().setLayout(
                        Layout.Builder().setRoot(
                            layout(globalCalCount.toInt(), getCalPercent(), deviceParameters)
                    ).build()
                    ).build()
                ).build()
            ).build()
    }

    override fun onResourcesRequest(requestParams: ResourcesRequest) = serviceScope.future {
        Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .addIdToImageMapping(
                ID_IMAGE_SUB_CALS,
                ImageResource.Builder()
                    .setAndroidResourceByResId(
                        AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.baseline_do_not_disturb_on_24)
                            .build()
                    )
                    .build()
            )
            .addIdToImageMapping(ID_IMAGE_ADD_CALS,
                ImageResource.Builder()
                    .setAndroidResourceByResId(
                        AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.baseline_add_circle_24)
                            .build()
                    )
                    .build())
            .addIdToImageMapping(ID_IMAGE_RESET_CALS,
                ImageResource.Builder()
                    .setAndroidResourceByResId(
                        AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.baseline_undo_12)
                            .build()
                    )
                    .build())
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleans up the coroutine
        serviceScope.cancel()
    }

    //private fun layout(goalProgress: GoalProgress, calorieCount: Int, caloriePercent: Float, deviceParameters: DeviceParameters) =
    private fun layout(calorieCount: Int, caloriePercent: Float, deviceParameters: DeviceParameters) =
        Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent((progressArc(caloriePercent)))
            .addContent(
                Column.Builder()
                    .addContent((resetCalsButton()))
                    .addContent(currentCalorieText(calorieCount.toString(), deviceParameters))
                    .addContent(goalCaloriesText(resources.getString(R.string.goalCals), deviceParameters))
                    .addContent(Spacer.Builder().setHeight((VERTICAL_SPACING_HEIGHT)).build())
                    .addContent(Row.Builder()
                        .addContent (addButton(ID_IMAGE_SUB_CALS))
                        .addContent (Spacer.Builder().setWidth((HORIZONTAL_SPACING_WIDTH)).build())
                        .addContent(addButton(ID_IMAGE_ADD_CALS)).build()).build()
            )
            .build()

    // TODO: How to change color of the arc over 1 for when you go over cal goal.
    private fun progressArc(percentage: Float) = Arc.Builder()
        .addContent(
            ArcLine.Builder()
                .setLength(degrees(percentage * ARC_TOTAL_DEGREES))
                .setColor(argb(ContextCompat.getColor(this, R.color.primaryDark)))
                .setThickness(PROGRESS_BAR_THICKNESS)
                .build()
        )
        .setAnchorAngle(degrees(0.0f))
        .setAnchorType(ARC_ANCHOR_START)
        .build()

    private fun currentCalorieText(current: String, deviceParameters: DeviceParameters) =
        LayoutElementBuilders.Text.Builder()
            .setText(current)
            .setFontStyle(FontStyles.display2(deviceParameters).build())
            .build()

    private fun goalCaloriesText(goal: String, deviceParameters: DeviceParameters) = Text.Builder()
        .setText(goal)
        .setFontStyle(FontStyles.title3(deviceParameters).build())
        .build()

    private fun resetCalsButton()=
        Image.Builder()
            .setWidth(BABY_BUTTON_SIZE)
            .setHeight(BABY_BUTTON_SIZE)
            .setResourceId(ID_IMAGE_RESET_CALS)
            .setModifiers(
                Modifiers.Builder()
                    .setPadding(
                        Padding.Builder()
                            .setStart(BABY_BUTTON_PADDING)
                            .setEnd(BABY_BUTTON_PADDING)
                            .setTop(BABY_BUTTON_PADDING)
                            .setBottom(BABY_BUTTON_PADDING)
                            .build()
                    )
                    .setBackground(
                        Background.Builder()
                            .setCorner(Corner.Builder().setRadius(BABY_BUTTON_RADIUS).build())
                            .setColor(argb(ContextCompat.getColor(this, R.color.primary)))
                            .build()
                    )
                    .setClickable(
                        Clickable.Builder()
                            .setId(ID_IMAGE_RESET_CALS)
                            // forces a reload of the tile from onTileRequest being called
                            // the other is called LaunchAction as takes you to something new
                            //.setOnClick(calorieCalculator.resetCalories())
                            .setOnClick(ActionBuilders.LoadAction.Builder().build())
                            .build()
                    )
                    .build()
            )
            .build()

    private fun addButton(theButtonID: String) =
        Image.Builder()
            .setWidth(BUTTON_SIZE)
            .setHeight(BUTTON_SIZE)
            .setResourceId(theButtonID)
            .setModifiers(
                Modifiers.Builder()
                    .setPadding(
                        Padding.Builder()
                            .setStart(BUTTON_PADDING)
                            .setEnd(BUTTON_PADDING)
                            .setTop(BUTTON_PADDING)
                            .setBottom(BUTTON_PADDING)
                            .build()
                    )
                    .setBackground(
                        Background.Builder()
                            .setCorner(Corner.Builder().setRadius(BUTTON_RADIUS).build())
                            .setColor(argb(ContextCompat.getColor(this, R.color.primaryDark)))
                            .build()
                    )
                    .setClickable(
                        Clickable.Builder()
                            .setId(theButtonID)
                            .setOnClick(ActionBuilders.LoadAction.Builder().build())
                            .build()
                    )
                    .build()
            )
            .build()


    private fun addCalories() {
        globalCalCount += 50f
        saveData(globalCalCount)
    }

    private fun minusCalories() {
        if (globalCalCount != 0f) {
            globalCalCount -= 50f
            saveData(globalCalCount)
        }
    }

    private fun getCalPercent(): Float {
        return globalCalCount/ globalCalGoal
    }

    private fun resetCalories() {
        globalCalCount = 0f
        saveData(globalCalCount)
    }

    private fun saveData(theData: Float) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val editor:SharedPreferences.Editor = sharedPreferences.edit()
        editor.apply(){
            putFloat("CAL_KEY", theData)
        }.apply()
    }

    private fun loadData(): Float {
        val sharedPreferences: SharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val savedCals : Float = sharedPreferences.getFloat("CAL_KEY", 0f)
        return savedCals
    }

}


//val openAppElement = LayoutElementBuilders.Image.Builder()
//    .setResourceId()
//    .setModifiers(
//        ModifiersBuilders.Modifiers.Builder() .setClickable(
//            ModifiersBuilders.Clickable.Builder()
//                .setId()
//                .setOnClick(
//                    ActionBuilders.LaunchAction.Builder()
//                        .setAndroidActivity(
//                            ActionBuilders.AndroidActivity.Builder()
//                                .setClassName()
//                                .setPackageName()
//                                .build()
//                        ).build()
//                ).build()
//        ).build()
//    ).build()