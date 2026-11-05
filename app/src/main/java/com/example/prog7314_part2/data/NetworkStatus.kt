package com.example.prog7314_part2.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Observe the default network while the screen is visible. */
fun networkStatus(context: Context) = callbackFlow {
    val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    fun connected(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(connected()) }
        override fun onLost(network: Network) { trySend(connected()) }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            trySend(connected())
        }
    }
    manager.registerDefaultNetworkCallback(callback)
    trySend(connected())
    awaitClose { manager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
