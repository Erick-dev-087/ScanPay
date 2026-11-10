package com.scanpay.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.scanpay.app.R
import com.scanpay.app.ScanPayApplication
import com.scanpay.app.data.response.TransactionResponse
import com.scanpay.app.databinding.ItemTransactionBinding
import com.scanpay.app.utils.CurrencyUtils
import java.time.*
import java.time.format.DateTimeFormatter

class TransactionAdapter(private val onItemClick: ((TransactionResponse) -> Unit)? = null) :
    RecyclerView.Adapter<TransactionAdapter.Holder>() {
    private var rows = emptyList<TransactionResponse>()
    fun updateList(values: List<TransactionResponse>) { rows = values; notifyDataSetChanged() }
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(rows[position])
    inner class Holder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tx: TransactionResponse) {
            val merchant = ScanPayApplication.sessionManager.isMerchant
            val name = (if (merchant) tx.customerName else tx.merchantName) ?: "M-Pesa payment"
            val date = runCatching { Instant.parse(tx.initiatedAt).atZone(ZoneId.of("Africa/Nairobi"))
                .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm")) }.getOrDefault("")
            binding.tvMerchantName.text = "$name\n$date · ${tx.status}\n${tx.mpesaReceipt ?: tx.accountReference.orEmpty()}"
            binding.tvAmount.text = (if (merchant) "+ " else "− ") + CurrencyUtils.formatKes(tx.amount)
            binding.tvAmount.setTextColor(binding.root.context.getColor(
                if (tx.status == "success") R.color.ui_secondary else R.color.ui_on_surface_variant))
            binding.root.setOnClickListener { onItemClick?.invoke(tx) }
        }
    }
}

