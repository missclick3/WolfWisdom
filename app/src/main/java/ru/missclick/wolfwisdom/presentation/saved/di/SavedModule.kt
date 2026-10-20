package ru.missclick.wolfwisdom.presentation.saved.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.missclick.wolfwisdom.presentation.saved.SavedViewModel

val savedModule = module {
    viewModelOf(::SavedViewModel)
}
