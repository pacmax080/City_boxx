package com.example.cityboxx

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class BoxxBluetoothService(
    private val handler: Handler,
    address: String,
    adapter: BluetoothAdapter
) {
    private var bluetoothDevice = adapter.getRemoteDevice(address)
    private var MY_UUID: UUID = bluetoothDevice.uuids[0].uuid
    private val createConnect: CreateConnectThread = CreateConnectThread()


    fun connect() {
        createConnect.start()
    }

    fun cancelConnect() {
        createConnect.cancel()
    }

    fun write(str: String) {
        createConnect.write(str.toByteArray())
    }


    private inner class CreateConnectThread : Thread() {
        var TAG = "CreateConnectThread"

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID)
        }


        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter.cancelDiscovery()
            if (mmSocket != null) {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    mmSocket!!.connect()
                    val writtenMsg = handler.obtainMessage(
                        CONNECTING_STATUS, 1, -1
                    )
                    writtenMsg.sendToTarget()

                } catch (connectException: IOException) {
                    // Unable to connect; close the socket and return.
                    try {
                        mmSocket!!.close()
                        Log.e(TAG, "Device not connect", connectException)
                        // send message in handler Device not connect
                        val writtenMsg = handler.obtainMessage(
                            CONNECTING_STATUS, -1, -1
                        )
                        writtenMsg.sendToTarget()
                    } catch (closeException: IOException) {
                        Log.e(TAG, "Could not close the client socket", closeException)
                    }
                }
                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                val connectedThread = ConnectedThread(mmSocket!!)
                connectedThread.start()
            }
        }

        fun write(byteArr: ByteArray) {
            mmSocket?.let { socket ->
                val connectedThread = ConnectedThread(socket)
                connectedThread.write(byteArr)
            }
        }


        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
                Log.d(TAG, "device disconnected")
                val writtenMsg = handler.obtainMessage(
                    CONNECTING_STATUS, 3, -1
                )
                writtenMsg.sendToTarget()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        var TAG = "ConnectThread"

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int = 0 // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    mmBuffer[numBytes] = mmInStream.read().toByte()
                    var readMessage: String?
                    if (mmBuffer[numBytes] == '\n'.toByte()) {
                        readMessage = String(mmBuffer, 0, numBytes)
                        Log.d("Arduino Message", readMessage!!)
                        handler.obtainMessage(MESSAGE_READ, readMessage).sendToTarget()
                        numBytes = 0
                    } else {
                        numBytes++
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MESSAGE_WRITE, mmBuffer
            )
            writtenMsg.sendToTarget()
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}