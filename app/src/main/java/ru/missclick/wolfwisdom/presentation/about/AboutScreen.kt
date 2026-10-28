package ru.missclick.wolfwisdom.presentation.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import ru.missclick.wolfwisdom.BuildConfig
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.analytics.rememberScreenAnalytics
import ru.missclick.wolfwisdom.presentation.components.WolfGlassBackdrop
import ru.missclick.wolfwisdom.presentation.components.WolfGlassCard
import ru.missclick.wolfwisdom.presentation.components.WolfServiceScreen
import ru.missclick.wolfwisdom.presentation.util.DeviceConfiguration
import ru.missclick.wolfwisdom.presentation.util.currentDeviceConfiguration
import ru.missclick.wolfwisdom.ui.theme.*

internal val AboutDocument.title: Int
    get() = when (this) {
        AboutDocument.LICENSES -> R.string.about_licenses
        AboutDocument.PRIVACY_POLICY -> R.string.about_privacy
        AboutDocument.TERMS -> R.string.about_terms
    }

@Composable
fun AboutRoot(onBackClick: () -> Unit, onDocumentClick: (AboutDocument) -> Unit) {
    val analytics = rememberScreenAnalytics(::AboutAnalytics)
    AboutScreen(
        onBackClick = { analytics.backClicked(); onBackClick() },
        onDocumentClick = { analytics.documentClicked(it); onDocumentClick(it) }
    )
}

@Composable
fun AboutScreen(onBackClick: () -> Unit, onDocumentClick: (AboutDocument) -> Unit) {
    val device = currentDeviceConfiguration()
    val tablet = !device.isMobile
    val landscape = device == DeviceConfiguration.MOBILE_LANDSCAPE || device == DeviceConfiguration.TABLET_LANDSCAPE
    val top = when (device) {
        DeviceConfiguration.MOBILE_PORTRAIT -> 12.dp
        DeviceConfiguration.MOBILE_LANDSCAPE -> 18.dp
        DeviceConfiguration.TABLET_PORTRAIT -> 42.dp
        DeviceConfiguration.TABLET_LANDSCAPE -> 108.dp
    }
    WolfServiceScreen(stringResource(R.string.about_app), onBackClick, device, expandTitle = true) { backdrop ->
        // Scroll the content, keeping the shared header and safe system insets fixed.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = landscape && maxWidth >= 700.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = if (columns) { if (tablet) 48.dp else 32.dp } else 16.dp)
                    .padding(top = top, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (columns) {
                    Row(
                        Modifier.widthIn(max = if (tablet) 1016.dp else 780.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (tablet) 32.dp else 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AboutBrand(if (tablet) 156.dp else 100.dp, tablet, Modifier.weight(if (tablet) 220f else 160f))
                        AboutDescription(tablet, true, Modifier.weight(if (tablet) 392f else 304f))
                        AboutLinks(backdrop, tablet, true, onDocumentClick, Modifier.weight(if (tablet) 340f else 276f))
                    }
                } else {
                    Column(
                        Modifier.widthIn(max = if (tablet) 560.dp else 358.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AboutBrand(if (tablet) 144.dp else 104.dp, tablet)
                        Spacer(Modifier.height(if (tablet) 24.dp else 16.dp))
                        AboutDescription(tablet, false)
                        Spacer(Modifier.height(if (tablet) 24.dp else 16.dp))
                        AboutLinks(backdrop, tablet, false, onDocumentClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutBrand(iconSize: Dp, tablet: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Explicit resource: the default launcher icon, regardless of the active seasonal alias.
    // painterResource cannot load AdaptiveIconDrawable, so draw the real icon into a bitmap.
    val icon = remember(context) {
        requireNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher))
            .toBitmap(512, 512).asImageBitmap()
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(icon, null, Modifier.size(iconSize))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.app_name), Modifier.fillMaxWidth(), color = WolfColors.Text,
            style = WolfTypography.Title.copy(fontSize = if (tablet) 28.sp else 22.sp, lineHeight = if (tablet) 39.sp else 31.sp),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME.substringBefore('-')), color = WolfColors.Secondary,
            style = WolfTypography.Label.copy(lineHeight = 18.sp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun AboutDescription(tablet: Boolean, landscape: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.about_quote), color = WolfColors.Text,
            style = WolfTypography.Quote.copy(fontSize = if (tablet) 24.sp else 18.sp, lineHeight = if (tablet) 34.sp else 25.sp),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(if (tablet) 24.dp else 16.dp))
        Text(stringResource(R.string.about_description), color = WolfColors.Text,
            style = WolfTypography.Body.copy(fontSize = if (tablet) 16.sp else if (landscape) 12.sp else 13.sp,
                lineHeight = if (tablet) 22.sp else if (landscape) 17.sp else 18.sp),
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun AboutLinks(backdrop: WolfGlassBackdrop, tablet: Boolean, landscape: Boolean,
    onClick: (AboutDocument) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AboutDocument.entries.forEach { document ->
            val icon = when (document) {
                AboutDocument.LICENSES -> R.drawable.about_licenses
                AboutDocument.PRIVACY_POLICY -> R.drawable.about_privacy
                AboutDocument.TERMS -> R.drawable.about_terms
            }
            WolfGlassCard(backdrop, Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().heightIn(min = if (tablet) 76.dp else 64.dp)
                    .clickable(role = Role.Button) { onClick(document) }
                    .padding(horizontal = if (landscape && !tablet) 12.dp else 20.dp, vertical = 10.dp)
                    .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (landscape && !tablet) 8.dp else 16.dp)) {
                    Image(painterResource(icon), null, Modifier.size(28.dp))
                    Text(stringResource(document.title), Modifier.weight(1f), color = WolfColors.Text,
                        style = WolfTypography.Body.copy(fontSize = if (landscape && !tablet) 12.sp else 15.sp,
                            lineHeight = if (landscape && !tablet) 17.sp else 22.sp))
                    Image(painterResource(R.drawable.about_chevron), null, Modifier.size(24.dp))
                }
            }
        }
    }
}

@Preview(name = "About · Phone", widthDp = 390, heightDp = 844, locale = "ru")
@Preview(name = "About · Phone landscape", widthDp = 844, heightDp = 390, locale = "ru")
@Preview(name = "About · Tablet", widthDp = 834, heightDp = 1112, locale = "ru")
@Preview(name = "About · Tablet landscape", widthDp = 1112, heightDp = 834, locale = "ru")
@Preview(name = "About · Large text", widthDp = 390, heightDp = 844, fontScale = 2f, locale = "ru")
@Composable
private fun AboutPreview() { WolfWisdomTheme { AboutScreen({}, {}) } }
