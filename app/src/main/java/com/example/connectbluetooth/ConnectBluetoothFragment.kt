package com.example.connectbluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bxl.config.editor.BXLConfigLoader
import com.example.connectbluetooth.sample.BluetoothUtils
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit


class ConnectBluetoothFragment : DialogFragment() , BluetoothUtils.OnBluetoothUtilsListener{

    private var action : Action = Action.ACTION

    private var _pairedDeviceList: MutableList<BluetoothDevice> = arrayListOf()
    private val _foundDeviceList: MutableList<BluetoothDevice> = arrayListOf()
    private val _adapterPairedDevice: BluetoothDeviceAdapter by lazy {BluetoothDeviceAdapter()}
    private val _adapterAvailableDevice: BluetoothDeviceAdapter by lazy {BluetoothDeviceAdapter()}

    private lateinit var _tvNoPairedDevices : TextView
    private lateinit var _tvNoAvailableDevices : TextView
    private lateinit var _rvPairedDevices : RecyclerView
    private lateinit var _rvAvailableDevices : RecyclerView
    private lateinit var _tvBluetoothName : TextView
    private lateinit var _switchEnableBluetooth : SwitchCompat
    private lateinit var _imgClose : ShapeableImageView
    private lateinit var _pbBluetoothSearchForDevices : ShapeableImageView
    private lateinit var _progressBarDialog : ProgressBar
    private lateinit var _viewDialog : View


    private lateinit var _mBluetoothAdapter : BluetoothAdapter
    private var _mRegistered = false
    private var _logicalName = ""
    private val _portType = BXLConfigLoader.DEVICE_BUS_BLUETOOTH
    private var _device: BluetoothDevice? = null


    companion object{
        fun create(action :Action = Action.ACTION) = ConnectBluetoothFragment().apply {
            this.action = action
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val _view =  inflater.inflate(R.layout.fragment_connect_bluetooth, container, false)
        return _view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _mBluetoothAdapter = if(Build.VERSION.SDK_INT >= 31) {
            val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }else{
            BluetoothAdapter.getDefaultAdapter()
        }

        MainActivity.BLUETOOTH_UTILS.setOnBluetoothUtilsListener(this)


        view.initViews()
        initData()
        clickableActions()


    }

    private fun View.initViews() {
        _tvNoPairedDevices = findViewById(R.id.tvNoPairedDevices)
        _tvNoAvailableDevices = findViewById(R.id.tvNoAvailableDevices)
        _tvBluetoothName = findViewById(R.id.tvBluetoothName)
        _switchEnableBluetooth = findViewById(R.id.switchEnableBluetooth)
        _imgClose = findViewById(R.id.imgClose)
        _pbBluetoothSearchForDevices = findViewById(R.id.pbBluetoothSearchForDevices)
        _rvPairedDevices = findViewById(R.id.rvBluetoothPairedDevices)
        _rvAvailableDevices = findViewById(R.id.rvBluetoothAvailableDevices)

        _progressBarDialog = findViewById(R.id.progressBarDialog)
        _viewDialog = findViewById(R.id.viewDialog)

        _rvPairedDevices.adapter = _adapterPairedDevice
        _rvAvailableDevices.adapter = _adapterAvailableDevice

        _adapterPairedDevice.onClickItem = {
            _viewDialog.visibility = View.VISIBLE
            _progressBarDialog.visibility = View.VISIBLE
            checkTypes(it)
        }

        _adapterAvailableDevice.onClickItem = {
            _viewDialog.visibility = View.VISIBLE
            _progressBarDialog.visibility = View.VISIBLE
            checkTypes(it)
        }


        val modelList = findViewById<Spinner>(R.id.spinnerModelList)

        val modelAdapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(context, R.array.modelList, android.R.layout.simple_spinner_dropdown_item)
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelList.adapter = modelAdapter
        modelList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                _logicalName = parent.getItemAtPosition(position) as String
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


    }


    enum class PrinterName(val value : String){
        MPD31D("MPD31D"),
        SW_6EAD("SW_6EAD"),
        SPP200II("SPP-R200II"),
        SPP200III("SPP-R200III"),
    }

    enum class PrinterBrand(val value : String){
        HONEYWELL("Honeywell"),
        SEWOO("SEWOO"),
        BIXOLON("Bixolon"),
    }

    @SuppressLint("MissingPermission")
    private fun checkTypes(device: BluetoothDevice) {
        _device = device
       MainScope().launch(Dispatchers.IO) {
           MainActivity.getPrinterInstance()?.printerClose()
           if ((device.name.contains(PrinterName.SPP200III.value) || device.name.contains(PrinterName.SPP200II.value))){
               if (_logicalName == PrinterBrand.BIXOLON.value){
                   if (MainActivity.getPrinterInstance()?.printerOpen(_portType, _logicalName, device.address, true) == true){
                       MainScope().launch(Dispatchers.Main) {
                           _viewDialog.visibility = View.GONE
                           _progressBarDialog.visibility = View.GONE
                           stopDiscovery()
                           action.action(device , _logicalName)
                           dismiss()
                       }
                   }
               }else{
                  showToastError("Please select [${PrinterBrand.BIXOLON.value}] model of printer and try again.")
               }
           }
           else if ((device.name.contains(PrinterName.MPD31D.value) || device.name.contains(PrinterName.SW_6EAD.value))){
               if (_logicalName == PrinterBrand.HONEYWELL.value || _logicalName == PrinterBrand.SEWOO.value) {
                   MainActivity.BLUETOOTH_UTILS.connectedBluetoothDevice(device)
               }else{
                   if (device.name.contains(PrinterName.MPD31D.value)){
                       showToastError("Please select [${PrinterBrand.HONEYWELL.value}] model of printer and try again.")
                   }else{
                       showToastError("Please select [${PrinterBrand.SEWOO.value}] model of printer and try again.")
                   }
               }
           }else{
               showToastError("You can't connect printer,\n Please select correct model of printer and try again.")
           }
       }
    }

    fun showToastError(msg : String){
        MainScope().launch(Dispatchers.Main) {
            _viewDialog.visibility = View.GONE
            _progressBarDialog.visibility = View.GONE
            Toast.makeText(context , msg, Toast.LENGTH_SHORT).show()
        }
    }



    private fun clickableActions() {
        _imgClose.setOnClickListener {dismiss()}
        _switchEnableBluetooth.setOnCheckedChangeListener { _, isChecked -> setBluetooth(isChecked)  }
        _pbBluetoothSearchForDevices.setOnClickListener { searchAvailableBluetoothDevices() }
    }

    @SuppressLint("MissingPermission")
    private fun setBluetooth(enable: Boolean) {
        val isEnabled = _mBluetoothAdapter.isEnabled
        if (enable && !isEnabled) {
            _switchEnableBluetooth.isChecked = true
            _mBluetoothAdapter.enable()
            try {
                TimeUnit.MILLISECONDS.sleep(500)
                initData()
            } catch (_: Exception) { }
        } else if (!enable && isEnabled) {
            _switchEnableBluetooth.isChecked = false
            _adapterPairedDevice.setListDevices(list = arrayListOf())
            _tvNoPairedDevices.visibility = View.VISIBLE
            _rvPairedDevices.visibility = View.GONE
            _mBluetoothAdapter.disable()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initData() {
        _tvBluetoothName.text = _mBluetoothAdapter.name

        if (!_mBluetoothAdapter.isEnabled) {
            _switchEnableBluetooth.isChecked = false
            Toast.makeText(requireContext(), "من فضلك قم بفتح البلوتوث", Toast.LENGTH_LONG).show()
        } else {
            _switchEnableBluetooth.isChecked = true
            _pairedDeviceList = ArrayList()
            val bondedDevicesList: List<BluetoothDevice> = ArrayList<BluetoothDevice>(_mBluetoothAdapter.bondedDevices)
            for (i in bondedDevicesList.indices) {
                if (bondedDevicesList[i].bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.IMAGING) {
                    _pairedDeviceList.add(bondedDevicesList[i])
                }
            }
            if (_pairedDeviceList.size == 0) {
                _tvNoPairedDevices.visibility = View.VISIBLE
                _rvPairedDevices.visibility = View.GONE
            } else {
                _tvNoPairedDevices.visibility = View.GONE
                _rvPairedDevices.visibility = View.VISIBLE
            }
            _adapterPairedDevice.setListDevices(_pairedDeviceList)


        }

    }

    @SuppressLint("MissingPermission")
    private fun searchAvailableBluetoothDevices() {

        if (_pbBluetoothSearchForDevices.drawable is GifDrawable ){
            val b = !(_pbBluetoothSearchForDevices.drawable as GifDrawable).isRunning
            if (b && !_switchEnableBluetooth.isChecked) _switchEnableBluetooth.isChecked = true
            stopDiscoveryGif(b)
        }else{
            Glide.with(requireContext()).asGif().load(R.drawable.reload2)
                .into(_pbBluetoothSearchForDevices)
            if (!_switchEnableBluetooth.isChecked) _switchEnableBluetooth.isChecked = true
        }
        _foundDeviceList.clear()
        _adapterAvailableDevice.setListDevices(_foundDeviceList)
        registerBluetoothReceiver()
        _mBluetoothAdapter.startDiscovery()
    }

    private fun stopDiscoveryGif(isEnabled: Boolean) {
        if (_pbBluetoothSearchForDevices.drawable is GifDrawable) {
            val drawable = _pbBluetoothSearchForDevices.drawable as GifDrawable
            if (isEnabled) drawable.start()
            else drawable.stop()
        }
    }

    private fun registerBluetoothReceiver() {
        try {
            val intentFilter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            }
            context?.registerReceiver(_bluetoothDeviceReceiver, intentFilter)
            _mRegistered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private var _bluetoothDeviceReceiver = object : BroadcastReceiver(){
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {

                    // Get the BluetoothDevice object from the Intent
                    val device = getParcelableExtra(intent ,BluetoothDevice.EXTRA_DEVICE , BluetoothDevice::class.java)
                    val devType = device?.bluetoothClass?.majorDeviceClass
                    if (devType != BluetoothClass.Device.Major.IMAGING) {
                        return
                    }

                    if (!_foundDeviceList.contains(device)) {
                        _foundDeviceList.add(device)
                        _adapterAvailableDevice.setListDevices(_foundDeviceList)
                    }

                    checkSizeAvailableDevices()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    stopDiscovery()
                    _mRegistered = false
                    stopDiscoveryGif(false)
                    checkSizeAvailableDevices()
                }
            }

        }

    }

    fun checkSizeAvailableDevices(){
        if (_foundDeviceList.isEmpty()){
            _rvAvailableDevices.visibility = View.GONE
            _tvNoAvailableDevices.visibility = View.VISIBLE
        }else{
            _rvAvailableDevices.visibility = View.VISIBLE
            _tvNoAvailableDevices.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery(){
       try {
           _mBluetoothAdapter.cancelDiscovery()
           if (_mRegistered){
               context?.unregisterReceiver(_bluetoothDeviceReceiver)
           }
       }catch (_:Exception){}
    }

    @SuppressLint("MissingPermission")
    fun handleDeviceSelected(device: BluetoothDevice, callback : (Boolean) -> Unit) {
        if (device.bluetoothClass.majorDeviceClass == DevicesTypes.IMAGING.type) {
            if (device.bondState == BluetoothDevice.BOND_NONE) {
                CoroutineScope(Dispatchers.IO).launch {
                    if (!device.createBond()) withContext(Dispatchers.Main) {
                        Toast.makeText(context , "Could\\'t Pair With This Device…" , Toast.LENGTH_LONG).show()
                    }
                }
                callback(true)
            } else if (device.bondState == BluetoothDevice.BOND_BONDED) {
                callback(true)
            }

        } else {
            callback(false)
        }

    }

    data class Action(var action:(BluetoothDevice , String)->Unit){
        companion object{
            val ACTION = Action{_ , _ ->}
        }
    }

    override fun onStart() {
        super.onStart()
        setWindowParams()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        stopDiscovery()
    }

    override fun onBluetoothUtilsListener(enable: Boolean) {
        MainScope().launch(Dispatchers.Main) {
                _progressBarDialog.visibility = View.GONE
                _viewDialog.visibility = View.GONE
            if (enable){
                action.action(_device!! , _logicalName)
                dismiss()
            }else{
                Toast.makeText(context , "You can't connect printer, please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }


}