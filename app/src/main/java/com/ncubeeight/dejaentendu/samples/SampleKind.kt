package com.ncubeeight.dejaentendu.samples

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Photo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ncubeeight.dejaentendu.ui.theme.AppColors

/**
 * The three kinds of source material the Samples tab can hold. Centralizes
 * the icon/color pairing so audio, text, and image rows stay visually
 * consistent everywhere they're shown (Samples tab, Home's "Continue
 * studying", the add-sample dialog). Mirrors iOS's SampleKind.swift.
 */
enum class SampleKind(val label: String, val icon: ImageVector, val tint: Color, val tintSoft: Color) {
    AUDIO("Audio", Icons.Filled.GraphicEq, AppColors.coral, AppColors.coralSoft),
    TEXT("Text", Icons.Filled.Description, AppColors.sky, AppColors.skySoft),
    IMAGE("Image", Icons.Filled.Photo, AppColors.teal, AppColors.tealSoft),
}
