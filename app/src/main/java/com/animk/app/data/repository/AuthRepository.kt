package com.animk.app.data.repository

import com.animk.app.data.supabase.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest

class AuthRepository {
    private val auth = SupabaseManager.client.auth

    suspend fun signUp(emailUser: String, passwordUser: String, username: String): Boolean {
        return try {
            auth.signUpWith(Email) {
                email = emailUser
                password = passwordUser
            }
            val user = auth.currentUserOrNull()
            if (user != null) {
                try {
                    SupabaseManager.client.postgrest["profiles"].insert(
                        mapOf("id" to user.id, "username" to username)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun signIn(emailUser: String, passwordUser: String): Boolean {
        return try {
            auth.signInWith(Email) {
                email = emailUser
                password = passwordUser
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isUserLoggedIn(): Boolean = auth.currentUserOrNull() != null

    fun getCurrentUser(): UserInfo? = auth.currentUserOrNull()
}
