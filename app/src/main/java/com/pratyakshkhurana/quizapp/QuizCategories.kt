package com.pratyakshkhurana.quizapp

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pratyakshkhurana.quizapp.databinding.ActivityQuizCategoriesBinding
import kotlinx.coroutines.launch

class QuizCategories : AppCompatActivity(), OnClicked {
    private lateinit var binding: ActivityQuizCategoriesBinding
    private lateinit var categoryList: ArrayList<CategoryView>
    private lateinit var userName: String
    private lateinit var categorySelected: String
    private lateinit var builder: AlertDialog.Builder
    private lateinit var alertDialog: AlertDialog
    private lateinit var quizRepository: FirebaseQuizRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userName = intent.getStringExtra("user").toString()
        quizRepository = FirebaseQuizRepository()

        // Показываем индикатор загрузки
        binding.progressBar.visibility = View.VISIBLE

        // Устанавливаем LayoutManager для RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        loadCategoriesFromFirebase()
    }

    private fun loadCategoriesFromFirebase() {
        lifecycleScope.launch {
            try {
                // Раскомментируйте эту строку для первичного заполнения базы данных
                // quizRepository.initializeQuizData()
                
                // Получаем список категорий из Firebase
                val categories = quizRepository.getCategories()
                
                if (categories.isNotEmpty()) {
                    // Создаем список CategoryView моделей для адаптера
                    categoryList = createCategoryViewsFromStrings(categories)
                    
                    // Устанавливаем адаптер для RecyclerView
                    binding.recyclerView.adapter = CategoriesAdapter(categoryList, this@QuizCategories)
                    
                    // Скрываем индикатор загрузки
                    binding.progressBar.visibility = View.GONE
                } else {
                    Toast.makeText(this@QuizCategories, 
                        "Қате: Категориялар табылмады", 
                        Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@QuizCategories, 
                    "Категорияларды жүктеу кезінде қате: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun createCategoryViewsFromStrings(categories: List<String>): ArrayList<CategoryView> {
        val data = ArrayList<CategoryView>()
        
        // Массив drawable ресурсов для фона
        val backgrounds = arrayOf(
            R.drawable.bg1,
            R.drawable.bg2,
            R.drawable.bg3,
            R.drawable.bg4
        )
        
        // Массив drawable ресурсов для иконок
        val icons = arrayOf(
            R.drawable.book_stack,
            R.drawable.brain,
            R.drawable.history,
            R.drawable.science,
            R.drawable.quiz
        )
        
        categories.forEachIndexed { index, category ->
            // Выбираем фон и иконку циклически из доступных ресурсов
            val bgDrawable = ResourcesCompat.getDrawable(
                resources, 
                backgrounds[index % backgrounds.size], 
                null
            )
            
            val iconDrawable = ResourcesCompat.getDrawable(
                resources, 
                icons[index % icons.size], 
                null
            )
            
            // Создаем CategoryView объект и добавляем в список, если оба drawable не null
            if (bgDrawable != null && iconDrawable != null) {
                data.add(CategoryView(R.drawable.bg, category, bgDrawable, iconDrawable))
            }
        }
        
        return data
    }

    // Вызывается при клике на категорию
    override fun categoryClicked(s: CategoryView) {
        showDialog(s)
    }

    // Диалог для выбора категории викторины
    private fun showDialog(s: CategoryView) {
        val dialogLayout = layoutInflater.inflate(R.layout.category_dialog, null)
        builder = AlertDialog.Builder(this)
        dialogLayout.findViewById<View>(R.id.okButton).setOnClickListener {
            categorySelected = s.category
            alertDialog.dismiss()
            val intent = Intent(this, QuestionsActivity::class.java)
            intent.putExtra("user", userName)
            intent.putExtra("category", categorySelected)
            startActivity(intent)
        }
        dialogLayout.findViewById<View>(R.id.cancelButton).setOnClickListener {
            alertDialog.dismiss()
        }
        builder.setView(dialogLayout)
        alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }
}