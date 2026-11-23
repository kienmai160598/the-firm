package com.kien.movies.addons.phone

import android.content.Context
import android.net.ConnectivityManager
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException

/**
 * The TV's IPv4 address on the home network, for the QR code, or null with no network.
 *
 * First the active network's link addresses (`ConnectivityManager.getLinkProperties`, which finds
 * Ethernet as well as Wi-Fi). That call needs ACCESS_NETWORK_STATE, which today reaches the merged
 * manifest through a library, not the app's own manifest; without it, or when it finds nothing,
 * the up, non-loopback interfaces are read instead, which needs no permission.
 */
object LanAddress {
    fun current(context: Context): String? = fromActiveNetwork(context) ?: fromInterfaces()

    private fun fromActiveNetwork(context: Context): String? = try {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val network = connectivity?.activeNetwork
        connectivity?.getLinkProperties(network)?.linkAddresses
            ?.map { linkAddress -> linkAddress.address }
            ?.firstOrNull(::isLanIpv4)
            ?.hostAddress
    } catch (_: SecurityException) {
        null
    }

    private fun fromInterfaces(): String? = try {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { networkInterface -> networkInterface.isUp && !networkInterface.isLoopback && !networkInterface.isVirtual }
            .flatMap { networkInterface -> networkInterface.inetAddresses.toList() }
            .firstOrNull(::isLanIpv4)
            ?.hostAddress
    } catch (_: SocketException) {
        null
    }

    /** A private IPv4 address (10/8, 172.16/12, 192.168/16): what a phone on the same Wi-Fi reaches. */
    private fun isLanIpv4(address: InetAddress): Boolean = address is Inet4Address && address.isSiteLocalAddress
}
