package com.example.tamaskozmer.kotlinrxexample.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.Toast
import com.example.tamaskozmer.kotlinrxexample.R
import com.example.tamaskozmer.kotlinrxexample.di.modules.MainActivityModule
import com.example.tamaskozmer.kotlinrxexample.model.entities.User
import com.example.tamaskozmer.kotlinrxexample.presentation.UserListPresenter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), MainView {

    private val presenter: UserListPresenter by lazy { component.presenter() }
    private val component by lazy { customApplication.component.plus(MainActivityModule(this)) }
    private var loading = false
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        component.inject(this)

        initViews()
        initAdapter()

        presenter.attachView(this)

        showLoading()
        presenter.getUsers()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    // region View interface methods
    private fun initViews() {
        swipeRefreshLayout.setOnRefreshListener {
            initAdapter()
            loading = true
            presenter.resetPaging()
            presenter.getUsers()
        }
    }

    override fun showLoading() {
        loading = true
        swipeRefreshLayout.isRefreshing = true
    }

    override fun hideLoading() {
        loading = false
        swipeRefreshLayout.isRefreshing = false
    }

    override fun addUsersToList(users: List<User>) {
        val adapter = recyclerView.adapter as UserListAdapter
        adapter.addUsers(users)
    }

    override fun showError() {
        Toast.makeText(this, "Couldn't load data", Toast.LENGTH_SHORT).show()
    }
    // endregion

    private fun initAdapter() {
        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        val userList = mutableListOf<User>()
        recyclerView.adapter = UserListAdapter(userList) {
            Toast.makeText(this, it.displayName, Toast.LENGTH_SHORT).show()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var pastVisibleItems = 0
            var visibleItemCount = 0
            var totalItemCount = 0
            val offset = 5

            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {

                visibleItemCount = layoutManager.childCount;
                totalItemCount = layoutManager.itemCount;
                pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                if (!loading) {
                    if (visibleItemCount + pastVisibleItems >= totalItemCount - offset) {
                        Log.d("asd", "Paging reached ${visibleItemCount + pastVisibleItems}")
                        presenter.getUsers()
                        loading = true
                    }
                }

                if (loading && visibleItemCount + pastVisibleItems >= totalItemCount) {
                    showLoading()
                }
            }
        })
    }
}
