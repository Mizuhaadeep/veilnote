package com.glassnote.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
)

object LocationUtils {

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): GeoLocation? {
        if (!hasLocationPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).filter { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }

        val lastKnown = providers.firstNotNullOfOrNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }

        val location = lastKnown ?: withTimeoutOrNull(8_000L) {
            requestSingleLocation(manager, providers)
        } ?: return null

        val address = reverseGeocode(
            context = context,
            latitude = location.latitude,
            longitude = location.longitude,
        )
        return GeoLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            address = address,
        )
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private suspend fun requestSingleLocation(
        manager: LocationManager,
        providers: List<String>,
    ): Location? = suspendCancellableCoroutine { continuation ->
        if (providers.isEmpty()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (continuation.isActive) {
                    manager.removeUpdates(this)
                    continuation.resume(location)
                }
            }

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit

            @Deprecated("Deprecated in API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        runCatching {
            manager.requestSingleUpdate(providers.first(), listener, Looper.getMainLooper())
        }.onFailure {
            continuation.resume(null)
        }

        continuation.invokeOnCancellation {
            runCatching { manager.removeUpdates(listener) }
        }
    }

    private suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): String = withContext(Dispatchers.IO) {
        val fallback = String.format(
            Locale.US,
            "未知位置 (%.6f, %.6f)",
            latitude,
            longitude,
        )

        runCatching {
            if (!Geocoder.isPresent()) return@runCatching fallback
            @Suppress("DEPRECATION")
            val addresses = Geocoder(context, Locale.getDefault())
                .getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.let { address ->
                listOfNotNull(
                    address.countryName,
                    address.adminArea,
                    address.locality ?: address.subAdminArea,
                    address.featureName,
                ).distinct().joinToString(" ").ifBlank { fallback }
            } ?: fallback
        }.getOrDefault(fallback)
    }
}
