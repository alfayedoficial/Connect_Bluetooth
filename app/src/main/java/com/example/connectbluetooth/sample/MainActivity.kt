package com.example.connectbluetooth.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import com.example.connectbluetooth.*
import com.example.connectbluetooth.databinding.LyPrintShortInvoiceReceiptSewooBinding

fun Context.createBitmapForShortInvoice():Bitmap? = try {

    val mInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val mView = RelativeLayout(this)

    // here check for printer

    val dataBinder = DataBindingUtil.inflate(mInflater, R.layout.ly_print_short_invoice_receipt_sewoo , mView, true) as LyPrintShortInvoiceReceiptSewooBinding


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
