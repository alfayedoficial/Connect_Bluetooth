package com.example.connectbluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.connectbluetooth.sample.BluetoothUtils
import com.example.connectbluetooth.sample.PrinterControl.BixolonPrinter
import com.example.connectbluetooth.sample.createBitmapForShortInvoice
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity()  , BluetoothUtils.PrinterCallback{


    private lateinit var _progressBarDialog : ProgressBar
    private lateinit var _viewDialog : View

    companion object{
        private var BXL_PRINTER: BixolonPrinter? = null
        var BLUETOOTH_UTILS : BluetoothUtils = BluetoothUtils()
        @JvmStatic
        var result : (String?)-> Unit = {_->}

        @Synchronized
        fun getPrinterInstance(): BixolonPrinter? {
            return BXL_PRINTER
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_main)

        _progressBarDialog = findViewById(R.id.progressBarDialog)
        _viewDialog = findViewById(R.id.viewDialog)


        findViewById<Button>(R.id.buttonConnectBluetooth).setOnClickListener {
            inflateBluetoothDialog()
        }

        BXL_PRINTER = BixolonPrinter(applicationContext)
        BLUETOOTH_UTILS.setActivity(this)
        BLUETOOTH_UTILS.setPrinterCallbackListener(this)
        result = {
            setDeviceLog(it)
        }

//        MainScope().launch{
//            delay(1000)
//            startActivity(Intent(this@MainActivity, MainActivityJava::class.java))
//        }
    }


    @SuppressLint("MissingPermission")
    private fun inflateBluetoothDialog() {
        ConnectBluetoothFragment.create(
            action = ConnectBluetoothFragment.Action { bluetoothDevice , logicalName ->
                Toast.makeText(this, "Bluetooth name is ${bluetoothDevice.name ?: null}", Toast.LENGTH_LONG).show()
                xPrint(bluetoothDevice , logicalName)
            }
        ).show(supportFragmentManager , "")
    }


    fun xPrint(bluetoothDevice: BluetoothDevice, logicalName: String) {
        _viewDialog.visibility = View.VISIBLE
        _progressBarDialog.visibility = View.VISIBLE

        val context = this
        CoroutineScope(Dispatchers.IO).launch {
            val bitmapResult = withContext(CoroutineScope(Dispatchers.Main).coroutineContext) {
                context.createBitmapForShortInvoice()
            }



            when (logicalName) {
                ConnectBluetoothFragment.PrinterBrand.BIXOLON.value -> {
                    BXL_PRINTER?.printImage(bitmapResult , 384 , 2 , 50 , 0  ,0 )
                    //delay(1000)
//                    BXL_PRINTER?.printerClose()
                }
                ConnectBluetoothFragment.PrinterName.SW_6EAD.value -> {
                    BLUETOOTH_UTILS.printEscCommand(bitmapResult)
                }
                else -> {
                    BLUETOOTH_UTILS.printEscCommand(bitmapResult)
                }
            }
        }
    }





    override fun onPrintFinished(result: BluetoothUtils.ResultOfPrint) {
        MainScope().launch(Dispatchers.Main) {
            _viewDialog.visibility = View.GONE
            _progressBarDialog.visibility = View.GONE

            Toast.makeText(this@MainActivity, "result is ${result.msg}", Toast.LENGTH_LONG).show()
        }

    }


    private val mHandler = Handler(Looper.myLooper()!!) { msg ->
        when (msg.what) {
            0 -> {
                _viewDialog.visibility = View.GONE
                _progressBarDialog.visibility = View.GONE
                Toast.makeText(this@MainActivity, "result is ${(msg.obj as String).trimIndent()}", Toast.LENGTH_LONG).show()

            }
        }
        false
    }

    fun setDeviceLog(data: String?) {
        mHandler.obtainMessage(0, 0, 0, data).sendToTarget()
    }


}