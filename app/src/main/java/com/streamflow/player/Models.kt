package com.streamflow.player

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("user_info") val userInfo: UserInfo? = null
)

data class UserInfo(
    val username: String = "",
    val password: String = "",
    val auth: Int = 0,
    val status: String = "",
    @SerializedName("exp_date") val expDate: String = "",
    @SerializedName("is_trial") val isTrial: String = "0",
    @SerializedName("max_connections") val maxConnections: String = "1",
    @SerializedName("active_cons") val activeCons: String = "0",
    val message: String = ""
)

data class Category(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String
)

data class LiveStream(
    val num: Int = 0,
    val name: String = "",
    @SerializedName("stream_type") val streamType: String = "",
    @SerializedName("stream_id") val streamId: String = "",
    @SerializedName("stream_icon") val streamIcon: String = "",
    @SerializedName("category_id") val categoryId: String = ""
)

data class VodStream(
    val num: Int = 0,
    val name: String = "",
    @SerializedName("stream_id") val streamId: String = "",
    @SerializedName("stream_icon") val streamIcon: String = "",
    @SerializedName("category_id") val categoryId: String = "",
    @SerializedName("container_extension") val containerExtension: String = "mp4"
)

data class SeriesItem(
    val num: Int = 0,
    val name: String = "",
    @SerializedName("series_id") val seriesId: String = "",
    val cover: String = "",
    @SerializedName("category_id") val categoryId: String = ""
)

data class SeriesDetail(
    val episodes: Map<String, List<EpisodeInfo>> = emptyMap()
)

data class EpisodeInfo(
    val id: String = "",
    val title: String = "",
    @SerializedName("container_extension") val containerExtension: String = "mp4",
    val info: EpisodeInfoData = EpisodeInfoData(),
    @SerializedName("stream_url") val streamUrl: String = "",
    @SerializedName("direct_source") val directSource: String = ""
)

data class EpisodeInfoData(
    @SerializedName("movie_image") val movieImage: String = ""
)
