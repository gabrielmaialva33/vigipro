package com.vigipro.core.data.repository

import com.vigipro.core.model.Invitation
import com.vigipro.core.model.UserRole

interface InvitationRepository {
    suspend fun getInvitationsForSite(siteId: String): Result<List<Invitation>>
    suspend fun createInvitation(
        siteId: String,
        role: UserRole,
        cameraIds: List<String>? = null,
        timeStart: String? = null,
        timeEnd: String? = null,
        daysOfWeek: List<Int>? = null,
        maxUses: Int = 1,
        expiresInHours: Int = 24,
    ): Result<Invitation>
    suspend fun deleteInvitation(invitationId: String): Result<Unit>
    suspend fun redeemInvitation(code: String): Result<String>
}
