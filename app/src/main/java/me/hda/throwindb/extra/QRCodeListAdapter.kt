package me.hda.throwindb.extra

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.hda.throwindb.QRCodeData
import me.hda.throwindb.R
import org.json.JSONObject

class QRCodeListAdapter(
    private val onClick: (QRCodeData) -> Unit
) : ListAdapter<QRCodeData, QRCodeListViewHolder>(QRCodeListDiffCallback) {

    private var fullList: List<QRCodeData> = listOf()
    private var filteredList: List<QRCodeData> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QRCodeListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.qrcode_list_item, parent, false)
        return QRCodeListViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: QRCodeListViewHolder, position: Int) {
        val qrCodeData = filteredList[position]
        holder.bind(qrCodeData)
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    // Method to set the full list initially
    fun setFullList(list: List<QRCodeData>) {
        fullList = list
        filteredList = list
        notifyDataSetChanged()
    }

    // Filter the list based on the query
    fun filter(query: String?) {
        filteredList = if (query.isNullOrEmpty()) {
            fullList // Restore the original full list
        } else {
            fullList.filter {
                val jsonObject = JSONObject(it.data)
                jsonObject.getString("text").contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
}

class QRCodeListViewHolder(
    itemView: View, val onClick: (QRCodeData) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    private val qrCodeListItemTextView: TextView = itemView.findViewById(R.id.qrcode_data)
    private val qrCodeListItemImageView: ImageView = itemView.findViewById(R.id.qrcode_image)
    private var currentQrCodeData: QRCodeData? = null

    init {
        itemView.setOnClickListener {
            currentQrCodeData?.let {
                onClick(it)
            }
        }
    }

    fun bind(qrCodeData: QRCodeData) {
        currentQrCodeData = qrCodeData
        val jObject = JSONObject(qrCodeData.data)

        qrCodeListItemTextView.text = jObject.getString("text")

        if (jObject.has("photoUrl")) {
            qrCodeListItemImageView.setImageURI(Uri.parse(jObject.getString("photoUrl")))
        } else {
            qrCodeListItemImageView.setImageBitmap(qrCodeData.bitmap)
        }
    }
}

object QRCodeListDiffCallback : DiffUtil.ItemCallback<QRCodeData>() {
    override fun areItemsTheSame(oldItem: QRCodeData, newItem: QRCodeData): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: QRCodeData, newItem: QRCodeData): Boolean {
        return oldItem.data == newItem.data
    }
}
