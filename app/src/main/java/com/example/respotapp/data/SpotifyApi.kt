package com.example.respotapp.data

import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// --- DTOs ---

data class SpotifyTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Long,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("scope") val scope: String?
)

data class SpotifyUserProfile(
    val id: String,
    @SerializedName("display_name") val displayName: String?,
    val email: String?,
    val product: String?,
    val images: List<SpotifyImage>?
)

data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?
)

data class SpotifyPagedResponse<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val next: String? = null
)

data class SpotifySavedAlbum(
    val album: SpotifyAlbum,
    @SerializedName("added_at") val addedAt: String?
)

data class SpotifySavedTrack(
    @SerializedName("added_at") val addedAt: String?,
    val track: SpotifyTrack?
)

data class SpotifyAlbum(
    val id: String? = null,
    val name: String? = null,
    val artists: List<SpotifyArtist>? = null,
    val images: List<SpotifyImage>?,
    val tracks: SpotifyAlbumTracks?,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("total_tracks") val totalTracks: Int? = null
)

data class SpotifyAlbumTracks(
    val items: List<SpotifyTrack> = emptyList(),
    val total: Int = 0
)

data class SpotifyArtist(
    val id: String? = null,
    val name: String? = null,
    val images: List<SpotifyImage>? = null,
    val genres: List<String>? = null,
    val followers: SpotifyFollowers? = null
)

data class SpotifyFollowers(val total: Int)

data class SpotifyTrack(
    val id: String? = null,
    val name: String? = null,
    val artists: List<SpotifyArtist>? = null,
    val album: SpotifyAlbum?,
    @SerializedName("duration_ms") val durationMs: Long = 0,
    val uri: String? = null,
    @SerializedName("track_number") val trackNumber: Int? = null,
    @SerializedName("disc_number") val discNumber: Int? = null,
    @SerializedName("preview_url") val previewUrl: String? = null,
    @SerializedName("is_local") val isLocal: Boolean = false
)

data class SpotifyPlaylist(
    val id: String? = null,
    val name: String? = null,
    val images: List<SpotifyImage>?,
    @SerializedName("items", alternate = ["tracks"]) val tracks: SpotifyPlaylistTracks?,
    val owner: SpotifyPlaylistOwner?
)

data class SpotifyPlaylistTracks(
    val total: Int = 0,
    val href: String? = null
)

data class SpotifyPlaylistOwner(
    @SerializedName("display_name") val displayName: String?
)

data class SpotifySearchResponse(
    val artists: SpotifyPagedResponse<SpotifyArtist>?,
    val albums: SpotifyPagedResponse<SpotifyAlbum>?,
    val tracks: SpotifyPagedResponse<SpotifyTrack>?
)

data class SpotifyPlaylistTrackItem(
    @SerializedName("item", alternate = ["track"]) val track: SpotifyTrack?,
    val type: String? = null
)

data class SpotifyAddTracksRequest(
    val uris: List<String>
)

// --- API ---

interface SpotifyAuthApi {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun exchangeToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("code") code: String? = null,
        @Field("redirect_uri") redirectUri: String? = null,
        @Field("code_verifier") codeVerifier: String? = null,
        @Field("refresh_token") refreshToken: String? = null
    ): SpotifyTokenResponse
}

interface SpotifyApi {
    @GET("v1/me")
    suspend fun getCurrentUser(): SpotifyUserProfile

    @GET("v1/me/albums")
    suspend fun getSavedAlbums(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): SpotifyPagedResponse<SpotifySavedAlbum>

    @GET("v1/me/playlists")
    suspend fun getUserPlaylists(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: String = "items(id,name,images,items(total),owner(display_name))"
    ): SpotifyPagedResponse<SpotifyPlaylist>

    @GET("v1/playlists/{playlist_id}")
    suspend fun getPlaylist(
        @Path("playlist_id") playlistId: String,
        @Query("fields") fields: String = "id,name,images,items(total),owner(display_name)"
    ): SpotifyPlaylist

    @GET("v1/albums/{id}")
    suspend fun getAlbum(@Path("id") id: String): SpotifyAlbum

    @GET("v1/tracks/{id}")
    suspend fun getTrack(@Path("id") id: String): SpotifyTrack

    @GET("v1/artists/{id}")
    suspend fun getArtist(@Path("id") id: String): SpotifyArtist

    @GET("v1/artists/{id}/albums")
    suspend fun getArtistAlbums(
        @Path("id") id: String,
        @Query("include_groups") includeGroups: String = "album,single",
        @Query("limit") limit: Int = 20
    ): SpotifyPagedResponse<SpotifyAlbum>

    @GET("v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "artist,album,track",
        @Query("limit") limit: Int = 10
    ): SpotifySearchResponse

    @GET("v1/me/tracks")
    suspend fun getSavedTracks(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): SpotifyPagedResponse<SpotifySavedTrack>

    @PUT("v1/me/library")
    suspend fun saveToLibrary(@Query("uris") uris: String)

    @DELETE("v1/me/library")
    suspend fun removeFromLibrary(@Query("uris") uris: String)

    @PUT("v1/me/albums")
    suspend fun saveAlbumsLegacy(@Query("ids") ids: String)

    @PUT("v1/me/tracks")
    suspend fun saveTracks(@Query("ids") ids: String)

    @DELETE("v1/me/tracks")
    suspend fun removeTracks(@Query("ids") ids: String)

    @GET("v1/me/library/contains")
    suspend fun checkLibraryContains(@Query("uris") uris: String): List<Boolean>

    @GET("v1/me/tracks/contains")
    suspend fun checkTracksSaved(@Query("ids") ids: String): List<Boolean>

    @GET("v1/playlists/{playlist_id}/items")
    suspend fun getPlaylistItems(
        @Path("playlist_id") playlistId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("additional_types") additionalTypes: String = "track"
    ): SpotifyPagedResponse<SpotifyPlaylistTrackItem>

    @POST("v1/playlists/{playlist_id}/items")
    suspend fun addTracksToPlaylist(
        @Path("playlist_id") playlistId: String,
        @Body body: SpotifyAddTracksRequest
    )
}

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

fun createSpotifyApi(tokenManager: TokenManager): SpotifyApi {
    val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenManager))
        .build()

    return Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpotifyApi::class.java)
}

fun createSpotifyAuthApi(): SpotifyAuthApi {
    return Retrofit.Builder()
        .baseUrl("https://accounts.spotify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpotifyAuthApi::class.java)
}
