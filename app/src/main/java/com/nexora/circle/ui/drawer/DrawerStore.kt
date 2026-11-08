package com.nexora.circle.ui.drawer

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

object DrawerStore {
    fun prefs(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences("nexora_drawer", Context.MODE_PRIVATE)
}

/** Saved on-device boolean. Usage: val (on, setOn) = rememberBoolPref("key", false) */
@Composable
fun rememberBoolPref(key: String, default: Boolean): Pair<Boolean, (Boolean) -> Unit> {
    val ctx = LocalContext.current
    var value by remember { mutableStateOf(DrawerStore.prefs(ctx).getBoolean(key, default)) }
    return value to { v: Boolean ->
        value = v
        DrawerStore.prefs(ctx).edit().putBoolean(key, v).apply()
    }
}
