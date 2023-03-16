package com.example.connectbluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
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
import com.example.connectbluetooth.sample.PrinterControl.BixolonPrinter
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity()  , PrinterCallback{

    private lateinit var btnRecord : Button
    private lateinit var buttonPlay : Button
    private lateinit var buttonStopSound : Button
    private lateinit var buttonStop : Button
    private lateinit var tvCounterRecord1 : TextView
    var currentTime: Long = 0

    private var isPlaying = false
    private var isRecording = false
    private var mAudioRecord: AudioRecord? = null
    private var mPlayer: MediaPlayer? = null
    private var mBufferSize = 0

    private val itemList = ArrayList<MyItem>()
    private val _adaptor : MyAdapter by lazy {MyAdapter()}
    private val mHandler by lazy {Handler(Looper.myLooper()!!)}
    private var mRecordingThread: Thread? = null
    private var mFileName: String? = null

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


        findViewById<RecyclerView>(R.id.rvTextCounters).apply {
            adapter = _adaptor
            _adaptor.onListSizeChanged ={ size ->
                if (size == 0) {
                    // Call API to get data again
                    Toast.makeText(applicationContext," Call API to get data again" , Toast.LENGTH_SHORT).show()
                }
            }
        }
        addRandomTimeList()

//        MainScope().launch{
//            delay(1000)
//            startActivity(Intent(this@MainActivity, com.example.connectbluetooth.sample.MainActivity::class.java))
//        }
    }





    private fun addRandomTimeList() {
        // Get current time
        val currentTime = Calendar.getInstance().time

        // Create list to hold MyItems

        // Create random end dates between 1 and 4 minutes from current time
        val random = Random()
        for (i in 1..10) {
            val randomMinute = random.nextInt(2) + 1
            val randomSec = random.nextInt(10) + 1
            val endDate = Calendar.getInstance()
            endDate.time = currentTime
            endDate.add(Calendar.MINUTE, randomMinute)
            endDate.add(Calendar.SECOND, randomSec)
            itemList.add(MyItem("Item $i", endDate.time))
        }

        _adaptor.add(itemList.toCollection(ArrayList()))


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
