package com.example.connectbluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BluetoothDeviceAdapter : RecyclerView.Adapter<BluetoothDeviceAdapter.BluetoothDeviceHolder>() {

    var onClickItem: (BluetoothDevice) -> Unit = {_ -> }

    private var devices: MutableList<BluetoothDevice> = arrayListOf()

    @SuppressLint("NotifyDataSetChanged")
    fun setListDevices(list: MutableList<BluetoothDevice>){
        this.devices = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothDeviceHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rv_bluetooth_device, parent, false)
        return BluetoothDeviceHolder(view)
    }

    override fun getItemCount(): Int = devices.size

    override fun onBindViewHolder(holder: BluetoothDeviceHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    inner class BluetoothDeviceHolder(var view:View) : RecyclerView.ViewHolder(view){

        private lateinit var tvDeviceName : TextView
        @SuppressLint("MissingPermission")
        fun bind(device: BluetoothDevice){
            tvDeviceName = view.findViewById(R.id.tvDeviceName)
            tvDeviceName.text = if (!device.name.isNullOrEmpty()){
                device.name + " [" + device.address +"]"
            }else{
                device.address
            }
            tvDeviceName.setCompoundDrawablesRelativeWithIntrinsicBounds(getDeviceDrawable(device.bluetoothClass.majorDeviceClass), 0, 0, 0)

            itemView.setOnClickListener {onClickItem(device)}
        }
    }

}