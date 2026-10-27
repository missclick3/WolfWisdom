package ru.missclick.wolfwisdom.presentation.contacts

sealed interface ContactsAction {
    data object BackPressed : ContactsAction
    data object OpenBoosty : ContactsAction
    data object OpenChannel : ContactsAction
    data object OpenTelegram : ContactsAction
    data object SendEmail : ContactsAction
}
