package com.example.mediseek.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.model.Order

class OrdersAdapter(private var orders: List<Order>) :
    RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderIdTextView: TextView = itemView.findViewById(R.id.tv_order_id)
        val orderItemsTextView: TextView = itemView.findViewById(R.id.tv_order_items)
        val orderPriceTextView: TextView = itemView.findViewById(R.id.tv_order_price)
        val orderDateTextView: TextView = itemView.findViewById(R.id.tv_order_date)
        val orderStatusTextView: TextView = itemView.findViewById(R.id.tv_order_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.orders_list, parent, false) // Use your item layout
        return OrderViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val currentOrder = orders[position]

        holder.orderIdTextView.text = currentOrder.orderId
        holder.orderItemsTextView.text = currentOrder.itemsSummary
        holder.orderPriceTextView.text = currentOrder.totalPrice
        holder.orderDateTextView.text = currentOrder.orderDate
        holder.orderStatusTextView.text = currentOrder.status

        // Set background color for status dynamically
        holder.orderStatusTextView.backgroundTintList =
            ContextCompat.getColorStateList(holder.itemView.context, currentOrder.statusBackgroundColor)
        // If you want to change text color based on status too, you can add:
        // holder.orderStatusTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.some_status_text_color))
    }

    override fun getItemCount() = orders.size

    fun updateData(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
