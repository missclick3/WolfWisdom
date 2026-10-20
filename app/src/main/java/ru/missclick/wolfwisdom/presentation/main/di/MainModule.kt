package ru.missclick.wolfwisdom.presentation.main.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.missclick.wolfwisdom.presentation.main.MainViewModel

val mainModule = module {
    viewModelOf(::MainViewModel)
}
