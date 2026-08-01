package com.example.pomodoro.core.di

import javax.inject.Qualifier

/**
 * Scope de corrutinas con vida de aplicación, para trabajo que debe sobrevivir a la
 * destrucción de un ViewModel (p. ej. procesar una compra ya confirmada por Google Play).
 * Sustituye al uso de GlobalScope.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope
