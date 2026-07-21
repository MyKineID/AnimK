package com.animk.app.data.repository

import com.animk.app.data.supabase.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseComment(
    val id: String = "",
    val media_id: String,
    val user_id: String,
    val content: String,
    val created_at: String? = null
)

@Serializable
data class SupabaseInteraction(
    val id: String = "",
    val media_id: String,
    val user_id: String,
    val interaction_type: String
)

class SocialRepository {
    private val db = SupabaseManager.client.postgrest

    suspend fun getComments(mediaId: String): List<SupabaseComment> {
        return try {
            db["comments"].select {
                filter { eq("media_id", mediaId) }
            }.decodeList<SupabaseComment>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addComment(mediaId: String, content: String): Boolean {
        return try {
            val user = SupabaseManager.client.auth.currentUserOrNull() ?: return false
            db["comments"].insert(
                mapOf(
                    "media_id" to mediaId,
                    "user_id" to user.id,
                    "content" to content
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun setInteraction(mediaId: String, type: String): Boolean {
        return try {
            val user = SupabaseManager.client.auth.currentUserOrNull() ?: return false
            db["media_interactions"].upsert(
                mapOf(
                    "media_id" to mediaId,
                    "user_id" to user.id,
                    "interaction_type" to type
                ),
                onConflict = "media_id, user_id"
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getInteractionStats(mediaId: String): Pair<Int, Int> {
        return try {
            val list = db["media_interactions"].select {
                filter { eq("media_id", mediaId) }
            }.decodeList<SupabaseInteraction>()

            val likes = list.count { it.interaction_type == "LIKE" }
            val dislikes = list.count { it.interaction_type == "DISLIKE" }
            Pair(likes, dislikes)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }
}
