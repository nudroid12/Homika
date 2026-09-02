package com.homiq.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.domain.AnalyticsDashboard
import com.homiq.app.domain.AnalyticsTrendPoint
import com.homiq.app.domain.PropertyPerformance
import com.homiq.app.ui.components.DateField
import com.homiq.app.ui.components.EmptyStateCard
import com.homiq.app.ui.components.InfoCard
import com.homiq.app.ui.components.MetricCard
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.components.SelectionField
import com.homiq.app.ui.util.formatEpochDay
import com.homiq.app.ui.util.formatPercent
import com.homiq.app.ui.util.formatSenAsRinggit
import com.homiq.app.ui.viewmodel.AnalyticsRangePreset
import com.homiq.app.ui.viewmodel.ReportsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    val dashboard = state.dashboard

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 14.dp,
            end = 16.dp,
            bottom = 34.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.homika_analytics_title),
                subtitle = stringResource(R.string.homika_analytics_subtitle),
                compact = true,
            )
        }

        item {
            AnalyticsFilters(
                properties = state.properties,
                selectedPropertyId = state.selectedPropertyId,
                preset = state.preset,
                customStartEpochDay = state.customStartEpochDay,
                customEndEpochDay = state.customEndEpochDay,
                onPropertySelected = viewModel::selectProperty,
                onPresetSelected = viewModel::selectPreset,
                onCustomStartSelected = viewModel::setCustomStart,
                onCustomEndSelected = viewModel::setCustomEnd,
            )
        }

        if (state.properties.isEmpty()) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.homika_analytics_no_properties_title),
                    body = stringResource(R.string.homika_analytics_no_properties_body),
                    icon = Icons.Outlined.QueryStats,
                )
            }
        } else if (dashboard != null) {
            item {
                Text(
                    text = formatPeriodLabel(dashboard, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                AnalyticsKpiGrid(
                    dashboard = dashboard,
                    locale = locale,
                )
            }

            item {
                RevenueProfitChartCard(
                    points = dashboard.trend,
                    locale = locale,
                )
            }

            item {
                OccupancyChartCard(
                    points = dashboard.trend,
                    bookedNights = dashboard.current.bookedNights,
                    availableNights = dashboard.current.availableNights,
                    locale = locale,
                )
            }

            if (
                state.selectedPropertyId == null &&
                dashboard.propertyPerformance.size > 1
            ) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.homika_analytics_property_performance,
                            ),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(
                                R.string.homika_analytics_property_performance_body,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                items(
                    items = dashboard.propertyPerformance,
                    key = { it.propertyId },
                ) { performance ->
                    PropertyPerformanceRow(
                        performance = performance,
                        locale = locale,
                    )
                }

                item {
                    InfoCard(
                        title = stringResource(R.string.homika_analytics_expenses),
                        body = stringResource(
                            R.string.homika_analytics_unassigned_expenses_note,
                        ),
                    )
                }
            }

            item {
                BusinessInsightsCard(
                    dashboard = dashboard,
                    locale = locale,
                )
            }

            item {
                Button(
                    onClick = {
                        val periodLabel = formatPeriodLabel(dashboard, locale)
                        val shareText = buildAnalyticsShareText(
                            dashboard = dashboard,
                            periodLabel = periodLabel,
                            locale = locale,
                            revenueLabel = context.getString(
                                R.string.homika_analytics_revenue,
                            ),
                            profitLabel = context.getString(
                                R.string.homika_analytics_net_profit,
                            ),
                            occupancyLabel = context.getString(
                                R.string.homika_analytics_occupancy,
                            ),
                            bookingLabel = context.getString(
                                R.string.homika_analytics_bookings,
                            ),
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                context.getString(
                                    R.string.homika_analytics_share_subject,
                                    periodLabel,
                                ),
                            )
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(
                            Intent.createChooser(
                                intent,
                                context.getString(R.string.homika_analytics_share),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.homika_analytics_share),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsFilters(
    properties: List<PropertyEntity>,
    selectedPropertyId: String?,
    preset: AnalyticsRangePreset,
    customStartEpochDay: Long,
    customEndEpochDay: Long,
    onPropertySelected: (String?) -> Unit,
    onPresetSelected: (AnalyticsRangePreset) -> Unit,
    onCustomStartSelected: (Long) -> Unit,
    onCustomEndSelected: (Long) -> Unit,
) {
    val options = listOf<PropertyEntity?>(null) + properties
    val selectedProperty = properties.firstOrNull { it.id == selectedPropertyId }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SelectionField(
                label = stringResource(R.string.homika_analytics_property),
                selectedText = selectedProperty?.name
                    ?: stringResource(R.string.homika_analytics_all_properties),
                options = options,
                optionText = { property ->
                    property?.name
                        ?: stringResource(R.string.homika_analytics_all_properties)
                },
                onSelected = { property ->
                    onPropertySelected(property?.id)
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnalyticsRangePreset.entries.forEach { option ->
                    FilterChip(
                        selected = preset == option,
                        onClick = { onPresetSelected(option) },
                        label = {
                            Text(
                                text = stringResource(option.labelRes()),
                                maxLines = 1,
                            )
                        },
                    )
                }
            }

            if (preset == AnalyticsRangePreset.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DateField(
                        label = stringResource(R.string.homika_analytics_from),
                        epochDay = customStartEpochDay,
                        onDateSelected = onCustomStartSelected,
                        modifier = Modifier.weight(1f),
                    )
                    DateField(
                        label = stringResource(R.string.homika_analytics_to),
                        epochDay = customEndEpochDay,
                        onDateSelected = onCustomEndSelected,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKpiGrid(
    dashboard: AnalyticsDashboard,
    locale: Locale,
) {
    val current = dashboard.current
    val comparison = dashboard.comparison

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricCard(
                label = stringResource(R.string.homika_analytics_revenue),
                value = formatSenAsRinggit(current.revenueSen, locale),
                supportingText = comparison.revenuePercentChange
                    ?.let {
                        stringResource(
                            R.string.homika_analytics_vs_previous,
                            signedPercent(it, locale),
                        )
                    }
                    ?: stringResource(R.string.homika_analytics_no_previous),
                compact = true,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = stringResource(R.string.homika_analytics_net_profit),
                value = formatSenAsRinggit(current.netProfitSen, locale),
                supportingText = comparison.netProfitPercentChange
                    ?.let {
                        stringResource(
                            R.string.homika_analytics_vs_previous,
                            signedPercent(it, locale),
                        )
                    }
                    ?: stringResource(R.string.homika_analytics_no_previous),
                compact = true,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricCard(
                label = stringResource(R.string.homika_analytics_occupancy),
                value = formatPercent(current.occupancyPercent, locale),
                supportingText = stringResource(
                    R.string.homika_analytics_pp_change,
                    signedNumber(comparison.occupancyPointChange, locale),
                ),
                compact = true,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = stringResource(R.string.homika_analytics_bookings),
                value = current.bookingCount.toString(),
                supportingText = stringResource(
                    R.string.homika_analytics_booking_delta,
                    signedInt(comparison.bookingCountDelta),
                ),
                compact = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RevenueProfitChartCard(
    points: List<AnalyticsTrendPoint>,
    locale: Locale,
) {
    val revenueColor = MaterialTheme.colorScheme.primary
    val profitColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.homika_analytics_revenue_profit_trend),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.homika_analytics_revenue_profit_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChartLegendDot(
                    color = revenueColor,
                    label = stringResource(R.string.homika_analytics_revenue),
                )
                ChartLegendDot(
                    color = profitColor,
                    label = stringResource(R.string.homika_analytics_net_profit),
                )
            }

            MoneyLineChart(
                points = points,
                locale = locale,
                revenueColor = revenueColor,
                profitColor = profitColor,
                gridColor = gridColor,
            )
        }
    }
}

@Composable
private fun MoneyLineChart(
    points: List<AnalyticsTrendPoint>,
    locale: Locale,
    revenueColor: androidx.compose.ui.graphics.Color,
    profitColor: androidx.compose.ui.graphics.Color,
    gridColor: androidx.compose.ui.graphics.Color,
) {
    val values = points.flatMap { listOf(it.revenueSen, it.netProfitSen) }
    var minValue = min(0L, values.minOrNull() ?: 0L)
    var maxValue = max(0L, values.maxOrNull() ?: 0L)
    if (maxValue == minValue) {
        maxValue = if (maxValue == 0L) 100L else maxValue + 100L
        minValue = min(minValue, 0L)
    }
    val middleValue = minValue + (maxValue - minValue) / 2L

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .width(74.dp)
                .height(176.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = formatSenAsRinggit(maxValue, locale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatSenAsRinggit(middleValue, locale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatSenAsRinggit(minValue, locale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
            ) {
                val range = (maxValue - minValue).toFloat().coerceAtLeast(1f)
                repeat(3) { index ->
                    val y = size.height * index.toFloat() / 2f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                fun x(index: Int): Float = if (points.size <= 1) {
                    size.width / 2f
                } else {
                    size.width * index.toFloat() / (points.size - 1).toFloat()
                }

                fun y(value: Long): Float {
                    val normalized = (value - minValue).toFloat() / range
                    return size.height - normalized * size.height
                }

                if (points.isNotEmpty()) {
                    val revenuePath = Path()
                    val profitPath = Path()
                    points.forEachIndexed { index, point ->
                        val px = x(index)
                        val revenueY = y(point.revenueSen)
                        val profitY = y(point.netProfitSen)
                        if (index == 0) {
                            revenuePath.moveTo(px, revenueY)
                            profitPath.moveTo(px, profitY)
                        } else {
                            revenuePath.lineTo(px, revenueY)
                            profitPath.lineTo(px, profitY)
                        }
                    }
                    drawPath(
                        path = revenuePath,
                        color = revenueColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                        ),
                    )
                    drawPath(
                        path = profitPath,
                        color = profitColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                        ),
                    )
                    if (points.size <= 12) {
                        points.forEachIndexed { index, point ->
                            drawCircle(
                                color = revenueColor,
                                radius = 3.dp.toPx(),
                                center = Offset(x(index), y(point.revenueSen)),
                            )
                            drawCircle(
                                color = profitColor,
                                radius = 3.dp.toPx(),
                                center = Offset(x(index), y(point.netProfitSen)),
                            )
                        }
                    }
                }
            }

            TrendAxisLabels(points = points, locale = locale)
        }
    }
}

@Composable
private fun OccupancyChartCard(
    points: List<AnalyticsTrendPoint>,
    bookedNights: Long,
    availableNights: Long,
    locale: Locale,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.homika_analytics_occupancy_trend),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.homika_analytics_occupancy_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.homika_analytics_booked_available,
                        bookedNights,
                        availableNights,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .height(154.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text("100%", style = MaterialTheme.typography.labelSmall)
                    Text("50%", style = MaterialTheme.typography.labelSmall)
                    Text("0%", style = MaterialTheme.typography.labelSmall)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(154.dp),
                    ) {
                        repeat(3) { index ->
                            val y = size.height * index.toFloat() / 2f
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }

                        if (points.isNotEmpty()) {
                            val slotWidth = size.width / points.size.toFloat()
                            val maxBarWidth = 18.dp.toPx()
                            val barWidth = min(maxBarWidth, slotWidth * 0.62f)
                            points.forEachIndexed { index, point ->
                                val percentage = point.occupancyPercent
                                    .coerceIn(0.0, 100.0)
                                    .toFloat()
                                val barHeight = size.height * percentage / 100f
                                val left = slotWidth * index + (slotWidth - barWidth) / 2f
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(left, size.height - barHeight),
                                    size = androidx.compose.ui.geometry.Size(
                                        barWidth,
                                        barHeight,
                                    ),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                        4.dp.toPx(),
                                        4.dp.toPx(),
                                    ),
                                )
                            }
                        }
                    }

                    TrendAxisLabels(points = points, locale = locale)
                }
            }
        }
    }
}

@Composable
private fun TrendAxisLabels(
    points: List<AnalyticsTrendPoint>,
    locale: Locale,
) {
    if (points.isEmpty()) return
    val indices = when {
        points.size <= 6 -> points.indices.toList()
        points.size == 7 -> listOf(0, 2, 4, 6)
        else -> listOf(0, points.lastIndex / 2, points.lastIndex)
    }.distinct()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        indices.forEach { index ->
            Text(
                text = trendLabel(points[index], locale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ChartLegendDot(
    color: androidx.compose.ui.graphics.Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.size(8.dp),
            shape = MaterialTheme.shapes.small,
            color = color,
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PropertyPerformanceRow(
    performance: PropertyPerformance,
    locale: Locale,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = performance.propertyName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    R.string.homika_analytics_property_row,
                    formatSenAsRinggit(performance.revenueSen, locale),
                    formatSenAsRinggit(performance.netProfitSen, locale),
                    formatPercent(performance.occupancyPercent, locale),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BusinessInsightsCard(
    dashboard: AnalyticsDashboard,
    locale: Locale,
) {
    val current = dashboard.current
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.homika_analytics_business_insights),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            AnalyticsLine(
                label = stringResource(R.string.homika_analytics_average_booking),
                value = formatSenAsRinggit(current.averageBookingValueSen, locale),
            )
            AnalyticsLine(
                label = stringResource(R.string.homika_analytics_average_stay),
                value = stringResource(
                    R.string.homika_analytics_nights_value,
                    current.averageStayNights,
                ),
            )
            AnalyticsLine(
                label = stringResource(
                    R.string.homika_analytics_revenue_per_available_night,
                ),
                value = formatSenAsRinggit(
                    current.revenuePerAvailableNightSen,
                    locale,
                ),
            )
            AnalyticsLine(
                label = stringResource(R.string.homika_analytics_expenses),
                value = formatSenAsRinggit(current.expensesSen, locale),
            )
        }
    }
}

@Composable
private fun AnalyticsLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun AnalyticsRangePreset.labelRes(): Int = when (this) {
    AnalyticsRangePreset.THIS_MONTH -> R.string.homika_analytics_this_month
    AnalyticsRangePreset.THREE_MONTHS -> R.string.homika_analytics_3m
    AnalyticsRangePreset.SIX_MONTHS -> R.string.homika_analytics_6m
    AnalyticsRangePreset.YTD -> R.string.homika_analytics_ytd
    AnalyticsRangePreset.CUSTOM -> R.string.homika_analytics_custom
}

private fun formatPeriodLabel(
    dashboard: AnalyticsDashboard,
    locale: Locale,
): String {
    val start = dashboard.period.startEpochDay
    val endInclusive = dashboard.period.endEpochDayExclusive - 1L
    return if (start == endInclusive) {
        formatEpochDay(start, locale)
    } else {
        "${formatEpochDay(start, locale)} – ${formatEpochDay(endInclusive, locale)}"
    }
}

private fun trendLabel(
    point: AnalyticsTrendPoint,
    locale: Locale,
): String {
    val start = LocalDate.ofEpochDay(point.startEpochDay)
    val duration = point.endEpochDayExclusive - point.startEpochDay
    val pattern = if (duration <= 1L) "d MMM" else "MMM"
    return start.format(DateTimeFormatter.ofPattern(pattern, locale))
}

private fun signedPercent(
    value: Double,
    locale: Locale,
): String = String.format(locale, "%+.1f%%", value)

private fun signedNumber(
    value: Double,
    locale: Locale,
): String = String.format(locale, "%+.1f", value)

private fun signedInt(value: Int): String = if (value > 0) "+$value" else value.toString()

private fun buildAnalyticsShareText(
    dashboard: AnalyticsDashboard,
    periodLabel: String,
    locale: Locale,
    revenueLabel: String,
    profitLabel: String,
    occupancyLabel: String,
    bookingLabel: String,
): String = buildString {
    append("Homika · ")
    append(periodLabel)
    append('\n')
    append(revenueLabel)
    append(": ")
    append(formatSenAsRinggit(dashboard.current.revenueSen, locale))
    append('\n')
    append(profitLabel)
    append(": ")
    append(formatSenAsRinggit(dashboard.current.netProfitSen, locale))
    append('\n')
    append(occupancyLabel)
    append(": ")
    append(formatPercent(dashboard.current.occupancyPercent, locale))
    append('\n')
    append(bookingLabel)
    append(": ")
    append(dashboard.current.bookingCount)
}
