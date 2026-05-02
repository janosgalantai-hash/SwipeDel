package com.swipedel

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scope = rememberCoroutineScope()

    val cardScale    = remember { Animatable(0f) }
    val leftArrowX   = remember { Animatable(-300f) }
    val rightArrowX  = remember { Animatable(300f) }
    val titleAlpha   = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val screenAlpha  = remember { Animatable(1f) }

    val demoRotation = remember { Animatable(0f) }
    val demoOffsetX  = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        cardScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))

        launch { leftArrowX.animateTo(0f, tween(350, easing = EaseOutCubic)) }
        launch { rightArrowX.animateTo(0f, tween(350, easing = EaseOutCubic)) }
        delay(300)

        launch { titleAlpha.animateTo(1f, tween(400)) }
        delay(200)
        subtitleAlpha.animateTo(1f, tween(400))

        delay(700)

        // Demo: card tilts left
        launch { demoRotation.animateTo(-15f, tween(400, easing = EaseInOutCubic)) }
        launch { demoOffsetX.animateTo(-80f, tween(400, easing = EaseInOutCubic)) }
        delay(500)

        screenAlpha.animateTo(0f, tween(350))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha.value }
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer { translationX = leftArrowX.value }
                )

                Spacer(Modifier.width(28.dp))

                // Card stack — outlines only, no color
                Box(
                    modifier = Modifier
                        .size(150.dp, 200.dp)
                        .graphicsLayer { scaleX = cardScale.value; scaleY = cardScale.value },
                    contentAlignment = Alignment.Center
                ) {
                    // Card 3 — back
                    Box(
                        modifier = Modifier
                            .size(130.dp, 175.dp)
                            .graphicsLayer { rotationZ = 8f; translationX = 18f; translationY = -8f }
                            .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    )
                    // Card 2 — middle
                    Box(
                        modifier = Modifier
                            .size(130.dp, 175.dp)
                            .graphicsLayer { rotationZ = -4f; translationX = -8f; translationY = 4f }
                            .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    )
                    // Card 1 — front, animated
                    Box(
                        modifier = Modifier
                            .size(130.dp, 175.dp)
                            .graphicsLayer {
                                rotationZ = demoRotation.value
                                translationX = demoOffsetX.value
                            }
                            .border(1.5.dp, Color.White, RoundedCornerShape(12.dp))
                    )
                }

                Spacer(Modifier.width(28.dp))

                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer { translationX = rightArrowX.value }
                )
            }

            Spacer(Modifier.height(56.dp))

            Text(
                text = "SwipeDel",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha.value }
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Swipe to clean your gallery",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha.value }
            )
        }
    }
}
