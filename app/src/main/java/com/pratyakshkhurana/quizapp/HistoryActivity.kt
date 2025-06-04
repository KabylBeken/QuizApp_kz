package com.pratyakshkhurana.quizapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pratyakshkhurana.quizapp.database.QuizDatabase
import com.pratyakshkhurana.quizapp.database.QuizRepository
import com.pratyakshkhurana.quizapp.database.QuizResult
import com.pratyakshkhurana.quizapp.databinding.ActivityHistoryBinding
import com.pratyakshkhurana.quizapp.databinding.HistoryItemBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var repository: QuizRepository
    private var adapter: HistoryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем репозиторий
        val dao = QuizDatabase.getDatabase(this).quizResultDao()
        repository = QuizRepository(dao)

        // Настраиваем RecyclerView
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(
            onItemDelete = { result ->
                showDeleteConfirmation(result)
            }
        )
        binding.historyRecyclerView.adapter = adapter

        // Загружаем результаты квизов
        loadQuizResults()
        
        // Обработка нажатия на кнопку "назад"
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // Обработка нажатия на кнопку "очистить историю"
        binding.clearHistoryButton.setOnClickListener {
            showClearAllConfirmation()
        }
    }

    private fun loadQuizResults() {
        lifecycleScope.launch {
            repository.getAllResults().collect { results ->
                if (results.isEmpty()) {
                    binding.noResultsText.visibility = View.VISIBLE
                    binding.historyRecyclerView.visibility = View.GONE
                    binding.clearHistoryButton.visibility = View.GONE
                } else {
                    binding.noResultsText.visibility = View.GONE
                    binding.historyRecyclerView.visibility = View.VISIBLE
                    binding.clearHistoryButton.visibility = View.VISIBLE
                    adapter?.setResults(results)
                }
            }
        }
    }
    
    private fun showDeleteConfirmation(result: QuizResult) {
        AlertDialog.Builder(this)
            .setTitle("Жазбаны жою")
            .setMessage("Бұл жазбаны жойғыңыз келе ме?")
            .setPositiveButton("Иә") { _, _ ->
                deleteResult(result)
            }
            .setNegativeButton("Жоқ", null)
            .show()
    }
    
    private fun showClearAllConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Барлық тарихты тазалау")
            .setMessage("Барлық тест тарихын тазалағыңыз келе ме? Бұл әрекетті кері қайтару мүмкін емес.")
            .setPositiveButton("Иә") { _, _ ->
                clearAllHistory()
            }
            .setNegativeButton("Жоқ", null)
            .show()
    }
    
    private fun deleteResult(result: QuizResult) {
        lifecycleScope.launch {
            try {
                repository.deleteResult(result)
                Toast.makeText(this@HistoryActivity, "Жазба жойылды", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@HistoryActivity, "Қате: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun clearAllHistory() {
        lifecycleScope.launch {
            try {
                repository.deleteAllResults()
                Toast.makeText(this@HistoryActivity, "Барлық тарих тазаланды", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@HistoryActivity, "Қате: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Адаптер для RecyclerView с историей результатов
    inner class HistoryAdapter(
        private val onItemDelete: (QuizResult) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
        private val results = mutableListOf<QuizResult>()
        private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        fun setResults(newResults: List<QuizResult>) {
            results.clear()
            results.addAll(newResults)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val binding = HistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return HistoryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val result = results[position]
            holder.bind(result)
        }

        override fun getItemCount() = results.size

        inner class HistoryViewHolder(private val binding: HistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(result: QuizResult) {
                binding.userNameTextView.text = result.userName
                binding.categoryTextView.text = result.category
                binding.scoreTextView.text = "${result.score}/${result.totalQuestions}"
                binding.dateTextView.text = dateFormat.format(Date(result.date))
                
                // Настраиваем кнопку удаления
                binding.deleteButton.setOnClickListener {
                    onItemDelete(result)
                }
            }
        }
    }
} 