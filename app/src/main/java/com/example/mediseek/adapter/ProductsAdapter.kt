package com.example.mediseek.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
// Import YOUR Product data class
import com.example.mediseek.model.Product // <--- Make sure this import is correct

// Remove or comment out the Google Analytics product import if it exists:
// import com.google.android.gms.analytics.ecommerce.Product <--- REMOVE THIS IF PRESENT

class ProductsAdapter(private var products: List<Product>) : // <--- CHANGE THIS LINE
    RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImageView: ImageView = itemView.findViewById(R.id.iv_product_image)
        val productNameTextView: TextView = itemView.findViewById(R.id.tv_product_name)
        val stockStatusTextView: TextView = itemView.findViewById(R.id.tv_stock_status)
        val productPriceTextView: TextView = itemView.findViewById(R.id.tv_product_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.products_list, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentProduct = products[position] // currentProduct is now of type com.example.mediseek.models.Product

        // This should now resolve correctly:
        holder.productImageView.setImageResource(currentProduct.imageName)

        // If you were planning to use imageUrl for Glide, it would be:
        // if (currentProduct.imageUrl != null) { // Assuming your Product model might have imageUrl
        //     Glide.with(holder.itemView.context)
        //        .load(currentProduct.imageUrl)
        //        .placeholder(R.drawable.ordersimg)
        //        .error(R.drawable.ic_error_placeholder)
        //        .into(holder.productImageView)
        // } else {
        //     holder.productImageView.setImageResource(R.drawable.ordersimg) // Fallback if no imageUrl
        // }

        holder.productNameTextView.text = currentProduct.name
        holder.stockStatusTextView.text = currentProduct.stockStatus
        holder.productPriceTextView.text = currentProduct.price

        // Optional: Dynamically change stock status text color
        if (currentProduct.stockStatus.equals("IN STOCK", ignoreCase = true)) {
            holder.stockStatusTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green))
        } else {
            holder.stockStatusTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.red))
        }
    }

    override fun getItemCount() = products.size

    // This function also needs to accept your custom Product type
    fun updateData(newProducts: List<Product>) { // <--- And this line
        products = newProducts
        notifyDataSetChanged() // Consider using DiffUtil for better performance
    }
}
