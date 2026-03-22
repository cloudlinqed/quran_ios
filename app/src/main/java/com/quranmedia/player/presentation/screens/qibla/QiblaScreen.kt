package com.quranmedia.player.presentation.screens.qibla

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.util.ArabicNumeralUtils
import com.quranmedia.player.presentation.components.BottomNavBar
import com.quranmedia.player.presentation.components.DarkModeToggle
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.theme.AppTheme
import com.quranmedia.player.presentation.util.layoutDirection
import kotlin.math.cos
import kotlin.math.sin

private val NorthRed = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    onNavigateBack: () -> Unit,
    onToggleDarkMode: () -> Unit = {},
    onNavigateByRoute: (String) -> Unit = {},
    viewModel: QiblaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings.appLanguage
    val isArabic = language == AppLanguage.ARABIC
    val useIndoArabic = isArabic && settings.useIndoArabicNumerals

    val context = LocalContext.current

    // Lock to portrait
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        viewModel.startCompass()
        onDispose {
            viewModel.stopCompass()
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Smooth azimuth animation with circular wrap handling
    val targetAzimuth = uiState.deviceAzimuth
    var previousTarget by remember { mutableFloatStateOf(0f) }
    var animationBase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(targetAzimuth) {
        var delta = targetAzimuth - previousTarget
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        animationBase += delta
        previousTarget = targetAzimuth
    }

    val animatedAzimuth by animateFloatAsState(
        targetValue = animationBase,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 100f),
        label = "azimuth"
    )

    // Theme colors
    val colors = AppTheme.colors

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isArabic) "اتجاه القبلة" else "Qibla Direction",
                            fontFamily = if (isArabic) scheherazadeFont else null
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        DarkModeToggle(language = language, onToggle = { onToggleDarkMode() })
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppTheme.colors.topBarBackground,
                        titleContentColor = AppTheme.colors.goldAccent,
                        navigationIconContentColor = AppTheme.colors.goldAccent
                    )
                )
            },
            bottomBar = {
                BottomNavBar(
                    currentRoute = "qibla",
                    language = language,
                    onNavigate = onNavigateByRoute
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(AppTheme.colors.screenBackground)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Loading state
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.islamicGreen)
                    }
                    return@Scaffold
                }

                // Error state
                if (uiState.error != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "يرجى تحديد الموقع من صفحة مواقيت الصلاة"
                                   else "Please set your location from Prayer Times",
                            color = colors.textSecondary,
                            fontFamily = if (isArabic) scheherazadeFont else null,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp
                        )
                    }
                    return@Scaffold
                }

                // Sensor not available
                if (!uiState.isSensorAvailable) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "مستشعر البوصلة غير متوفر"
                                   else "Compass sensor not available",
                            color = colors.textSecondary,
                            fontFamily = if (isArabic) scheherazadeFont else null,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp
                        )
                    }
                    return@Scaffold
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calibration banner
                if (uiState.needsCalibration) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.orange.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isArabic) "حرّك هاتفك على شكل رقم ٨ لمعايرة البوصلة"
                                   else "Move your phone in a figure-8 pattern to calibrate",
                            modifier = Modifier.padding(12.dp),
                            color = colors.orange,
                            fontFamily = if (isArabic) scheherazadeFont else null,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Location row
                uiState.location?.let { loc ->
                    val locationText = buildString {
                        loc.cityName?.let { append(it) }
                        if (loc.cityName != null && loc.countryName != null) append(", ")
                        loc.countryName?.let { append(it) }
                    }
                    if (locationText.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = colors.islamicGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = locationText,
                                color = colors.textSecondary,
                                fontSize = 14.sp,
                                fontFamily = if (isArabic) scheherazadeFont else null
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Compass Canvas
                val borderColor = colors.border
                val textPrimaryColor = colors.textPrimary
                val textSecondaryColor = colors.textSecondary
                val goldColor = colors.goldAccent
                val greenColor = colors.islamicGreen
                val qiblaBearing = uiState.qiblaBearing

                val cardinalLabels = if (isArabic) {
                    listOf("ش", "شر", "ج", "غ")
                } else {
                    listOf("N", "E", "S", "W")
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = -animatedAzimuth }
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f - 16.dp.toPx()

                        // Outer ring
                        drawCircle(
                            color = borderColor,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Tick marks
                        for (i in 0 until 360 step 5) {
                            val angle = Math.toRadians(i.toDouble()).toFloat()
                            val isMajor = i % 30 == 0
                            val tickLength = if (isMajor) 16.dp.toPx() else 8.dp.toPx()
                            val tickColor = if (isMajor) textPrimaryColor else textSecondaryColor
                            val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                            val outerX = center.x + radius * sin(angle)
                            val outerY = center.y - radius * cos(angle)
                            val innerX = center.x + (radius - tickLength) * sin(angle)
                            val innerY = center.y - (radius - tickLength) * cos(angle)

                            drawLine(
                                color = tickColor,
                                start = Offset(outerX, outerY),
                                end = Offset(innerX, innerY),
                                strokeWidth = tickWidth
                            )
                        }

                        // Cardinal letters
                        val textRadius = radius - 32.dp.toPx()
                        val cardinalAngles = listOf(0f, 90f, 180f, 270f)
                        cardinalLabels.forEachIndexed { index, label ->
                            val angle = Math.toRadians(cardinalAngles[index].toDouble()).toFloat()
                            val x = center.x + textRadius * sin(angle)
                            val y = center.y - textRadius * cos(angle)
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    color = if (index == 0) NorthRed.hashCode()
                                            else textPrimaryColor.hashCode()
                                    textSize = 18.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                    isFakeBoldText = index == 0
                                }
                                // Adjust y for text baseline centering
                                val textBounds = android.graphics.Rect()
                                paint.getTextBounds(label, 0, label.length, textBounds)
                                drawText(label, x, y + textBounds.height() / 2f, paint)
                            }
                        }

                        // North indicator (red triangle at 0 degrees)
                        val northTriangleSize = 10.dp.toPx()
                        val northTipX = center.x
                        val northTipY = center.y - radius - 4.dp.toPx()
                        val northPath = Path().apply {
                            moveTo(northTipX, northTipY)
                            lineTo(northTipX - northTriangleSize / 2, northTipY + northTriangleSize)
                            lineTo(northTipX + northTriangleSize / 2, northTipY + northTriangleSize)
                            close()
                        }
                        drawPath(northPath, NorthRed)

                        // Qibla arrow
                        val qiblaAngle = Math.toRadians(qiblaBearing.toDouble()).toFloat()
                        val arrowLength = radius - 40.dp.toPx()
                        val arrowEndX = center.x + arrowLength * sin(qiblaAngle)
                        val arrowEndY = center.y - arrowLength * cos(qiblaAngle)

                        // Arrow line
                        drawLine(
                            color = goldColor,
                            start = center,
                            end = Offset(arrowEndX, arrowEndY),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Arrowhead
                        val arrowHeadSize = 14.dp.toPx()
                        val arrowHeadAngle = Math.toRadians(25.0).toFloat()
                        val arrowPath = Path().apply {
                            moveTo(arrowEndX, arrowEndY)
                            lineTo(
                                arrowEndX - arrowHeadSize * sin(qiblaAngle - arrowHeadAngle),
                                arrowEndY + arrowHeadSize * cos(qiblaAngle - arrowHeadAngle)
                            )
                            lineTo(
                                arrowEndX - arrowHeadSize * sin(qiblaAngle + arrowHeadAngle),
                                arrowEndY + arrowHeadSize * cos(qiblaAngle + arrowHeadAngle)
                            )
                            close()
                        }
                        drawPath(arrowPath, goldColor)

                        // Kaaba marker circle at arrow tip
                        drawCircle(
                            color = goldColor,
                            radius = 8.dp.toPx(),
                            center = Offset(arrowEndX, arrowEndY)
                        )

                        // Kaaba icon (small square inside circle)
                        val kaabaSize = 5.dp.toPx()
                        drawRect(
                            color = Color.Black.copy(alpha = 0.7f),
                            topLeft = Offset(arrowEndX - kaabaSize / 2, arrowEndY - kaabaSize / 2),
                            size = androidx.compose.ui.geometry.Size(kaabaSize, kaabaSize)
                        )

                        // Center dot
                        drawCircle(
                            color = greenColor,
                            radius = 6.dp.toPx(),
                            center = center
                        )
                    }
                }

                // Bearing info
                val headingDeg = uiState.deviceAzimuth.toInt()
                val qiblaDeg = uiState.qiblaBearing.toInt()

                val headingText = if (useIndoArabic) ArabicNumeralUtils.toIndoArabic(headingDeg.toString())
                                  else headingDeg.toString()
                val qiblaText = if (useIndoArabic) ArabicNumeralUtils.toIndoArabic(qiblaDeg.toString())
                                else qiblaDeg.toString()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isArabic) "الاتجاه" else "Heading",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontFamily = if (isArabic) scheherazadeFont else null
                        )
                        Text(
                            text = "$headingText°",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isArabic) scheherazadeFont else null
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isArabic) "القبلة" else "Qibla",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontFamily = if (isArabic) scheherazadeFont else null
                        )
                        Text(
                            text = "$qiblaText°",
                            color = colors.goldAccent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isArabic) scheherazadeFont else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Distance to Makkah
                val distanceKm = uiState.distanceToMakkahKm.toInt()
                val formattedDistance = if (useIndoArabic) {
                    ArabicNumeralUtils.toIndoArabic(String.format("%,d", distanceKm))
                } else {
                    String.format("%,d", distanceKm)
                }
                Text(
                    text = if (isArabic) "$formattedDistance كم إلى مكة المكرمة"
                           else "$formattedDistance km to Makkah",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = if (isArabic) scheherazadeFont else null
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Usage hint
                Text(
                    text = if (isArabic) "أمسك الهاتف بشكل مسطح"
                           else "Hold phone flat",
                    color = colors.textSecondary.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = if (isArabic) scheherazadeFont else null
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
