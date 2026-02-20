package com.vigipro.core.data.repository

import com.vigipro.core.model.Invitation
import com.vigipro.core.model.UserRole
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Serializable
private data class CreateInvitationRequest(
    @SerialName("site_id") val siteId: String,
    @SerialName("invite_code") val inviteCode: String,
    val role: String,
    @SerialName("camera_ids") val cameraIds: List<String>? = null,
    @SerialName("time_start") val timeStart: String? = null,
    @SerialName("time_end") val timeEnd: String? = null,
    @SerialName("days_of_week") val daysOfWeek: List<Int>? = null,
    @SerialName("max_uses") val maxUses: Int = 1,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("created_by") val createdBy: String,
)

@Singleton
class SupabaseInvitationRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : InvitationRepository {

    override suspend fun getInvitationsForSite(siteId: String): Result<List<Invitation>> =
        runCatching {
            supabase.from("invitations")
                .select {
                    filter { eq("site_id", siteId) }
                }
                .decodeList<Invitation>()
        }

    override suspend fun createInvitation(
        siteId: String,
        role: UserRole,
        cameraIds: List<String>?,
        timeStart: String?,
        timeEnd: String?,
        daysOfWeek: List<Int>?,
        maxUses: Int,
        expiresInHours: Int,
    ): Result<Invitation> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("Usuario nao autenticado")

        val code = generateInviteCode()
        val expiresAt = Instant.now()
            .plus(expiresInHours.toLong(), ChronoUnit.HOURS)
            .toString()

        val roleName = when (role) {
            UserRole.VIEWER -> "viewer"
            UserRole.TIME_RESTRICTED -> "time_restricted"
            UserRole.GUEST -> "guest"
            else -> "viewer"
        }

        supabase.from("invitations")
            .insert(
                CreateInvitationRequest(
                    siteId = siteId,
                    inviteCode = code,
                    role = roleName,
                    cameraIds = cameraIds,
                    timeStart = timeStart,
                    timeEnd = timeEnd,
                    daysOfWeek = daysOfWeek,
                    maxUses = maxUses,
                    expiresAt = expiresAt,
                    createdBy = userId,
                ),
            ) {
                select()
            }
            .decodeSingle<Invitation>()
    }

    override suspend fun deleteInvitation(invitationId: String): Result<Unit> =
        runCatching {
            supabase.from("invitations").delete {
                filter { eq("id", invitationId) }
            }
        }

    override suspend fun redeemInvitation(code: String): Result<String> =
        runCatching {
            val json = supabase.postgrest.rpc(
                "redeem_invitation",
                buildJsonObject { put("p_code", code) },
            ).decodeSingle<JsonObject>()

            val status = json["status"]?.jsonPrimitive?.content ?: "error"

            when (status) {
                "success" -> json["site_id"]?.jsonPrimitive?.content ?: ""
                "already_member" -> throw IllegalStateException("Voce ja e membro deste site")
                else -> throw IllegalStateException("Erro ao resgatar convite")
            }
        }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Sem I/O/0/1 pra evitar confusao
        return buildString {
            repeat(8) { append(chars[Random.nextInt(chars.length)]) }
        }
    }
}
