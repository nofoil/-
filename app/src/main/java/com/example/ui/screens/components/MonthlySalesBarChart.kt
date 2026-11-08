package com.example.ui.screens.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.MonthlySalesData
import com.example.util.UiHelpers

@Composable
fun MonthlySalesBarChart(
    monthlyData: List<MonthlySalesData>,
    modifier: Modifier = Modifier
) {
    if (monthlyData.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(monthlyData.lastIndex) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(monthlyData) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val maxSalesValue = remember(monthlyData) {
        val maxVal = monthlyData.maxOfOrNull { it.totalSales } ?: 1000.0
        if (maxVal > 0) maxVal * 1.25 else 1000.0
    }

    val selectedItem = monthlyData.getOrNull(selectedIndex) ?: monthlyData.last()

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_sales_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title & Evolution Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldDark.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = GoldLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إجمالي المبيعات الشهرية وتطورها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "رسم بياني عمودي لتتبع نمو المبيعات شهراً بشهر",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Growth badge
                selectedItem.growthPercentage?.let { growth ->
                    val isPositive = growth >= 0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPositive) EmeraldSuccess.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isPositive) EmeraldSuccess else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${if (isPositive) "+" else ""}${String.format("%.1f", growth)}%",
                                color = if (isPositive) EmeraldSuccess else MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tooltip card of the selected month
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate800,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تفاصيل ${selectedItem.monthLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = UiHelpers.formatMoney(selectedItem.totalSales),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "المحصل: ${UiHelpers.formatMoney(selectedItem.totalCollected)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${selectedItem.salesCount} عمليات بيع",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vertical Bar Chart Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val chartWidth = constraints.maxWidth.toFloat()
                val chartHeight = constraints.maxHeight.toFloat()
                val bottomLabelHeight = 50f
                val topPadding = 30f
                val availableHeight = chartHeight - bottomLabelHeight - topPadding

                val barCount = monthlyData.size
                val itemSlotWidth = chartWidth / barCount
                val barWidth = (itemSlotWidth * 0.55f).coerceIn(18f, 44f)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(monthlyData) {
                            detectTapGestures { offset ->
                                val tappedIndex = (offset.x / itemSlotWidth).toInt().coerceIn(0, monthlyData.lastIndex)
                                selectedIndex = tappedIndex
                            }
                        }
                ) {
                    // 1. Draw horizontal reference grid lines
                    val gridLinesCount = 3
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    for (i in 0..gridLinesCount) {
                        val y = topPadding + (availableHeight / gridLinesCount) * i
                        drawLine(
                            color = Color(0xFF334155),
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1f,
                            pathEffect = dashedEffect
                        )
                    }

                    // 2. Draw Vertical Bars
                    monthlyData.forEachIndexed { index, data ->
                        val isSelected = index == selectedIndex
                        val fraction = (data.totalSales / maxSalesValue).toFloat().coerceIn(0f, 1f)
                        val animatedBarHeight = (availableHeight * fraction) * animationProgress.value

                        val slotCenterX = (index * itemSlotWidth) + (itemSlotWidth / 2f)
                        val barLeft = slotCenterX - (barWidth / 2f)
                        val barTop = (topPadding + availableHeight) - animatedBarHeight
                        val barBottom = topPadding + availableHeight

                        // Bar background (shadow column)
                        drawRoundRect(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            topLeft = Offset(barLeft, topPadding),
                            size = Size(barWidth, availableHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        // Bar Gradient
                        val barBrush = if (isSelected) {
                            Brush.verticalGradient(
                                colors = listOf(GoldLight, GoldDark)
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFE2E8F0), Color(0xFF64748B))
                            )
                        }

                        // Bar shape
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, animatedBarHeight.coerceAtLeast(4f)),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        // Selected indicator border
                        if (isSelected) {
                            drawRoundRect(
                                color = GoldLight,
                                topLeft = Offset(barLeft - 2f, barTop - 2f),
                                size = Size(barWidth + 4f, animatedBarHeight + 4f),
                                cornerRadius = CornerRadius(10f, 10f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                            )
                        }

                        // Text labels: Month name and short amount
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#94A3B8")
                                textSize = 28f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = isSelected
                            }
                            drawText(
                                data.monthLabel,
                                slotCenterX,
                                chartHeight - 12f,
                                paint
                            )

                            // Floating value over selected bar
                            if (isSelected && animatedBarHeight > 10f) {
                                val valuePaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#FBBF24")
                                    textSize = 24f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                }
                                val shortValueText = "${(data.totalSales / 1000).toInt()}k"
                                drawText(
                                    shortValueText,
                                    slotCenterX,
                                    (barTop - 10f).coerceAtLeast(20f),
                                    valuePaint
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hint: Click to inspect
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "اضغط على أي عمود شهري لعرض تفاصيله ونسبة نموه",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontSize = 10.5.sp
                )
            }
        }
    }
}
