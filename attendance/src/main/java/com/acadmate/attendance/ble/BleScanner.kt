package com.acadmate.attendance.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.acadmate.attendance.data.BleScanResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val FACULTY_BEACON_UUID = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
const val RSSI_THRESHOLD = -70  // Approximately 5 meters
const val SCAN_TIMEOUT_MS = 15000L

class BleScanner(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter

    @SuppressLint("MissingPermission")
    fun scanForFacultyBeacon(): Flow<BleScanResult> = callbackFlow {
        if (bluetoothAdapter == null) {
            close(Exception("Bluetooth not supported"))
            return@callbackFlow
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            close(Exception("BLE Scanner not available"))
            return@callbackFlow
        }

        val scanCallback = object : android.bluetooth.le.ScanCallback() {
            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                super.onScanResult(callbackType, result)
                result?.let {
                    val rssi = it.rssi
                    if (rssi > RSSI_THRESHOLD) {  // RSSI is negative, so > -70 means closer
                        val distance = calculateDistance(rssi)
                        trySend(
                            BleScanResult(
                                deviceName = it.device.name ?: "Unknown",
                                address = it.device.address,
                                rssi = rssi,
                                distance = distance
                            )
                        )
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                close(Exception("Scan failed with error code: $errorCode"))
            }
        }

        // Start scanning
        scanner.startScan(listOf(), android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build(), scanCallback)


        awaitClose {
            scanner.stopScan(scanCallback)
        }
    }

    /**
     * Calculate distance from RSSI value
     * Using the formula: distance = 10 ^ ((RSSI - txPower) / (10 * N))
     * where txPower is typically -59 dBm at 1 meter
     */
    private fun calculateDistance(rssi: Int): Double {
        val txPower = -59  // Typical BLE transmit power
        val n = 2.0        // Path loss exponent
        return Math.pow(10.0, ((rssi - txPower) / (10 * n)).toDouble())
    }

    /**
     * Check if beacon is within acceptable range
     */
    fun isBeaconInRange(rssi: Int): Boolean {
        return rssi > RSSI_THRESHOLD
    }
}

