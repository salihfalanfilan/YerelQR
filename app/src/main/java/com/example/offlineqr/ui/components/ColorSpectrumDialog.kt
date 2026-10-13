package com.example.offlineqr.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offlineqr.R
import java.util.Locale

@Composable
fun ColorSpectrumDialog(
    initialColor: Long,
    title: String = "Renk Spektrumu",
    onColorSelected: (Long, String) -> Unit,
    onSaveToCustomPalette: ((String) -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    val initialInt = initialColor.toInt()
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialInt, hsv)
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by remember {
        mutableStateOf(String.format(Locale.US, "#%06X", 0xFFFFFF and initialInt))
    }

    val currentColorInt = remember(hue, saturation, value) {
        android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
    }
    val currentColor = remember(currentColorInt) { Color(currentColorInt) }
    val currentHex = remember(currentColorInt) {
        String.format(Locale.US, "#%06X", 0xFFFFFF and currentColorInt)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Saturation & Value 2D Canvas Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val sat = (offset.x / size.width).coerceIn(0f, 1f)
                                val v = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                                saturation = sat
                                value = v
                                hexInput = String.format(
                                    Locale.US, "#%06X",
                                    0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, v))
                                )
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                val v = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                                saturation = sat
                                value = v
                                hexInput = String.format(
                                    Locale.US, "#%06X",
                                    0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, v))
                                )
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val pureHueRgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                        drawRect(Color(pureHueRgb))

                        // Horizontal: White to Transparent (Saturation)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.White, Color.Transparent)
                            )
                        )

                        // Vertical: Transparent to Black (Value/Brightness)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black)
                            )
                        )

                        // Selector target ring
                        val cx = saturation * size.width
                        val cy = (1f - value) * size.height
                        drawCircle(
                            color = Color.White,
                            radius = 10.dp.toPx(),
                            center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 8.dp.toPx(),
                            center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                // 2. Hue Rainbow Spectrum Slider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val h = ((offset.x / size.width) * 360f).coerceIn(0f, 360f)
                                hue = h
                                hexInput = String.format(
                                    Locale.US, "#%06X",
                                    0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(h, saturation, value))
                                )
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val h = ((change.position.x / size.width) * 360f).coerceIn(0f, 360f)
                                hue = h
                                hexInput = String.format(
                                    Locale.US, "#%06X",
                                    0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(h, saturation, value))
                                )
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val rainbow = listOf(
                            Color.Red, Color.Yellow, Color.Green,
                            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                        drawRect(brush = Brush.horizontalGradient(colors = rainbow))

                        val thumbX = (hue / 360f) * size.width
                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx(),
                            center = Offset(thumbX, size.height / 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 9.dp.toPx(),
                            center = Offset(thumbX, size.height / 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                // 3. Current Color Preview & Hex String Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(currentColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    )

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            hexInput = input
                            if (input.matches(Regex("^#([A-Fa-f0-9]{6})$"))) {
                                try {
                                    val parsed = android.graphics.Color.parseColor(input)
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(parsed, hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                } catch (_: Exception) {}
                            }
                        },
                        label = { Text(stringResource(R.string.hex_code_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // 4. Quick Preset Color Swatches
                val presets = listOf(
                    "#000000", "#FFFFFF", "#E11D48", "#EA580C", "#F59E0B",
                    "#16A34A", "#0D9488", "#0284C7", "#2563EB", "#7C3AED", "#DB2777"
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { presetHex ->
                        val presetColor = try {
                            Color(android.graphics.Color.parseColor(presetHex))
                        } catch (e: Exception) { Color.Black }
                        val isSelected = currentHex.equals(presetHex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(presetColor)
                                .border(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape
                                )
                                .clickable {
                                    try {
                                        val parsed = android.graphics.Color.parseColor(presetHex)
                                        val hsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(parsed, hsv)
                                        hue = hsv[0]
                                        saturation = hsv[1]
                                        value = hsv[2]
                                        hexInput = presetHex
                                    } catch (_: Exception) {}
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (presetHex == "#FFFFFF") Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onSaveToCustomPalette != null) {
                    OutlinedButton(onClick = {
                        onSaveToCustomPalette(currentHex)
                        val colorLong = (currentColor.value.toLong() shr 32)
                        onColorSelected(colorLong, currentHex)
                    }) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.add_palette_and_select))
                    }
                }
                Button(onClick = {
                    val colorLong = (currentColor.value.toLong() shr 32)
                    onColorSelected(colorLong, currentHex)
                }) {
                    Text(stringResource(R.string.select_btn))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun DeleteColorConfirmDialog(
    hexColor: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(R.string.delete_color_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Text(stringResource(R.string.delete_color_msg, hexColor))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
