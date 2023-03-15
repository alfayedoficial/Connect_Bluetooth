package com.example.connectbluetooth.sample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import com.example.connectbluetooth.BaseEnum
import com.example.connectbluetooth.MainActivity
import com.rt.printerlibrary.bean.BluetoothEdrConfigBean
import com.rt.printerlibrary.cmd.EscFactory
import com.rt.printerlibrary.connect.PrinterInterface
import com.rt.printerlibrary.enumerate.BmpPrintMode
import com.rt.printerlibrary.enumerate.CommonEnum
import com.rt.printerlibrary.enumerate.ConnectStateEnum
import com.rt.printerlibrary.exception.SdkException
import com.rt.printerlibrary.factory.connect.BluetoothFactory
import com.rt.printerlibrary.factory.connect.PIFactory
import com.rt.printerlibrary.factory.printer.PrinterFactory
import com.rt.printerlibrary.factory.printer.ThermalPrinterFactory
import com.rt.printerlibrary.observer.PrinterObserver
import com.rt.printerlibrary.observer.PrinterObserverManager
import com.rt.printerlibrary.printer.RTPrinter
import com.rt.printerlibrary.setting.BitmapSetting
import com.rt.printerlibrary.setting.CommonSetting
import com.rt.printerlibrary.utils.PrintStatusCmd
import com.rt.printerlibrary.utils.PrinterStatusPareseUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

const val TAG = "PrintUtils"


class BluetoothUtils : PrinterObserver {

    private var tvDeviceSelected: String? = null
    private var tvDeviceSelectedTag: Int = BaseEnum.NO_DEVICE
    private var printEnable: Boolean? = null
    private var printerFactory: PrinterFactory? = null
    private var configObj: Any? = null
    private val printerInterfaceArrayList = ArrayList<PrinterInterface<*>>()
    private var rtPrinterKotlin: RTPrinter<BluetoothEdrConfigBean>? = null
    private var curPrinterInterface: PrinterInterface<*>? = null
    private var iPrintTimes = 0

    @BaseEnum.ConnectType
    private var checkedConType = BaseEnum.CON_BLUETOOTH
    @BaseEnum.CmdType
    private val currentCmdType = BaseEnum.CMD_ESC

    private var mActivity :  MainActivity? = null
    private var onBluetoothUtilsListener : OnBluetoothUtilsListener? = null
    private var printerCallbackListener: PrinterCallback? = null


    interface OnBluetoothUtilsListener {
        fun onBluetoothUtilsListener(enable : Boolean)
    }

    fun setActivity(activity: MainActivity?){
        mActivity = activity
    }

    fun setPrinterCallbackListener(printerCallbacksListener: PrinterCallback){
        this.printerCallbackListener = printerCallbacksListener
    }

    fun setOnBluetoothUtilsListener(onBluetoothUtilsListener: OnBluetoothUtilsListener){
        this.onBluetoothUtilsListener = onBluetoothUtilsListener
    }

    private fun Activity.showAlertDialog(msg: String?) {
        MainScope().launch {
            val dialog = AlertDialog.Builder(this@showAlertDialog)
            dialog.setTitle("R.string.dialog_tip")
            dialog.setMessage(msg)
            dialog.setNegativeButton("R.string.dialog_back", null)
            dialog.show()
        }
    }

    init {
        printerFactory = ThermalPrinterFactory()
        rtPrinterKotlin = printerFactory?.create() as RTPrinter<BluetoothEdrConfigBean>?
        rtPrinterKotlin?.setPrinterInterface(curPrinterInterface)
        PrinterObserverManager.getInstance().add(this)
    }

    fun isBluetoothConnected(): Boolean {
        return tvDeviceSelectedTag != BaseEnum.NO_DEVICE
    }


    @SuppressLint("MissingPermission")
    fun connectedBluetoothDevice(device: BluetoothDevice){
        configObj = BluetoothEdrConfigBean(device)
        tvDeviceSelectedTag = BaseEnum.HAS_DEVICE
        printEnable = isInConnectList(configObj as BluetoothEdrConfigBean)
        tvDeviceSelected = if (TextUtils.isEmpty(device.name)) {
            device.address
        } else {
            device.name + " [" + device.address + "]"
        }
        doConnect{

        }
    }



    private fun isInConnectList(configObj: Any): Boolean {
        var isInList = false
        for (i in printerInterfaceArrayList.indices) {
            val printerInterface = printerInterfaceArrayList[i]
            if (configObj.toString() == printerInterface.configObject.toString()) {
                if (printerInterface.connectState == ConnectStateEnum.Connected) {
                    isInList = true
                    break
                }
            }
        }
        return isInList
    }

    override fun printerObserverCallback(printerInterface: PrinterInterface<*>, state: Int) {
       MainScope().launch {

           when (state) {
               CommonEnum.CONNECT_STATE_SUCCESS -> {
                   Toast.makeText(mActivity , printerInterface.configObject.toString() + "_main_connected" , Toast.LENGTH_LONG).show()
                   tvDeviceSelected = printerInterface.configObject.toString()
                   tvDeviceSelectedTag = BaseEnum.HAS_DEVICE
                   curPrinterInterface = printerInterface //设置为当前连接， set current Printer Interface
                   printerInterfaceArrayList.add(printerInterface) //多连接-添加到已连接列表
                   rtPrinterKotlin!!.setPrinterInterface(printerInterface)
                   //  BaseApplication.getInstance().setRtPrinter(rtPrinter);
                   onBluetoothUtilsListener?.onBluetoothUtilsListener(true)

               }
               CommonEnum.CONNECT_STATE_INTERRUPTED -> {
                   if (printerInterface.configObject != null) {
                      // mActivity!!.kuToast(printerInterface.configObject.toString() +mActivity!!.getString(R.string._main_disconnect))
                   } else {
                       // mActivity!!.kuToast(mActivity!!.getString(R.string._main_disconnect))
                   }
                   tvDeviceSelectedTag= BaseEnum.NO_DEVICE
                   curPrinterInterface = null
                   printerInterfaceArrayList.remove(printerInterface) //多连接-从已连接列表中移除
                   //  BaseApplication.getInstance().setRtPrinter(null);
                   onBluetoothUtilsListener?.onBluetoothUtilsListener(false)
               }
               else -> {}
           }
       }
    }

    override fun printerReadMsgCallback(printerInterface: PrinterInterface<*>?, bytes: ByteArray?) {
        MainScope().launch {
            val statusBean = PrinterStatusPareseUtils.parsePrinterStatusResult(bytes)
            if (statusBean.printStatusCmd == PrintStatusCmd.cmd_PrintFinish) {
                if (statusBean.blPrintSucc) {
                    Toast.makeText(mActivity ,  "print ok" , Toast.LENGTH_LONG).show()

                } else {
                    Toast.makeText(mActivity , PrinterStatusPareseUtils.getPrinterStatusStr(statusBean) , Toast.LENGTH_LONG).show()

                }
            } else if (statusBean.printStatusCmd == PrintStatusCmd.cmd_Normal) {
                Toast.makeText(mActivity ,  "print status：" , Toast.LENGTH_LONG).show()

            }
        }
    }

    /************* doConnect ****************/
    fun doConnect(visibility : (Boolean) -> Unit) {
        if (tvDeviceSelectedTag == BaseEnum.NO_DEVICE) {
            mActivity!!.showAlertDialog("mActivity!!.getString(R.string.main_pls_choose_device)")
            visibility(false)
            return
        }
        visibility(true)
        val bluetoothEdrConfigBean = configObj as BluetoothEdrConfigBean
        iPrintTimes = 0
        connectBluetooth(bluetoothEdrConfigBean)
    }

    /************* connectBluetooth ****************/
    private fun connectBluetooth(bluetoothEdrConfigBean: BluetoothEdrConfigBean) {
        val piFactory: PIFactory = BluetoothFactory()
        val printerInterface = piFactory.create()
        printerInterface.configObject = bluetoothEdrConfigBean
        rtPrinterKotlin!!.setPrinterInterface(printerInterface)
        try {
            rtPrinterKotlin!!.connect(bluetoothEdrConfigBean)
        } catch (e: Exception) {
            e.printStackTrace()
            e.message?.let { Log.i(TAG, it) }
        } finally {
        }
    }

    /************* doDisConnect ****************/
    fun doDisConnect() {
        if (tvDeviceSelectedTag == BaseEnum.NO_DEVICE) return
        if (rtPrinterKotlin != null && rtPrinterKotlin!!.getPrinterInterface() != null) {
            rtPrinterKotlin!!.disConnect()
        }
        tvDeviceSelected = "mActivity!!.getString(R.string.tip_have_no_found_bluetooth_device)"
        tvDeviceSelectedTag = BaseEnum.NO_DEVICE
    }

    /************* printEscCommand ****************/
    fun printEscCommand(mBitmap : Bitmap?) {
        if (mBitmap != null){
            val cmd = EscFactory().create()
            cmd.append(cmd.headerCmd) // header

            cmd.chartsetName = "UTF-8"

            val commonSetting = CommonSetting()
            commonSetting.align = CommonEnum.ALIGN_MIDDLE;

            val bitmapSetting = BitmapSetting()
            bitmapSetting.bmpPrintMode = BmpPrintMode.MODE_SINGLE_COLOR
            bitmapSetting.bimtapLimitWidth = mBitmap.width


            try {
                cmd.append(cmd.getBitmapCmd(bitmapSetting, mBitmap)) // bitmap
            } catch (e: SdkException) {
                e.printStackTrace()
                return
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }
            cmd.append(cmd.lfcrCmd)
            cmd.append(cmd.lfcrCmd)
            cmd.append(cmd.lfcrCmd)

            try {
                if (rtPrinterKotlin != null|| cmd.appendCmds != null  || cmd.appendCmds.isNotEmpty()) {
                    rtPrinterKotlin?.writeMsg(cmd.appendCmds) //Sync Write

                    printerCallbackListener?.onPrintFinished(ResultOfPrint(true, "تمت الطباعة"))
                }else{

                    printerCallbackListener?.onPrintFinished(ResultOfPrint(false, "لم تتم الطباعة , لا يوجد بيانات للطباعة او الطباعة غير متصلة"))
                }
            } catch (e: SdkException) {

                e.printStackTrace()
                printerCallbackListener?.onPrintFinished(ResultOfPrint(false, "لم تتم الطباعة , لا يوجد بيانات للطباعة او الطباعة غير متصلة"))
            } catch (e: Exception) {

                e.printStackTrace()
                printerCallbackListener?.onPrintFinished(ResultOfPrint(false, "لم تتم الطباعة , لا يوجد بيانات للطباعة او الطباعة غير متصلة"))
            }


        }else{
            printerCallbackListener?.onPrintFinished(ResultOfPrint(false, "لم تتم الطباعة , لا يوجد بيانات للطباعة او الطباعة غير متصلة"))
        }
    }

    interface PrinterCallback {
        fun onPrintFinished(result: ResultOfPrint)
    }

    data class ResultOfPrint(val result: Boolean, val msg: String)


    /************* printEscCommand ****************/

    fun applyDimension(unit: Int, value: Float, metrics: DisplayMetrics): Float {
        return when (unit) {
            TypedValue.COMPLEX_UNIT_PX -> value
            TypedValue.COMPLEX_UNIT_DIP -> value * metrics.density
            TypedValue.COMPLEX_UNIT_SP -> value * metrics.scaledDensity
            TypedValue.COMPLEX_UNIT_PT -> value * metrics.xdpi * (1.0f / 72)
            TypedValue.COMPLEX_UNIT_IN -> value * metrics.xdpi
            TypedValue.COMPLEX_UNIT_MM -> value * metrics.xdpi * (1.0f / 25.4f)
            else -> 0f
        }
    }

    private fun convertCmToPx(cm: Float, metrics: DisplayMetrics): Int {
        return (cm * metrics.xdpi * (1.0f / 2.54f)).toInt()
    }

    private fun convertMmToPx(mm: Float, metrics: DisplayMetrics): Int {
        return (mm * metrics.xdpi * (1.0f / 25.4f)).toInt()
    }

    private fun convertInToPx(inch: Float, metrics: DisplayMetrics):Int{
        return (inch * metrics.xdpi).toInt()
    }

    fun convertToPx(unit: Int, value: Float, metrics: DisplayMetrics):Int{
        return when(unit){
            3 -> convertMmToPx(value, metrics)
            2 -> convertInToPx(value, metrics)
            else -> convertCmToPx(value, metrics)
        }
    }




}