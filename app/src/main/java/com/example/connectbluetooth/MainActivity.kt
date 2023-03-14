package com.example.connectbluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioRecord
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.connectbluetooth.sample.MainActivity
import com.example.connectbluetooth.sample.PrinterControl.BixolonPrinter
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity()  , PrinterCallback{

    companion object{
        private var BXL_PRINTER: BixolonPrinter? = null

        @Synchronized
        fun getPrinterInstance(): BixolonPrinter? {
            return BXL_PRINTER
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_main)

        findViewById<Button>(R.id.buttonConnectBluetooth).setOnClickListener {
            inflateBluetoothDialog()
        }

        BXL_PRINTER = BixolonPrinter(applicationContext)


        MainScope().launch{
            delay(1000)
            startActivity(Intent(this@MainActivity, MainActivity::class.java))
        }
    }


    @SuppressLint("MissingPermission")
    private fun inflateBluetoothDialog() {
        ConnectBluetoothFragment.create(
            action = ConnectBluetoothFragment.Action{ bluetoothDevice ->
                Toast.makeText(this, "Bluetooth name is ${bluetoothDevice.name?: null}" , Toast.LENGTH_LONG).show()
                xPrint(bluetoothDevice)
            }
        ).show(supportFragmentManager , "")
    }


    fun xPrint(bluetoothDevice: BluetoothDevice) {
        val context = this
        CoroutineScope(Dispatchers.IO).launch {
            val bitmapResult = withContext(CoroutineScope(Dispatchers.Main).coroutineContext) {
                context.createBitmapForInvoice()
            }

        }
    }

    override fun onPrintFinished(result: ResultOfPrint) {
        MainScope().launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "result is ${result.msg}" , Toast.LENGTH_LONG).show()
        }
    }

}

private fun Context.createBitmapForInvoice() :Bitmap? = try {
    val mInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val mView = RelativeLayout(this)

    mInflater.inflate(R.layout.item_test_print , mView , true)

    mView.layoutParams = ConstraintLayout.LayoutParams(
        ConstraintLayout.LayoutParams.MATCH_PARENT,
        ConstraintLayout.LayoutParams.MATCH_PARENT)

    //Pre-measure the view so that height and width don't remain null.
    mView.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )

    //Assign a size and position to the view and all of its descendants
    mView.layout(0, 0, mView.measuredWidth, mView.measuredHeight)

    //Create the bitmap
    val bitmap = Bitmap.createBitmap(mView.measuredWidth, mView.measuredHeight, Bitmap.Config.ARGB_8888)
    //Create a canvas with the specified bitmap to draw into
    val c = Canvas(bitmap)

    //Render this view (and all of its children) to the given Canvas
    mView.draw(c)
    //bitmap = resizeImage(bitmap, 576, false);
    bitmap
}catch (e :Exception){
    e.printStackTrace()
    null
}
