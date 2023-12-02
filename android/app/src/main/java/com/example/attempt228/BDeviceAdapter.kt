package com.example.attempt228

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attempt228.databinding.BtItemBinding

class BDeviceAdapter(private val devices: ArrayList<BDevice>) : RecyclerView.Adapter<BDeviceAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var binding = BtItemBinding.bind(itemView)

        fun bind(device: BDevice) = with(binding){
            tvName.text = device.name
            tvRssi.text = "${device.rssi}"
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bt_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addDevice(device: BDevice){
        if (devices.any { it.name == device.name }){
            devices[devices.indexOf(devices.find { it.name == device.name })].rssi = device.rssi
        }else{
            devices.add(device)
        }
        notifyDataSetChanged()
    }

    /*class Comparator : DiffUtil.ItemCallback<BDevice>(){
        override fun areItemsTheSame(oldItem: BDevice, newItem: BDevice): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: BDevice, newItem: BDevice): Boolean {
            return oldItem == newItem
        }

    }*/

}