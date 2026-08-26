package com.example.core.supabase

import android.util.Log
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {
    private const val TAG = "SupabaseClientProvider"

    val client: SupabaseClient by lazy {
        val url = try {
            BuildConfig.SUPABASE_URL.ifBlank { "https://ogqyecmncqclhnvczwxo.supabase.co" }
        } catch (_: Exception) {
            "https://ogqyecmncqclhnvczwxo.supabase.co"
        }

        val anonKey = try {
            BuildConfig.SUPABASE_ANON_KEY.ifBlank { "sb_publishable_P-Gm62dHOAPRtoJhE-504g_tDiinUAA" }
        } catch (_: Exception) {
            "sb_publishable_P-Gm62dHOAPRtoJhE-504g_tDiinUAA"
        }

        Log.i(TAG, "Initializing Supabase Client with URL: $url")

        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anonKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
