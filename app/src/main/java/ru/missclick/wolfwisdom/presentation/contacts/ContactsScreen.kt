package ru.missclick.wolfwisdom.presentation.contacts

import ru.missclick.wolfwisdom.analytics.rememberScreenAnalytics

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.BuildConfig
import ru.missclick.wolfwisdom.presentation.components.WolfGlassBackdrop
import ru.missclick.wolfwisdom.presentation.components.WolfGlassCard
import ru.missclick.wolfwisdom.presentation.components.WolfServiceScreen
import ru.missclick.wolfwisdom.presentation.util.DeviceConfiguration
import ru.missclick.wolfwisdom.presentation.util.currentDeviceConfiguration
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfTypography
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

private enum class ContactCard(
    @StringRes val title: Int,
    @StringRes val description: Int?,
    @DrawableRes val icon: Int,
    val action: ContactsAction
) {
    BOOSTY(R.string.contacts_boosty, R.string.contacts_news_description, R.drawable.ic_contacts_boosty, ContactsAction.OpenBoosty),
    CHANNEL(R.string.contacts_channel, R.string.contacts_news_description, R.drawable.ic_contacts_telegram, ContactsAction.OpenChannel),
    TELEGRAM(R.string.contacts_telegram, R.string.contacts_message_description, R.drawable.ic_contacts_telegram, ContactsAction.OpenTelegram),
    EMAIL(R.string.contacts_email, null, R.drawable.ic_contacts_mail, ContactsAction.SendEmail)
}

@Composable
fun ContactsRoot(onBackClick: () -> Unit = {}) {
    val analytics = rememberScreenAnalytics(::ContactsAnalytics)
    val context = LocalContext.current
    ContactsScreen(
        onBackButtonClick = { analytics.backClicked(); onBackClick() },
        onAction = { action ->
            analytics.onAction(action)
            if (action == ContactsAction.BackPressed) {
                onBackClick()
            } else {
                val intent = when (action) {
                    ContactsAction.OpenBoosty -> Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.CONTACT_BOOSTY_LINK))
                    ContactsAction.OpenChannel -> Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.CONTACT_TELEGRAM_CHANNEL))
                    ContactsAction.OpenTelegram -> Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://t.me/${BuildConfig.CONTACT_TELEGRAM_USERNAME.removePrefix("@")}")
                    )
                    ContactsAction.SendEmail -> Intent(
                        Intent.ACTION_SENDTO,
                        Uri.fromParts("mailto", BuildConfig.CONTACT_EMAIL, null)
                    )
                    ContactsAction.BackPressed -> error("Back is handled above")
                }
                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.contacts_app_unavailable, Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}

@Composable
fun ContactsScreen(
    onAction: (ContactsAction) -> Unit,
    onBackButtonClick: () -> Unit = { onAction(ContactsAction.BackPressed) }
) {
    BackHandler { onAction(ContactsAction.BackPressed) }
    val device = currentDeviceConfiguration()
    val landscape = device == DeviceConfiguration.MOBILE_LANDSCAPE ||
        device == DeviceConfiguration.TABLET_LANDSCAPE
    val columns = if (landscape) 2 else 1
    val contentWidth = when (device) {
        DeviceConfiguration.MOBILE_PORTRAIT -> 358.dp
        DeviceConfiguration.MOBILE_LANDSCAPE -> 704.dp
        DeviceConfiguration.TABLET_PORTRAIT -> 520.dp
        DeviceConfiguration.TABLET_LANDSCAPE -> 896.dp
    }
    // Insets start below the native status bar, matching the Settings screen.
    val contentTop = when (device) {
        DeviceConfiguration.MOBILE_PORTRAIT -> 92.dp
        DeviceConfiguration.MOBILE_LANDSCAPE -> 80.dp
        DeviceConfiguration.TABLET_PORTRAIT -> 132.dp
        DeviceConfiguration.TABLET_LANDSCAPE -> 168.dp
    }
    WolfServiceScreen(
        title = stringResource(R.string.contacts),
        onBackClick = onBackButtonClick,
        device = device
    ) { backdrop ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = WolfSpacing.Space16)
                .padding(top = contentTop - 60.dp, bottom = WolfSpacing.Space24),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.widthIn(max = contentWidth).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(WolfSpacing.Space12)
            ) {
                ContactCard.entries.chunked(columns).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space12)
                    ) {
                        row.forEach { contact ->
                            ContactCardContent(
                                contact = contact,
                                backdrop = backdrop,
                                minHeight = if (device.isMobile) 100.dp else 112.dp,
                                onClick = { onAction(contact.action) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactCardContent(
    contact: ContactCard,
    backdrop: WolfGlassBackdrop,
    minHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WolfGlassCard(backdrop, modifier.heightIn(min = minHeight)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = minHeight)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = WolfSpacing.Space20, vertical = WolfSpacing.Space16)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space16)
        ) {
            Box(
                Modifier.size(28.dp),
                contentAlignment = if (contact == ContactCard.EMAIL) Alignment.Center else Alignment.TopStart
            ) {
                Image(painterResource(contact.icon), null, Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WolfSpacing.Space4)) {
                Text(stringResource(contact.title), style = WolfTypography.Body, color = WolfColors.Text)
                Text(contact.description?.let { stringResource(it) } ?: BuildConfig.CONTACT_EMAIL, style = WolfTypography.Caption, color = WolfColors.Secondary)
            }
            Image(painterResource(R.drawable.ic_contacts_external), null, Modifier.size(24.dp))
        }
    }
}

@Preview(name = "Contacts · Phone portrait", widthDp = 390, heightDp = 844, locale = "ru")
@Preview(name = "Contacts · Phone landscape", widthDp = 844, heightDp = 390, locale = "ru")
@Preview(name = "Contacts · Tablet portrait", widthDp = 834, heightDp = 1112, locale = "ru")
@Preview(name = "Contacts · Tablet landscape", widthDp = 1112, heightDp = 834, locale = "ru")
@Composable
private fun ContactsPreview() {
    WolfWisdomTheme { ContactsScreen(onAction = {}) }
}
