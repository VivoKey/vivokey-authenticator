package com.dangerousthings.vivoauth.ui

import android.app.Activity
import android.app.PendingIntent
import android.arch.lifecycle.ViewModel
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.AsyncTask
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ProgressBar
import com.dangerousthings.vivoauth.R
import com.dangerousthings.vivoauth.R.id.progressBar
import com.dangerousthings.vivoauth.client.KeyManager
import com.dangerousthings.vivoauth.client.OathClient
import com.dangerousthings.vivoauth.exc.AppletMissingException
import com.dangerousthings.vivoauth.exc.PasswordRequiredException
import com.dangerousthings.vivoauth.keystore.ClearingMemProvider
import com.dangerousthings.vivoauth.keystore.KeyStoreProvider
import com.dangerousthings.vivoauth.keystore.SharedPrefProvider
import com.dangerousthings.vivoauth.protocol.YkOathApi
import com.dangerousthings.vivoauth.transport.Backend
import com.dangerousthings.vivoauth.transport.NfcBackend
import com.dangerousthings.vivoauth.transport.UsbBackend
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.Channel
import nordpol.IsoCard
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import org.jetbrains.anko.usbManager
import java.lang.Thread.sleep
import java.util.concurrent.Executors

val EXEC = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

abstract class BaseViewModel : ViewModel() {
    companion object {
        const private val ACTION_USB_PERMISSION = "com.dangerousthings.vivoauth.USB_PERMISSION"
        const private val SP_STORED_AUTH_KEYS = "com.dangerousthings.vivoauth.SP_STORED_AUTH_KEYS"

        private val DUMMY_INFO = YkOathApi.DeviceInfo("", false, YkOathApi.Version(0, 0, 0), false)
        private val MEM_STORE = ClearingMemProvider(EXEC)
        private var sharedLastDeviceInfo = DUMMY_INFO
    }

    data class ClientResult<T>(val result:T?, val error: Throwable?)
    data class ClientRequest(val deviceId: String?, val func: suspend (OathClient) -> Unit)

    protected data class Services(val context: Context, val usbManager: UsbManager, val keyManager: KeyManager)

    protected var services: Services? = null

    private var usbReceiver: BroadcastReceiver? = null
    private val devicesPrompted: MutableSet<UsbDevice> = mutableSetOf()
    private val clientRequests = Channel<ClientRequest>()

    internal var ndefConsumed = false
    internal var nfcWarned = false

    val lastDeviceInfo: YkOathApi.DeviceInfo get() = sharedLastDeviceInfo

    protected open suspend fun onStart(services: Services) = Unit
    fun start(context: Context) = launch(EXEC) {
        val keyManager = KeyManager(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    KeyStoreProvider()
                } else {
                    SharedPrefProvider(context.getSharedPreferences(SP_STORED_AUTH_KEYS, Context.MODE_PRIVATE))
                },
                MEM_STORE
        )
        services?.apply { usbReceiver?.let { context.unregisterReceiver(it) } }
        services = Services(context, context.usbManager, keyManager).apply {
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            usbReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d("yubioath", "USB permission granted, $device")
                        if (device != null) {
                            launch(EXEC) {
                                useBackend(UsbBackend.connect(usbManager, device), keyManager)
                            }
                        }
                    } else {
                        Log.d("yubioath", "USB denied!")
                    }
                }
            }
            context.registerReceiver(usbReceiver, filter)
            checkUsb(this)
            onStart(this)
        }
        Log.d("yubioath", "Started ViewModel: ${this@BaseViewModel}")
    }

    protected open suspend fun onStop(services: Services?) = Unit
    fun stop() = launch(EXEC) {
        services?.apply { context.unregisterReceiver(usbReceiver) }
        onStop(services)
        services = null
        usbReceiver = null
        Log.d("yubioath", "Stopped ViewModel: ${this@BaseViewModel}")
    }

    override fun onCleared() {
        services?.apply { context.unregisterReceiver(usbReceiver) }
        Log.d("yubioath", "ViewModel onCleared() called for $this")
    }

    fun nfcConnected(card: IsoCard) = launch(EXEC) {
        Log.d("yubioath", "NFC DEVICE!")

        services?.let {
            try {
                useBackend(NfcBackend(card), it.keyManager)
            } catch (e: Exception) {
                sharedLastDeviceInfo = DUMMY_INFO
                Log.e("yubioath", "Error using NFC device", e)
            }
        }
    }

    fun <T> requestClient(id: String? = null, func: (api: OathClient) -> T): Deferred<T> = async(EXEC) {
        Log.d("yubioath", "Requesting API...")
        services?.let {
            launch(EXEC) { checkUsb(it) }
        }

        val responseChannel = Channel<ClientResult<T>>()
        clientRequests.send(ClientRequest(id, {
            val result: ClientResult<T> = try {
                ClientResult(func(it), null)
            } catch (e: Throwable) {
                ClientResult(null, e)
            }
            responseChannel.send(result)
        }))

        responseChannel.receive().let {
            it.error?.let { throw it }
            it.result!!
        }
    }

    protected fun clearDevice() {
        sharedLastDeviceInfo = DUMMY_INFO
    }

    protected suspend fun checkUsb(services: Services) {
        val device = services.usbManager.deviceList.values.find { UsbBackend.isSupported(it) }

        when {
            device == null -> {
                if (sharedLastDeviceInfo.persistent) {
                    Log.d("yubioath", "Persistent device removed!")
                    sharedLastDeviceInfo = DUMMY_INFO
                }
            }
            services.usbManager.hasPermission(device) -> {
                Log.d("yubioath", "USB device present")
                useBackend(UsbBackend.connect(services.usbManager, device), services.keyManager)
            }
            device in devicesPrompted -> Log.d("yubioath", "USB no permission, already requested!")
            else -> {
                Log.d("yubioath", "USB no permission, request")
                devicesPrompted.add(device)
                val mPermissionIntent = PendingIntent.getBroadcast(services.context, 0, Intent(ACTION_USB_PERMISSION), 0)
                services.usbManager.requestPermission(device, mPermissionIntent)
            }
        }
    }

    protected open suspend fun useClient(client: OathClient) = Unit

    protected data class BackgroundRefreshParams(val backend: Backend, val keyManager: KeyManager)

    protected inner class BackgroundRefreshTask() : AsyncTask<BackgroundRefreshParams, Void, Unit>() {
        private fun setProgressBarIndeterminate(indeterminate: Boolean) {
            var activity: Activity? = services?.context as? Activity
            var progressBar: ProgressBar? = activity?.findViewById<ProgressBar>(R.id.progressBar)
            progressBar?.isIndeterminate = indeterminate
            progressBar?.scaleY = if(indeterminate) 3f else 1f

        }
        override fun onPreExecute() {
            super.onPreExecute()
            setProgressBarIndeterminate(true)
        }

        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)

            setProgressBarIndeterminate(false)
        }

        override fun doInBackground(vararg params: BackgroundRefreshParams): Unit {
            var param = params[0];

            try {
                OathClient(param.backend, param.keyManager).use { client ->
                    sharedLastDeviceInfo = client.deviceInfo
                    Log.d("yubioath", "Got API, checking requests...")
                    while (!clientRequests.isEmpty) {
                        runBlocking {
                            clientRequests.receive().let { (id, func) ->
                                if (id == null || id == client.deviceInfo.id) {
                                    func(client)
                                }
                            }
                        }
                    }
                    runBlocking {
                        useClient(client)
                    }

                    sleep(5000)
                }
            } catch (e: PasswordRequiredException) {
                launch(UI) {
                    services?.apply {
                        if (context is AppCompatActivity) {
                            context.supportFragmentManager.apply {
                                if (findFragmentByTag("dialog_require_password") == null) {
                                    val transaction = beginTransaction()
                                    RequirePasswordDialog.newInstance(param.keyManager, e.deviceId, e.salt, e.isMissing).show(transaction, "dialog_require_password")
                                }
                            }
                        }
                    }
                }
            } catch (e: AppletMissingException) {
                sharedLastDeviceInfo = DUMMY_INFO
                Log.e("yubioath", "No applet", e)
                launch(UI) {
                    services?.apply {
                        context.toast(R.string.tag_no_applet)
                    }
                }
            } catch (e: Exception) {
                sharedLastDeviceInfo = DUMMY_INFO
                Log.e("yubioath", "Error using OathClient", e)
                launch(UI) {
                    services?.apply {
                        context.toast(R.string.tag_error)
                    }
                }
            }
        }
    }

    private suspend fun useBackend(backend: Backend, keyManager: KeyManager) {
        var param = BackgroundRefreshParams(backend, keyManager)
        BackgroundRefreshTask().execute(param)
    }
}