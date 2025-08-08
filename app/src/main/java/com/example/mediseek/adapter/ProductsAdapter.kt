package com.example.mediseek.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Import Glide
import com.example.mediseek.R
import com.example.mediseek.model.Product

class ProductsAdapter(private var products: List<Product>, private val listener: OnItemClickListener ) :
    RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(productId: String)
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val productImageView: ImageView = itemView.findViewById(R.id.iv_product_image)
        val productNameTextView: TextView = itemView.findViewById(R.id.tv_product_name)
        val stockStatusTextView: TextView = itemView.findViewById(R.id.tv_stock_status)
        val productPriceTextView: TextView = itemView.findViewById(R.id.tv_product_price)

        init {
            itemView.setOnClickListener(this) // Set the click listener on the item view
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(products[position].id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.products_list, parent, false) // Ensure this layout file name is correct
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentProduct = products[position]

        // Set text data
        holder.productNameTextView.text = currentProduct.name
        holder.stockStatusTextView.text = currentProduct.stockStatus
        holder.productPriceTextView.text = currentProduct.price

        // --- FIXED: Load image from URL using Glide ---
        // This handles downloading, resizing, and caching the image automatically.
        Glide.with(holder.itemView.context)
            .load(currentProduct.imgURL) // Load the image URL (String)
            .centerCrop() // Scale the image to fit the view, cropping if needed
            .placeholder(R.drawable.ic_add_photo) // Optional: show a placeholder while loading
            .error(R.drawable.ic_add_photo) // Optional: show an error image if loading fails
            .into(holder.productImageView) // The target ImageView

        // Optional: Dynamically change stock status text color
        if (currentProduct.stockStatus.equals("IN STOCK", ignoreCase = true)) {
            holder.stockStatusTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green))
        } else {
            holder.stockStatusTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.red))
        }
    }

    override fun getItemCount() = products.size

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
