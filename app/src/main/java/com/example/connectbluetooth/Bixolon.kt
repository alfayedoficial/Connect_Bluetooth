package com.example.connectbluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bxl.config.editor.BXLConfigLoader
import jpos.JposException
import jpos.POSPrinter
import jpos.POSPrinterConst
import jpos.config.JposEntry
import jpos.events.*
import java.nio.ByteBuffer

const val TAG = "PrintUtils"

class Bixolon(
  private val context: Context,
  private val device: BluetoothDevice,
  private val callback: PrinterCallback,
  private val check : Boolean? = null
) : ErrorListener, OutputCompleteListener, StatusUpdateListener, DirectIOListener, DataListener {
  private var bxlConfigLoader: BXLConfigLoader? = null
  private var posPrinter: POSPrinter? = null
  private var isOpen = false
  private var mPrinterLogicalName: String = ""
  private var mPrinterMacAddress: String = ""

  @SuppressLint("MissingPermission")
  private fun initBixolonPrinter() {
    try {
      posPrinter = POSPrinter(context)
      posPrinter!!.addStatusUpdateListener(this)
      posPrinter!!.addErrorListener(this)
      posPrinter!!.addOutputCompleteListener(this)
      posPrinter!!.addDirectIOListener(this)
      bxlConfigLoader = BXLConfigLoader(context)
      mPrinterLogicalName = device.name
      mPrinterMacAddress = device.address
      try {
        bxlConfigLoader!!.openFile()
      } catch (e: Exception) {
        bxlConfigLoader!!.newFile()
      }
      isOpen = try {
        printerOpen()
      } catch (e: Exception) {
        e.printStackTrace()
        if (check == true)  callback.onPrintFinished(ResultOfPrint(false, ""))
        false
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun printImage(bitmap: Bitmap?) {
    try {
      if (isOpen) {
        val buffer = ByteBuffer.allocate(4)
        buffer.put(POSPrinterConst.PTR_S_RECEIPT.toByte())
        buffer.put(50.toByte())
        buffer.put(1.toByte())
        buffer.put(0x00.toByte())
        posPrinter!!.printBitmap(buffer.getInt(0), bitmap, 600, POSPrinterConst.PTR_BM_CENTER)
        callback.onPrintFinished(ResultOfPrint(true, "تمت الطباعة"))
      } else callback.onPrintFinished(
        ResultOfPrint(false, "لم تتم الطباعة , الطباعة غير متصلة")
      )
    } catch (e: Exception) {
      e.printStackTrace()
      callback.onPrintFinished(ResultOfPrint(false, "لم تتم الطباعة , لا يوجد بيانات للطباعة او الطباعة غير متصلة"))
    }
  }

  private fun printerOpen(): Boolean {
    try {
      if (setTargetDevice()) {
        try {
          posPrinter!!.open(mPrinterLogicalName)
          posPrinter!!.claim(5000)
          posPrinter!!.deviceEnabled = true
          posPrinter!!.asyncMode = true
          if (check == true)  callback.onPrintFinished(ResultOfPrint(true, ""))
        } catch (e: JposException) {
          if (check == true)  callback.onPrintFinished(ResultOfPrint(false, ""))
          e.printStackTrace()
          try {
            posPrinter!!.close()
          } catch (e1: JposException) {
            e1.printStackTrace()
            if (check == true)  callback.onPrintFinished(ResultOfPrint(false, ""))
          }
          return false
        }
      } else {
        if (check == true)  callback.onPrintFinished(ResultOfPrint(false, ""))
        return false
      }
    } catch (e: Exception) {
      e.printStackTrace()
      return false
    }
    return true
  }

  private fun setTargetDevice(): Boolean {
    try {
      for (entry in bxlConfigLoader!!.entries) {
        val jposEntry = entry as JposEntry
        if (jposEntry.logicalName == mPrinterLogicalName) {
          bxlConfigLoader!!.removeEntry(jposEntry.logicalName)
        }
      }
      bxlConfigLoader!!.addEntry(
        mPrinterLogicalName, BXLConfigLoader.DEVICE_CATEGORY_POS_PRINTER,
        getProductName(mPrinterLogicalName), BXLConfigLoader.DEVICE_BUS_BLUETOOTH, mPrinterMacAddress)
      bxlConfigLoader!!.saveFile()
    } catch (e: Exception) {
      e.printStackTrace()
      return false
    }
    return true
  }

  private fun getProductName(name: String): String {
    var productName = BXLConfigLoader.PRODUCT_NAME_SPP_R200II
    try {
      if (name.contains("SPP-R200II")) {
        if (name.length > 10) {
          if (name.substring(10, 11) == "I") {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R200III
          }
        }
      } else if (name.contains("SPP-R210")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SPP_R210
      } else if (name.contains("SPP-R215")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SPP_R215
      } else if (name.contains("SPP-R220")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SPP_R220
      } else if (name.contains("SPP-R300")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SPP_R300
      } else if (name.contains("SPP-R310")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SPP_R310
      } else if (name.contains("SPP-R318")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SPP_R318
      } else if (name.contains("SPP-R400")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SPP_R400
      } else if (name.contains("SPP-R410")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SPP_R410
      } else if (name.contains("SPP-R418")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SPP_R418
      } else if (name.contains("SRP-350III")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_350III
      } else if (name.contains("SRP-352III")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_352III
      } else if (name.contains("SRP-350plusIII")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_350PLUSIII
      } else if (name.contains("SRP-352plusIII")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_352PLUSIII
      } else if (name.contains("SRP-380")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_380
      } else if (name.contains("SRP-382")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_382
      } else if (name.contains("SRP-383")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_383
      } else if (name.contains("SRP-340II")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_340II
      } else if (name.contains("SRP-342II")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_342II
      } else if (name.contains("SRP-Q300")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_Q300
      } else if (name.contains("SRP-Q302")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_Q302
      } else if (name.contains("SRP-QE300")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_QE300
      } else if (name.contains("SRP-QE302")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_QE302
      } else if (name.contains("SRP-E300")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_E300
      } else if (name.contains("SRP-E302")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_E302
      } else if (name.contains("SRP-330II")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_330II
      } else if (name.contains("SRP-332II")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_332II
      } else if (name.contains("SRP-S300")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_S300
      } else if (name.contains("SRP-F310II")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_F310II
      } else if (name.contains("SRP-F312II")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_F312II
      } else if (name.contains("SRP-F313II")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_F313II
      } else if (name.contains("SRP-275III")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SRP_275III
      } else if (name.contains("BK3-3")) {
        productName = BXLConfigLoader.PRODUCT_NAME_BK3_3
      } else if (name.contains("MSR")) {
        productName = BXLConfigLoader.PRODUCT_NAME_MSR
      } else if (name.contains("SmartCardRW")) {
        productName = BXLConfigLoader.PRODUCT_NAME_SMART_CARD_RW
      } else if (name.contains("CashDrawer")) {
        productName = BXLConfigLoader.PRODUCT_NAME_CASH_DRAWER
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return productName
  }

  override fun dataOccurred(dataEvent: DataEvent) {
    Log.i(TAG, "dataOccurred: ")
  }

  override fun directIOOccurred(directIOEvent: DirectIOEvent) {
    Log.i(TAG, "directIOOccurred: ")
  }

  override fun errorOccurred(errorEvent: ErrorEvent) {
    Log.i(TAG, "errorOccurred: ")
  }

  override fun outputCompleteOccurred(outputCompleteEvent: OutputCompleteEvent) {}
  override fun statusUpdateOccurred(statusUpdateEvent: StatusUpdateEvent) {
    Log.i(TAG, "statusUpdateOccurred: ")
  }


  init {
    initBixolonPrinter()
  }
}