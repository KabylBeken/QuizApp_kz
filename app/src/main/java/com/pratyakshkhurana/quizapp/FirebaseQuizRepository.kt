package com.pratyakshkhurana.quizapp

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseQuizRepository {
    private val database = Firebase.database
    private val questionsRef = database.getReference("questions")
    
    /**
     * Получение всех категорий вопросов
     */
    suspend fun getCategories(): List<String> = suspendCoroutine { continuation ->
        questionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categories = snapshot.children.mapNotNull { it.key }
                continuation.resume(categories)
            }
            
            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }
    
    /**
     * Получение вопросов по категории
     */
    suspend fun getQuestionsByCategory(category: String): List<Question> = suspendCoroutine { continuation ->
        questionsRef.child(category).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val questions = snapshot.children.mapNotNull { 
                    it.getValue(Question::class.java) 
                }
                continuation.resume(questions)
            }
            
            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }
    
    /**
     * Инициализация базы данных вопросами для каждой категории на казахском языке
     * Вызывать этот метод нужно только один раз при первом запуске или для обновления вопросов
     */
    suspend fun initializeQuizData() {
        // Спорт
        val sportQuestions = listOf(
            Question(
                1,
                "Футбол қай елдің ұлттық спорты болып табылады?",
                "Бразилия",
                "Англия",
                "Испания", 
                "Аргентина",
                2
            ),
            Question(
                2,
                "Қазақстанда бұрыннан келе жатқан ұлттық спорт түрі?",
                "Көкпар",
                "Поло",
                "Гольф",
                "Теннис",
                1
            ),
            Question(
                3,
                "Олимпиада ойындары нешінші жылы басталды?",
                "1896",
                "1900",
                "1904",
                "1908",
                1
            )
        )
        
        // Тарих
        val historyQuestions = listOf(
            Question(
                1,
                "Қазақстан тәуелсіздігін қай жылы алды?",
                "1990",
                "1991",
                "1992",
                "1993",
                2
            ),
            Question(
                2,
                "Абылай хан қай жылдары өмір сүрген?",
                "1711-1781",
                "1693-1781",
                "1713-1781",
                "1700-1781",
                2
            ),
            Question(
                3,
                "Алматы қай жылға дейін Қазақстанның астанасы болды?",
                "1995",
                "1996",
                "1997",
                "1998",
                3
            )
        )
        
        // Әдебиет
        val literatureQuestions = listOf(
            Question(
                1,
                "«Абай жолы» романының авторы кім?",
                "Мұхтар Әуезов",
                "Абай Құнанбаев",
                "Сәбит Мұқанов",
                "Ілияс Жансүгіров",
                1
            ),
            Question(
                2,
                "Абайдың толық аты-жөні қалай?",
                "Абай Құнанбаев",
                "Ибраһим Құнанбайұлы",
                "Абай Құнанбайұлы",
                "Ибраһим Құнанбаев",
                2
            ),
            Question(
                3,
                "Қазақтың алғашқы романы?",
                "«Абай жолы»",
                "«Қан мен тер»",
                "«Бақытсыз Жамал»",
                "«Қилы заман»",
                3
            )
        )
        
        // Ғылым
        val scienceQuestions = listOf(
            Question(
                1,
                "Су молекуласының химиялық формуласы қандай?",
                "H2O",
                "CO2",
                "O2",
                "H2O2",
                1
            ),
            Question(
                2,
                "Менделеев кестесінде неше элемент бар?",
                "108",
                "118",
                "120",
                "116",
                2
            ),
            Question(
                3,
                "ДНҚ молекуласын қандай ғалымдар ашты?",
                "Уотсон және Крик",
                "Эйнштейн және Бор",
                "Менделеев және Ломоносов",
                "Дарвин және Мендель",
                1
            )
        )
        
        // География
        val geographyQuestions = listOf(
            Question(
                1,
                "Қазақстанның ең биік нүктесі?",
                "Хан Тәңірі шыңы",
                "Белуха тауы",
                "Талғар шыңы",
                "Мұзтау шыңы",
                3
            ),
            Question(
                2,
                "Қазақстанның ең ірі көлі?",
                "Балқаш",
                "Каспий",
                "Алакөл",
                "Зайсан",
                2
            ),
            Question(
                3,
                "Қазақстанның ең ұзын өзені?",
                "Жайық",
                "Сырдария",
                "Ертіс",
                "Іле",
                3
            )
        )
        
        // Планета - новая категория
        val planetQuestions = listOf(
            Question(
                1,
                "Күн жүйесіндегі ең үлкен планета қандай?",
                "Юпитер",
                "Сатурн",
                "Жер",
                "Нептун",
                1
            ),
            Question(
                2,
                "Жерден санағанда күн жүйесіндегі ең жақын планета қандай?",
                "Венера",
                "Марс",
                "Меркурий",
                "Юпитер",
                3
            ),
            Question(
                3,
                "Жерде неше материк бар?",
                "5",
                "6",
                "7",
                "8",
                3
            ),
            Question(
                4,
                "Жердің айналасын толық айналуға қанша уақыт кетеді?",
                "12 сағат",
                "24 сағат",
                "365 күн",
                "30 күн",
                2
            ),
            Question(
                5,
                "Күн жүйесіндегі қай планетаның сақиналары бар?",
                "Сатурн",
                "Меркурий",
                "Жер",
                "Марс",
                1
            )
        )
        
        // Загрузка данных в Firebase
        questionsRef.child("Спорт").setValue(sportQuestions.associateBy { it.id.toString() })
        questionsRef.child("Тарих").setValue(historyQuestions.associateBy { it.id.toString() })
        questionsRef.child("Әдебиет").setValue(literatureQuestions.associateBy { it.id.toString() })
        questionsRef.child("Ғылым").setValue(scienceQuestions.associateBy { it.id.toString() })
        questionsRef.child("География").setValue(geographyQuestions.associateBy { it.id.toString() })
        questionsRef.child("Планета").setValue(planetQuestions.associateBy { it.id.toString() })
    }
    
    /**
     * Удаление категории из базы данных
     */
    suspend fun deleteCategory(categoryName: String) {
        questionsRef.child(categoryName).removeValue()
    }
} 