package com.pratyakshkhurana.quizapp

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.pratyakshkhurana.quizapp.databinding.ActivityQuestions2Binding
import kotlinx.coroutines.launch


class QuestionsActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityQuestions2Binding
    private var mSelectOptionPosition: Int = 0
    private var mCurrentQuestionIndex: Int = 1
    private var mQuestionList: List<Question> = listOf()
    private lateinit var mUsername: String
    private lateinit var category: String
    private var mCorrectAnswers: Int = 0
    private lateinit var quizRepository: FirebaseQuizRepository
    private var isDataLoaded = false

    // Музыкальные эффекты
    private lateinit var right: MediaPlayer
    private lateinit var wrong: MediaPlayer
    
    // Таймер
    private lateinit var countDownTimer: CountDownTimer
    private val initialTimerValue = 15 // Начальное значение таймера в секундах
    private var secondsRemaining = initialTimerValue
    private var timerRunning = false
    
    // Состояние ответа
    private var hasAnswered = false
    
    // Анимации
    private lateinit var fadeInAnimation: Animation
    private lateinit var fadeOutAnimation: Animation
    private lateinit var slideInRightAnimation: Animation
    private lateinit var slideOutLeftAnimation: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestions2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        mUsername = intent.getStringExtra("user").toString()
        category = intent.getStringExtra("category").toString()
        
        // Загружаем анимации
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        slideInRightAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        slideOutLeftAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        
        quizRepository = FirebaseQuizRepository()
        
        // Показываем прогресс загрузки
        binding.progressContainer.visibility = View.VISIBLE
        
        // Загружаем вопросы из Firebase
        loadQuestionsFromFirebase()

        binding.option1.setOnClickListener(this)
        binding.option2.setOnClickListener(this)
        binding.option3.setOnClickListener(this)
        binding.option4.setOnClickListener(this)
        binding.submitButton.setOnClickListener(this)

        //music effects on wrong and right answers
        right = MediaPlayer.create(this, R.raw.right)
        wrong = MediaPlayer.create(this, R.raw.w)
        
        // Инициализация таймера
        setupCountDownTimer()
    }
    
    private fun loadQuestionsFromFirebase() {
        lifecycleScope.launch {
            try {
                // Загружаем вопросы по выбранной категории
                mQuestionList = quizRepository.getQuestionsByCategory(category)
                
                if (mQuestionList.isNotEmpty()) {
                    isDataLoaded = true
                    binding.progressContainer.visibility = View.GONE
                    setQuestion()
                    startTimer() // Запускаем таймер после загрузки вопроса
                } else {
                    Toast.makeText(this@QuestionsActivity, 
                        "Бұл категория бойынша сұрақтар табылмады", 
                        Toast.LENGTH_SHORT).show()
                    binding.progressContainer.visibility = View.GONE
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@QuestionsActivity, 
                    "Сұрақтарды жүктеу кезінде қате: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
                binding.progressContainer.visibility = View.GONE
                finish()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setQuestion() {
        if (!isDataLoaded) return
        
        // Сбрасываем состояние ответа для нового вопроса
        hasAnswered = false
        resetToDefaultOptions()

        val currentQuestion = mQuestionList[mCurrentQuestionIndex - 1]

        if (mCurrentQuestionIndex == mQuestionList.size) {
            binding.submitButton.text = "АЯҚТАУ"
        } else {
            binding.submitButton.text = "ЖІБЕРУ"
        }

        // Анимируем прогресс бар
        setProgressAnimate(binding.progressBar, mCurrentQuestionIndex)
        binding.rating.text = "${mCurrentQuestionIndex}/${mQuestionList.size}"
        
        // Анимация показа нового вопроса
        binding.tvQuestion.startAnimation(fadeInAnimation)
        binding.tvQuestion.text = currentQuestion.question
        
        // Анимация показа вариантов ответа
        val options = arrayOf(binding.option1, binding.option2, binding.option3, binding.option4)
        
        options[0].text = currentQuestion.option1
        options[1].text = currentQuestion.option2
        options[2].text = currentQuestion.option3
        options[3].text = currentQuestion.option4
        
        for (i in options.indices) {
            options[i].startAnimation(fadeInAnimation)
        }
    }

    private fun resetToDefaultOptions() {
        val allOptions = arrayListOf<TextView>()
        allOptions.add(binding.option1)
        allOptions.add(binding.option2)
        allOptions.add(binding.option3)
        allOptions.add(binding.option4)

        for (option in allOptions) {
            option.setTextColor(Color.parseColor("#FFFFFF"))
            option.typeface = Typeface.DEFAULT
            option.background = null
            option.isEnabled = true
        }
    }

    private fun selectedOptionView(tv: TextView, selectedOptionPosition: Int) {
        resetToDefaultOptions()
        mSelectOptionPosition = selectedOptionPosition

        tv.setTextColor(Color.parseColor("#363A43"))
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.background = ContextCompat.getDrawable(
            this,
            R.drawable.selected_option_clicked_bg
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onClick(view: View) {
        // Проверяем состояние для вариантов ответов - они должны работать только когда таймер активен
        // и пользователь еще не ответил
        if (view in arrayOf(binding.option1, binding.option2, binding.option3, binding.option4)) {
            if (!isDataLoaded || !timerRunning || hasAnswered) return
            
            when (view) {
                binding.option1 -> selectedOptionView(binding.option1, 1)
                binding.option2 -> selectedOptionView(binding.option2, 2)
                binding.option3 -> selectedOptionView(binding.option3, 3)
                binding.option4 -> selectedOptionView(binding.option4, 4)
            }
        } 
        // Для кнопки подтверждения/следующего вопроса нужна только проверка загрузки данных
        else if (view == binding.submitButton && isDataLoaded) {
            if (mSelectOptionPosition == 0) {
                // Если не выбран вариант ответа, но уже ответили на вопрос (нажимаем "КЕЛЕСІ")
                if (hasAnswered) {
                    moveToNextQuestion()
                } else {
                    // Если пользователь не выбрал вариант ответа и не ответил ранее
                    Toast.makeText(
                        this, 
                        "Жауап нұсқасын таңдаңыз", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Обрабатываем выбранный ответ
                handleSelectedAnswer()
            }
        }
    }
    
    // Метод для обработки выбранного ответа
    private fun handleSelectedAnswer() {
        pauseTimer() // Останавливаем таймер при ответе
        hasAnswered = true
        
        val quest = mQuestionList[mCurrentQuestionIndex - 1]
        var wrongAns = 0

        // Определяем, верный ли ответ выбран
        if (quest.correctOptionIndex != mSelectOptionPosition) {
            wrong.start()
            wrongAns = mSelectOptionPosition
            showAnswerFeedback(false)
        } else {
            right.start()
            mCorrectAnswers++
            showAnswerFeedback(true)
        }

        // Делаем кнопки неактивными после ответа
        binding.option1.isEnabled = false
        binding.option2.isEnabled = false
        binding.option3.isEnabled = false
        binding.option4.isEnabled = false

        for (i in 1..4) {
            when (i) {
                wrongAns -> {
                    selectedOptionView(i, R.drawable.wrong_option_clicked_bg)
                }
                quest.correctOptionIndex -> {
                    selectedOptionView(i, R.drawable.correct_option_clicked_bg)
                }
                else -> {
                    selectedOptionView(i, R.drawable.question_options_bg)
                }
            }
        }

        if (mCurrentQuestionIndex == mQuestionList.size) {
            binding.submitButton.text = "АЯҚТАУ"
        } else {
            binding.submitButton.text = "КЕЛЕСІ"
        }

        mSelectOptionPosition = 0
    }
    
    // Метод для перехода к следующему вопросу
    private fun moveToNextQuestion() {
        mCurrentQuestionIndex++

        if (mCurrentQuestionIndex <= mQuestionList.size) {
            // Сбрасываем и запускаем таймер для следующего вопроса
            resetTimer()
            
            // Анимация для перехода к следующему вопросу
            binding.tvQuestion.startAnimation(slideOutLeftAnimation)
            binding.option1.startAnimation(slideOutLeftAnimation)
            binding.option2.startAnimation(slideOutLeftAnimation)
            binding.option3.startAnimation(slideOutLeftAnimation)
            binding.option4.startAnimation(slideOutLeftAnimation)
            
            // Устанавливаем новый вопрос после анимации
            slideOutLeftAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    setQuestion()
                    binding.tvQuestion.startAnimation(slideInRightAnimation)
                    binding.option1.startAnimation(slideInRightAnimation)
                    binding.option2.startAnimation(slideInRightAnimation)
                    binding.option3.startAnimation(slideInRightAnimation)
                    binding.option4.startAnimation(slideInRightAnimation)
                }
            })
        } else {
            // Завершение теста
            val intent = Intent(this, ResultActivity::class.java)
            intent.putExtra("correct", mCorrectAnswers)
            intent.putExtra("user", mUsername)
            intent.putExtra("category", category)
            intent.putExtra("total", mQuestionList.size)
            startActivity(intent)
            finish()
        }
    }
    
    private fun showAnswerFeedback(isCorrect: Boolean) {
        binding.answerFeedbackText.text = if (isCorrect) "Дұрыс!" else "Қате!"
        binding.answerFeedbackText.background = ContextCompat.getDrawable(
            this,
            if (isCorrect) R.drawable.correct_option_clicked_bg else R.drawable.wrong_option_clicked_bg
        )
        binding.answerFeedbackText.visibility = View.VISIBLE
        binding.answerFeedbackText.startAnimation(fadeInAnimation)
        
        // Скрываем сообщение через 1 секунду
        binding.answerFeedbackText.postDelayed({
            binding.answerFeedbackText.startAnimation(fadeOutAnimation)
            binding.answerFeedbackText.visibility = View.GONE
        }, 1000)
    }

    private fun selectedOptionView(answer: Int, drawableView: Int) {
        when (answer) {
            1 -> {
                binding.option1.background = ContextCompat.getDrawable(
                    this,
                    drawableView
                )
            }
            2 -> {
                binding.option2.background = ContextCompat.getDrawable(
                    this,
                    drawableView
                )
            }
            3 -> {
                binding.option3.background = ContextCompat.getDrawable(
                    this,
                    drawableView
                )
            }
            4 -> {
                binding.option4.background = ContextCompat.getDrawable(
                    this,
                    drawableView
                )
            }
        }
    }
    
    private fun setProgressAnimate(pb: ProgressBar, progressTo: Int) {
        val animation = ObjectAnimator.ofInt(pb, "progress", pb.progress, progressTo * 100 / mQuestionList.size)
        animation.duration = 500
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }
    
    // Функции для таймера
    private fun setupCountDownTimer() {
        countDownTimer = object : CountDownTimer((initialTimerValue * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = (millisUntilFinished / 1000).toInt()
                binding.timerTextView.text = secondsRemaining.toString()
                
                // Обновляем прогресс бар таймера
                binding.timerProgressBar.progress = secondsRemaining
                
                // Изменение цвета таймера, когда мало времени
                if (secondsRemaining <= 5) {
                    binding.timerTextView.setTextColor(Color.RED)
                } else {
                    binding.timerTextView.setTextColor(Color.WHITE)
                }
            }

            override fun onFinish() {
                timerRunning = false
                binding.timerTextView.text = "0"
                binding.timerProgressBar.progress = 0
                binding.timerTextView.setTextColor(Color.RED)
                
                // Если время истекло и ответ не выбран, автоматически переходим к следующему вопросу
                if (!hasAnswered) {
                    Toast.makeText(this@QuestionsActivity, "Уақыт бітті!", Toast.LENGTH_SHORT).show()
                    
                    // Подсвечиваем правильный ответ
                    val quest = mQuestionList[mCurrentQuestionIndex - 1]
                    selectedOptionView(quest.correctOptionIndex, R.drawable.correct_option_clicked_bg)
                    
                    // Делаем кнопки неактивными
                    binding.option1.isEnabled = false
                    binding.option2.isEnabled = false
                    binding.option3.isEnabled = false
                    binding.option4.isEnabled = false
                    
                    // Помечаем, что ответ был дан (автоматически)
                    hasAnswered = true
                    
                    // Показываем сообщение, что время истекло
                    binding.answerFeedbackText.text = "Уақыт бітті!"
                    binding.answerFeedbackText.background = ContextCompat.getDrawable(
                        this@QuestionsActivity,
                        R.drawable.bg1
                    )
                    binding.answerFeedbackText.visibility = View.VISIBLE
                    binding.answerFeedbackText.startAnimation(fadeInAnimation)
                    
                    // Обновляем текст кнопки для перехода к следующему вопросу
                    if (mCurrentQuestionIndex == mQuestionList.size) {
                        binding.submitButton.text = "АЯҚТАУ"
                    } else {
                        binding.submitButton.text = "КЕЛЕСІ"
                    }
                    
                    // Скрываем сообщение через 1 секунду и автоматически переходим к следующему вопросу
                    binding.answerFeedbackText.postDelayed({
                        binding.answerFeedbackText.startAnimation(fadeOutAnimation)
                        binding.answerFeedbackText.visibility = View.GONE
                        
                        // Автоматически переходим к следующему вопросу через еще 1 секунду
                        binding.answerFeedbackText.postDelayed({
                            // Переходим к следующему вопросу, если автоматический переход
                            if (mCurrentQuestionIndex < mQuestionList.size) {
                                moveToNextQuestion()
                            } else {
                                val intent = Intent(this@QuestionsActivity, ResultActivity::class.java)
                                intent.putExtra("correct", mCorrectAnswers)
                                intent.putExtra("user", mUsername)
                                intent.putExtra("category", category)
                                intent.putExtra("total", mQuestionList.size)
                                startActivity(intent)
                                finish()
                            }
                        }, 1000)
                    }, 2000)
                }
            }
        }
    }

    private fun startTimer() {
        if (!timerRunning) {
            countDownTimer.start()
            timerRunning = true
        }
    }

    private fun pauseTimer() {
        if (timerRunning) {
            countDownTimer.cancel()
            timerRunning = false
        }
    }

    private fun resetTimer() {
        pauseTimer()
        secondsRemaining = initialTimerValue
        binding.timerTextView.text = secondsRemaining.toString()
        binding.timerProgressBar.progress = secondsRemaining
        binding.timerTextView.setTextColor(Color.WHITE)
        startTimer()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        pauseTimer()
        
        // Освобождаем ресурсы медиаплеера
        right.release()
        wrong.release()
    }
}


