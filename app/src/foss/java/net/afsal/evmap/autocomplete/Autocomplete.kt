package net.afsal.evmap.autocomplete

import android.content.Context

fun getAutocompleteProviders(context: Context) = listOf(MapboxAutocompleteProvider(context))