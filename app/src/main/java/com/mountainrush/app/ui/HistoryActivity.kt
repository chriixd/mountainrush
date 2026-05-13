package com.mountainrush.app.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mountainrush.app.data.AppDatabase
import com.mountainrush.app.data.RunSession
import com.mountainrush.app.databinding.ActivityHistoryBinding
import com.mountainrush.app.databinding.ItemHistoryBinding
import com.mountainrush.app.util.Formatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val adapter = HistoryAdapter(
        onClick = { run ->
            startActivity(Intent(this, RecapActivity::class.java).apply {
                putExtra(RecapActivity.EXTRA_RUN_ID, run.id)
            })
        },
        onLongClick = { run -> confirmDelete(run) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.historyList.layoutManager = LinearLayoutManager(this)
        binding.historyList.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppDatabase.get(this@HistoryActivity).runDao().observeAll().collect { list ->
                    adapter.submit(list)
                    binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun confirmDelete(run: RunSession) {
        AlertDialog.Builder(this)
            .setTitle("Delete run?")
            .setMessage(Formatter.formatDate(run.startTime))
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        AppDatabase.get(this@HistoryActivity).runDao().deleteById(run.id)
                    }
                }
            }
            .show()
    }
}

class HistoryAdapter(
    private val onClick: (RunSession) -> Unit,
    private val onLongClick: (RunSession) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    private val items = mutableListOf<RunSession>()

    fun submit(list: List<RunSession>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.b.itemDate.text = Formatter.formatDate(r.startTime)
        holder.b.itemDistance.text = "${Formatter.formatDistanceKm(r.distanceMeters)} km"
        holder.b.itemTime.text = Formatter.formatDuration(r.durationMs)
        holder.b.itemTopSpeed.text = "${Formatter.formatSpeed(r.maxSpeedKmh)} km/h"
        holder.b.itemGain.text = Formatter.formatAltitude(r.elevationGainM)
        holder.b.root.setOnClickListener { onClick(r) }
        holder.b.root.setOnLongClickListener { onLongClick(r); true }
    }

    override fun getItemCount() = items.size
}
