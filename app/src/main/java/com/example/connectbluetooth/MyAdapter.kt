package com.example.connectbluetooth

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.ArrayList

class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    private var itemList: ArrayList<MyItem> = arrayListOf()

    fun add(itemList: ArrayList<MyItem>){
        this.itemList = itemList
    }

    private val countDownTimers = mutableMapOf<Int, CountDownTimer?>()
    var onListSizeChanged: (Int) -> Unit = { _ -> }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.tvName)
        val countdownTextView: TextView = itemView.findViewById(R.id.tvCounter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_counter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = itemList[position]
        holder.titleTextView.text = currentItem.title

        val endDate = currentItem.endDate
        val currentDate = Calendar.getInstance().time
        val timeDiff = endDate.time - currentDate.time

        val existingTimer = countDownTimers[position]

        if (existingTimer == null && timeDiff > 0) {
            val countDownTimer = object : CountDownTimer(timeDiff, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val days = millisUntilFinished / (1000 * 60 * 60 * 24)
                    val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                    val minutes = (millisUntilFinished / (1000 * 60)) % 60
                    val seconds = (millisUntilFinished / 1000) % 60

                    holder.countdownTextView.text = String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds)
                }

                override fun onFinish() {
                    val indexToRemove = itemList.indexOf(currentItem)
                    if (indexToRemove != -1) {
                        itemList.removeAt(indexToRemove)
                        countDownTimers.remove(position)
                        onListSizeChanged(itemList.size)
                        notifyItemRemoved(indexToRemove)
                    }

                    val itemsToRemove = mutableListOf<MyItem>()
                    for (item in itemList) {
                        val countdownText = holder.countdownTextView.text.toString()
                        if (countdownText == "COUNTER") {
                            itemsToRemove.add(item)
                        }
                    }

                    if (itemsToRemove.isNotEmpty()){
                        itemList.removeAll(itemsToRemove.toSet())
                        notifyDataSetChanged()
                    }

                }
            }

            countDownTimers[position] = countDownTimer
            countDownTimer.start()
        } else if (existingTimer != null && timeDiff > 0) {
            existingTimer.start()
        } else {
            holder.countdownTextView.text = "Finished"
            Handler(Looper.getMainLooper()).post {
                val indexToRemove = itemList.indexOf(currentItem)
                if (indexToRemove != -1) {
                    itemList.removeAt(indexToRemove)
                    countDownTimers.remove(position)
                    onListSizeChanged(itemList.size)
                    notifyDataSetChanged() // update the adapter data
                }

                val itemsToRemove = mutableListOf<MyItem>()
                for (item in itemList) {
                    val countdownText = holder.countdownTextView.text.toString()
                    if (countdownText == "COUNTER") {
                        itemsToRemove.add(item)
                    }
                }

                if (itemsToRemove.isNotEmpty()){
                    itemList.removeAll(itemsToRemove.toSet())
                    notifyDataSetChanged()
                }


            }

        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            val countDownTimer = countDownTimers[position]
            countDownTimer?.cancel()
            countDownTimers[position] = null
        }
    }
}


data class MyItem(var title : String ? = null, var endDate : Date )