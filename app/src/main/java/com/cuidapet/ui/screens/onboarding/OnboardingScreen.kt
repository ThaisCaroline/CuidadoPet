package com.cuidadopet.ui.screens.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuidadopet.R
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector? = null,
    val imageRes: Int? = null,
    val titleRes: Int,
    val bodyRes: Int
)

private val pages = listOf(
    OnboardingPage(imageRes = R.mipmap.ic_launcher_round, titleRes = R.string.onboarding_1_title, bodyRes = R.string.onboarding_1_body),
    OnboardingPage(icon = Icons.Default.Medication,       titleRes = R.string.onboarding_2_title, bodyRes = R.string.onboarding_2_body),
    OnboardingPage(icon = Icons.Default.Restaurant,       titleRes = R.string.onboarding_3_title, bodyRes = R.string.onboarding_3_body),
    OnboardingPage(icon = Icons.Default.Favorite,         titleRes = R.string.onboarding_4_title, bodyRes = R.string.onboarding_4_body)
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope      = rememberCoroutineScope()
    val isLast     = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Botão Pular
        Box(modifier = Modifier.fillMaxWidth()) {
            if (!isLast) {
                TextButton(
                    onClick  = onFinish,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.weight(1f)
        ) { index ->
            val page = pages[index]
            Column(
                modifier            = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (page.imageRes != null) {
                    AsyncImage(
                        model              = page.imageRes,
                        contentDescription = null,
                        modifier           = Modifier.size(96.dp)
                    )
                } else if (page.icon != null) {
                    Icon(
                        imageVector        = page.icon,
                        contentDescription = null,
                        modifier           = Modifier.size(96.dp),
                        tint               = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    text       = stringResource(page.titleRes),
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text      = stringResource(page.bodyRes),
                    style     = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Indicadores de página
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.padding(vertical = 24.dp)
        ) {
            repeat(pages.size) { index ->
                val width by animateDpAsState(
                    targetValue = if (pagerState.currentPage == index) 24.dp else 8.dp,
                    label       = "dot_width"
                )
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .background(
                            color = if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                )
            }
        }

        // Botão Próximo / Começar
        Button(
            onClick  = {
                if (isLast) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next))
        }

        Spacer(Modifier.height(8.dp))
    }
}
