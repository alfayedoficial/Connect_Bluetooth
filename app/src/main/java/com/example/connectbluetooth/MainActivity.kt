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
    private var mWaveWriter: WaveWriter? = null
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
        mBufferSize = AudioRecord.getMinBufferSize(44100, 16, 2)

        tvCounterRecord1 = findViewById(R.id.tvCounterRecord1)
        btnRecord = findViewById(R.id.buttonRecord)
        buttonStopSound = findViewById(R.id.buttonStopSound)
        buttonPlay = findViewById(R.id.buttonPlay)
        buttonStop = findViewById(R.id.buttonStop)
        btnRecord.setOnClickListener {
            if (!isRecording) {
                isRecording = true
                currentTime = 0
                tvCounterRecord1.text = "00:00"
                displayTime()
                startAudioRecording()
            }

        }

        buttonStop.setOnClickListener {
            stopRecord()
        }
        buttonPlay.setOnClickListener {
            if (PlayerServiceUtil.isPlaying()) {
                PlayerServiceUtil.pause(PauseReason.NONE)
            }
            if (PlayerActivity.mMediaPlayer != null) {
                if (PlayerActivity.mMediaPlayer.isPlaying()) {
                    PlayerActivity.mMediaPlayer.pause()
                }
            }
            if (!isPlaying) {
                startAudioPlaying()
                return
            } else {
                pauseAudioPlaying()
                return
            }
        }
        buttonStopSound.setOnClickListener {
            stopRecord()
        }

//        MainScope().launch{
//            delay(1000)
//            startActivity(Intent(this@MainActivity, com.example.connectbluetooth.sample.MainActivity::class.java))
//        }
    }

    private fun getFilename(): String? {
        val file: File = MyUtils.ChangeVoiceLocation
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath + "/record.wav"
    }
    @SuppressLint("MissingPermission")
    private fun startAudioRecording() {
        if (mBufferSize <= 0) {
            stopRecord()
            return
        }
        deleteMainFile()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mAudioRecord = AudioRecord(1, 44100, 16, 2, mBufferSize)
        mFileName = getFilename()
        mWaveWriter =
            WaveWriter(File(mFileName), 44100, 1, 16)
        try {
            mWaveWriter?.createWaveFile()
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
        if (mAudioRecord!!.state == 1) {
            mAudioRecord!!.startRecording()
            mRecordingThread = Thread { writeAudioDataToFile() }
            mRecordingThread!!.start()
            return
        }
        Toast.makeText(this , "Error" , Toast.LENGTH_SHORT).show()
        stopRecord()
    }

    private fun writeAudioDataToFile() {
        val sArr = ShortArray(8192)
        if (mWaveWriter != null) {
            while (isRecording) {
                val read = mAudioRecord!!.read(sArr, 0, 8192)
                if (-3 != read) {
                    try {
                        mWaveWriter!!.write(sArr, 0, read)
                    } catch (exception: IOException) {
                        exception.printStackTrace()
                    }
                }
            }
        }
    }

    fun isEmptyString(str: String?): Boolean {
        return str == null || str == ""
    }

    private fun deleteMainFile() {
        if (!isEmptyString(mFileName)) {
            try {
                File(mFileName).delete()
            } catch (exception: java.lang.Exception) {
                exception.printStackTrace()
            }
        }
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

    fun displayTime() {
        mHandler.postDelayed(java.lang.Runnable {
            if (currentTime < 60000) {
                currentTime += 1000
                var valueOf: String = (currentTime / 1000 / 60).toString()
                var valueOf2: String = (currentTime / 1000 % 60).toString()
                if (valueOf.length == 1) {
                    valueOf = "0$valueOf"
                }
                if (valueOf2.length == 1) {
                    valueOf2 = "0$valueOf2"
                }
                tvCounterRecord1.text = "$valueOf:$valueOf2"
                displayTime()
                return@Runnable
            }
            stopRecord()
        }, 1000)
    }

    fun stopRecord() {
        if (isRecording) {
            mHandler.removeCallbacksAndMessages(null as Any?)
            isRecording = false
            stopAudioRecording(true)
        }
    }

    fun stopAudioRecording(b: Boolean) {
        if (mAudioRecord != null) {
            try {
                if (mRecordingThread != null) {
                    mRecordingThread!!.interrupt()
                    mRecordingThread = null
                }
                mAudioRecord?.stop()
                mAudioRecord?.release()
                mAudioRecord = null
                try {
                    mWaveWriter?.closeWaveFile()
                } catch (exception: IOException) {
                    exception.printStackTrace()
                }
            } catch (exception2: java.lang.Exception) {
                exception2.printStackTrace()
            }
        }
    }
    private fun pauseAudioPlaying() {
        var mediaPlayer: MediaPlayer ? = null
        if (isPlaying && mPlayer?.also { mediaPlayer = it } != null  && mediaPlayer?.isPlaying == true) {
            mPlayer?.pause()
            isPlaying = false
            btnRecord.text = "Record"
        }
    }


    fun stopAudioPlaying() {
        if (mPlayer != null) {
            mPlayer?.release()
            mPlayer = null
        }
        isPlaying = false
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
