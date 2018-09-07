package com.example.tamaskozmer.kotlinrxexample.presentation.viewmodels

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.example.tamaskozmer.kotlinrxexample.domain.interactors.GetUsers
import com.example.tamaskozmer.kotlinrxexample.presentation.view.viewmodels.UserViewModel
import com.example.tamaskozmer.kotlinrxexample.util.SchedulerProvider
import javax.inject.Inject

private const val LOADING_OFFSET = 5

class UserListViewModel @Inject constructor(
    private val getUsers: GetUsers,
    private val schedulerProvider: SchedulerProvider
) : ViewModel() {

    val userList: MutableLiveData<List<UserViewModel>> = MutableLiveData()
    val showLoading: MutableLiveData<Boolean> = MutableLiveData()
    val showError: MutableLiveData<Boolean> = MutableLiveData()

    private var loading = false
        set(value) {
            field = value
            if (value) {
                if (page == 1) {
                    showLoading.value = true
                }
            } else {
                showLoading.value = false
            }
        }

    private var page = 1
    private val users = mutableListOf<UserViewModel>()

    init {
        userList.value = users
        showError.value = false
    }

    fun getUsers(forced: Boolean = false) {
        loading = true
        val pageToRequest = if (forced) 1 else page
        getUsers.execute(pageToRequest, forced)
            .subscribeOn(schedulerProvider.ioScheduler())
            .observeOn(schedulerProvider.uiScheduler())
            .subscribe({ users ->
                showError.value = false
                if (forced) {
                    resetPaging()
                }
                if (page == 1) {
                    this.users.clear()
                }
                this.users.addAll(users)
                userList.value = this.users
                loading = false
                page++
            },
                {
                    showError.value = true
                    loading = false
                })
    }

    private fun resetPaging() {
        page = 1
    }

    fun onScrollChanged(lastVisibleItemPosition: Int, totalItemCount: Int) {
        if (!loading) {
            if (lastVisibleItemPosition >= users.size - LOADING_OFFSET) {
                getUsers()
            }
        }

        if (loading && lastVisibleItemPosition >= totalItemCount) {
            showLoading.value = true
        }
    }

}