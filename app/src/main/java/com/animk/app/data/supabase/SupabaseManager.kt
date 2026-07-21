package com.animk.app.data.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {
    val client = createSupabaseClient(
        supabaseUrl = "https://arvkpxmggonwcoodfuby.supabase.co",
        supabaseKey = "sb_publishable_dan0CbVYn2fCYpmDnT1oPw_1VcO4Z6e"
    ) {
        install(Auth)
        install(Postgrest)
    }
}
