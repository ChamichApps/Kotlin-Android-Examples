package com.example.tamaskozmer.kotlinrxexample.model.repositories

import com.example.tamaskozmer.kotlinrxexample.model.entities.AnswerList
import com.example.tamaskozmer.kotlinrxexample.model.entities.FavoritedByUser
import com.example.tamaskozmer.kotlinrxexample.model.entities.QuestionList
import com.example.tamaskozmer.kotlinrxexample.model.persistence.daos.AnswerDao
import com.example.tamaskozmer.kotlinrxexample.model.persistence.daos.FavoritedByUserDao
import com.example.tamaskozmer.kotlinrxexample.model.persistence.daos.QuestionDao
import com.example.tamaskozmer.kotlinrxexample.model.services.QuestionService
import com.example.tamaskozmer.kotlinrxexample.model.services.UserService
import com.example.tamaskozmer.kotlinrxexample.util.CalendarWrapper
import com.example.tamaskozmer.kotlinrxexample.util.ConnectionHelper
import com.example.tamaskozmer.kotlinrxexample.util.Constants
import com.example.tamaskozmer.kotlinrxexample.util.PreferencesHelper
import io.reactivex.Single
import io.reactivex.SingleEmitter

/**
 * Created by Tamas_Kozmer on 7/18/2017.
 */
class DetailsRepository(
        private val userService: UserService,
        private val questionService: QuestionService,
        private val questionDao: QuestionDao,
        private val answerDao: AnswerDao,
        private val favoritedByUserDao: FavoritedByUserDao,
        private val connectionHelper: ConnectionHelper,
        private val preferencesHelper: PreferencesHelper,
        private val calendarWrapper: CalendarWrapper) {

    private val REFRESH_LIMIT = 1000 * 60 * 60 * 12 // 12 Hours in milliseconds

    fun getQuestionsByUser(userId: Long): Single<QuestionList> {
        val onlineStrategy = {
            val questions = userService.getQuestionsByUser(userId).execute().body()
                    ?.items
                    ?.take(Constants.NUMBER_OF_ITEMS_IN_SECTION)
            questions?.let {
                questionDao.insertAll(questions)
            }
            QuestionList(questions ?: emptyList())
        }

        val offlineStrategy = {
            val questionsFromDb = questionDao.getQuestionsByUser(userId)
            QuestionList(questionsFromDb)
        }

        return createSingle<QuestionList>("last_update_questions_by_user_$userId", onlineStrategy, offlineStrategy)
    }

    fun getAnswersByUser(userId: Long): Single<AnswerList> {
        val onlineStrategy = {
            val answers = userService.getAnswersByUser(userId).execute().body()?.items
                    ?.filter { it.accepted }
                    ?.take(Constants.NUMBER_OF_ITEMS_IN_SECTION)
            answers?.let {
                answerDao.insertAll(answers)
            }
            AnswerList(answers ?: emptyList())
        }

        val offlineStrategy = {
            val answersFromDb = answerDao.getAnswersByUser(userId)
            AnswerList(answersFromDb)
        }

        return createSingle<AnswerList>("last_update_answers_by_user_$userId", onlineStrategy, offlineStrategy)
    }

    fun getFavoritesByUser(userId: Long): Single<QuestionList> {
        val onlineStrategy = {
            val questions = userService.getFavoritesByUser(userId).execute().body()?.items
                    ?.take(Constants.NUMBER_OF_ITEMS_IN_SECTION)
            questions?.let {
                questionDao.insertAll(questions)
                val favoritedByUser =
                        FavoritedByUser(userId, questions
                                .map { it.questionId })
                favoritedByUserDao.insert(favoritedByUser)
            }
            QuestionList(questions ?: emptyList())
        }

        val offlineStrategy = {
            val questionIds = favoritedByUserDao.getFavoritesForUser(userId)?.questionIds ?: emptyList()
            val questionsFromDb = questionDao.getQuestionsById(questionIds)
                    .take(Constants.NUMBER_OF_ITEMS_IN_SECTION)
            QuestionList(questionsFromDb)
        }

        return createSingle<QuestionList>("last_update_favorites_by_user_$userId", onlineStrategy, offlineStrategy)
    }

    fun getQuestionsById(ids: List<Long>, userId: Long): Single<QuestionList> {
        val onlineStrategy = {
            val questions = questionService.getQuestionsById(ids.joinToString(separator = ";")).execute().body()
            questions?.let {
                questionDao.insertAll(questions.items)
            }
            questions ?: QuestionList(emptyList())
        }

        val offlineStrategy = {
            val questionsFromDb = questionDao.getQuestionsById(ids)
            QuestionList(questionsFromDb)
        }

        return createSingle<QuestionList>("last_update_questions_by_ids_for_user_$userId", onlineStrategy, offlineStrategy)
    }

    private fun <T> createSingle(lastUpdateKey: String, onlineStrategy: () -> T, offlineStrategy: () -> T) : Single<T> {
        return Single.create<T> { emitter: SingleEmitter<T>? ->
            if (shouldUpdate(lastUpdateKey)) {
                try {
                    val onlineResults = onlineStrategy()
                    val currentTime = calendarWrapper.getCurrentTimeInMillis()
                    preferencesHelper.save(lastUpdateKey, currentTime)
                    emitter?.onSuccess(onlineResults)
                } catch (exception: Exception) {
                    emitter?.onError(exception)
                }
            } else {
                val offlineResults = offlineStrategy()
                emitter?.onSuccess(offlineResults)
            }
        }
    }

    private fun shouldUpdate(lastUpdateKey: String) = when {
        !connectionHelper.isOnline() -> false
        else -> {
            val lastUpdate = preferencesHelper.loadLong(lastUpdateKey)
            val currentTime = calendarWrapper.getCurrentTimeInMillis()
            lastUpdate + REFRESH_LIMIT < currentTime
        }
    }
}