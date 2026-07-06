package com.sonus.player.data.remote.ccmixter

import retrofit2.http.GET
import retrofit2.http.Query

interface CcMixterApiService {

    @GET("api/query")
    suspend fun searchByTags(
        @Query("tags") tags: String,
        @Query("limit") limit: Int = 20,
        @Query("f") format: String = "json"
    ): List<CcMixterTrack>
}

data class CcMixterTrack(
    val upload_id: Long?,
    val upload_name: String?,
    val user_name: String?,
    val user_real_name: String?,
    val file_page_url: String?,
    val license_name: String?,
    val upload_date_format: String?,
    val files: List<CcMixterFile>?
)

data class CcMixterFile(
    val file_id: Long?,
    val file_name: String?,
    val file_nicname: String?,
    val file_format_info: CcMixterFormatInfo?,
    val download_url: String?,
    val file_rawsize: Long?
)

data class CcMixterFormatInfo(
    val ps: String?,        // duration string like "1:40"
    val sr: String?,        // sample rate like "48k"
    val ch: String?,        // channels "stereo"/"mono"
    val br: String?         // bitrate type "CBR"/"VBR"
)
