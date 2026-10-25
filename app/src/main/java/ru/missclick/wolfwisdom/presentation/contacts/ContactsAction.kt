package ru.missclick.wolfwisdom.presentation.contacts

sealed interface ContactsAction {
    data object BackPressed : ContactsAction
}
