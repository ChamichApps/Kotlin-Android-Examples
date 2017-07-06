package com.example.tamaskozmer.kotlinrxexample.view.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import com.example.tamaskozmer.kotlinrxexample.R
import com.example.tamaskozmer.kotlinrxexample.di.modules.DetailActivityModule
import com.example.tamaskozmer.kotlinrxexample.model.entities.DetailsModel
import com.example.tamaskozmer.kotlinrxexample.model.entities.Heading
import com.example.tamaskozmer.kotlinrxexample.model.entities.User
import com.example.tamaskozmer.kotlinrxexample.view.DetailView
import com.example.tamaskozmer.kotlinrxexample.view.adapters.DetailsAdapter
import com.example.tamaskozmer.kotlinrxexample.view.customApplication
import kotlinx.android.synthetic.main.activity_detail.*

/**
 * Created by Tamas_Kozmer on 7/5/2017.
 */
class DetailActivity : AppCompatActivity(), DetailView {

    private val component by lazy { customApplication.component.plus(DetailActivityModule(this)) }
    private val presenter by lazy { component.presenter() }
    private val detailsAdapter by lazy { DetailsAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        component.inject(this)
        presenter.attachView(this)

        initAdapter()
        processIntent()
    }

    private fun initAdapter() {
        detailsRecyclerView.layoutManager = LinearLayoutManager(this)
        detailsRecyclerView.adapter = detailsAdapter
    }

    private fun processIntent() {
        val user = intent.getParcelableExtra<User>("user")
        detailsAdapter.addItem(user)
        detailsAdapter.notifyDataSetChanged()
        presenter.getDetails(user.userId)
    }

    override fun showDetails(detailsModel: DetailsModel) {
        with(detailsAdapter) {
            addItem(Heading("Top questions by user"))
            addItems(detailsModel.questions)
            addItem(Heading("Top answers by user"))
            addItems(detailsModel.answers)
            addItem(Heading("Favorited by user"))
            addItems(detailsModel.favorites)
            notifyDataSetChanged()
        }
    }

    override fun showError() {
        Log.d("details", "error")
    }
}