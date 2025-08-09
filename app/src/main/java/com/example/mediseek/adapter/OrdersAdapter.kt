package com.example.mediseek.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.model.Order

class OrdersAdapter(
    private var orders: List<Order>,
    private val listener: OnItemClickListener // Add listener
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    // Interface to handle click events
    interface OnItemClickListener {
        fun onItemClick(orderId: String)
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val orderIdTextView: TextView = itemView.findViewById(R.id.tv_order_id)
        val orderItemsTextView: TextView = itemView.findViewById(R.id.tv_order_items)
        val orderPriceTextView: TextView = itemView.findViewById(R.id.tv_order_price)
        val orderDateTextView: TextView = itemView.findViewById(R.id.tv_order_date)
        val orderStatusTextView: TextView = itemView.findViewById(R.id.tv_order_status)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(orders[position].id) // Pass the document ID
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.orders_list, parent, false)
        return OrderViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val currentOrder = orders[position]
        holder.orderIdTextView.text = currentOrder.orderId
        holder.orderItemsTextView.text = currentOrder.itemsSummary
        holder.orderPriceTextView.text = currentOrder.totalPrice
        holder.orderDateTextView.text = currentOrder.orderDate
        holder.orderStatusTextView.text = currentOrder.status
        holder.orderStatusTextView.backgroundTintList =
            ContextCompat.getColorStateList(holder.itemView.context, currentOrder.statusBackgroundColor)
    }

    override fun getItemCount() = orders.size

    fun updateData(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
