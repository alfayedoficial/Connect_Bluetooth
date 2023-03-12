package com.example.connectbluetooth

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Parcelable
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import java.io.Serializable

fun DialogFragment.setWindowParams() {
    dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog?.window?.setLayout(
        ConstraintLayout.LayoutParams.MATCH_PARENT,
        ConstraintLayout.LayoutParams.MATCH_PARENT
    )
    dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
    dialog?.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
    dialog?.window?.setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
}

fun getDeviceDrawable(deviceClass: Int): Int {
    return when (deviceClass) {
        DevicesTypes.AUDIO_VIDEO.type -> R.drawable.ic_mic
        DevicesTypes.PHONE.type -> R.drawable.ic_phone
        DevicesTypes.COMPUTER.type -> R.drawable.ic_mac
        DevicesTypes.IMAGING.type -> R.drawable.ic_printshop
        else -> R.drawable.ic_device_unknown
    }
}

enum class DevicesTypes(val type: Int) {
    AUDIO_VIDEO(1024),
    COMPUTER(256),
    IMAGING(1536),
    PHONE(512),
}

fun <T : Serializable?> getSerializable(intent: Intent, key: String, m_class: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        intent.getSerializableExtra(key, m_class)!!
    else
        intent.getSerializableExtra(key) as T?
}

fun <T : Parcelable?> getParcelableExtra(intent: Intent , key: String , m_class: Class<T>):T?{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        intent.getParcelableExtra(key, m_class)!!
    }else{
        intent.getParcelableExtra(key) as T?
    }
}


interface PrinterCallback {
    fun onPrintFinished(result: ResultOfPrint)
}

data class ResultOfPrint(val result: Boolean, val msg: String)
