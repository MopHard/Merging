package com.example.myfridgeapp

import android.animation.ObjectAnimator
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.example.myfridgeapp.databinding.ActivityFridgeManagerBinding

class FridgeManager : AppCompatActivity() {
    private lateinit var getResult : ActivityResultLauncher<Intent>
    private lateinit var adapter : OuterFridgeAdapter
    lateinit var fridgeListDB: FridgeListDB
    lateinit var binding: ActivityFridgeManagerBinding
    private var isFabOpen = false
    private lateinit var fridgeList : List<FridgeData>
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityFridgeManagerBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initDB()
        initFridge()
        initIntent()
        initFloatingBtn()
        initViewPager()
    }

    private fun initIntent() {
        getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if(it.resultCode == RESULT_OK){
                finish()
            }else if(it.resultCode == RESULT_CANCELED){
                adapter.notifyDataSetChanged()
            }
        }
    }

    fun startActivityResult(intent : Intent){
        getResult.launch(intent)
    }

    private fun initDB() {
        fridgeListDB = FridgeListDB(this)
    }

    private fun initViewPager() {
        adapter = OuterFridgeAdapter(fridgeList)
        val viewPager = binding.viewPager
        viewPager.adapter = adapter
        viewPager.isNestedScrollingEnabled = false

    }

    //fridge db?????? ????????????
    private fun initFridge() {
        fridgeList = fridgeListDB.getFridgeList()
        Log.d("name1", fridgeList.size.toString())
        for(i in fridgeList){
            Log.d("name1", i.name)
        }
    }

    private fun initFloatingBtn(){
        setFABClickEvent()
    }

    private fun setFABClickEvent() {
        // ????????? ?????? ????????? ??????????????? ?????? ??????
        binding.fabMain.setOnClickListener {
            toggleFab()
        }

        // ????????? ?????? ?????? ????????? - ????????? ?????? ?????? ?????? ??????
        binding.fabScanbarcode.setOnClickListener {
            val intent = Intent(this,ProductInfo::class.java)
            getResult.launch(intent)
        }

        binding.fabCart.setOnClickListener {
            val intent = Intent(this,cart_view::class.java)
            startActivity(intent)
        }
        // ????????? ?????? ?????? ????????? - ????????? ??????
        binding.fabAddfridge.setOnClickListener {
            val intent = Intent(this, AddFridge::class.java)
            getResult.launch(intent)
        }


    }

    private fun toggleFab() {
        // ????????? ?????? ?????? ?????? - ???????????? ????????? ?????? ???????????? ???????????????
        if (isFabOpen) {
            ObjectAnimator.ofFloat(binding.fabCart, "translationY", 0f).start()
            ObjectAnimator.ofFloat(binding.fabAddfridge, "translationX", 0f).start()
            ObjectAnimator.ofFloat(binding.fabAddfridge, "translationY", 0f).start()
            ObjectAnimator.ofFloat(binding.fabScanbarcode, "translationX", 0f).start()
            ObjectAnimator.ofFloat(binding.fabMain, View.ROTATION, 45f, 0f).start()
        } else { // ????????? ?????? ?????? ?????? - ???????????? ????????? ?????? ????????? ???????????????
            ObjectAnimator.ofFloat(binding.fabCart, "translationY", -180f).start()
            ObjectAnimator.ofFloat(binding.fabAddfridge, "translationX", -165f).start()
            ObjectAnimator.ofFloat(binding.fabAddfridge, "translationY", -165f).start()
            ObjectAnimator.ofFloat(binding.fabScanbarcode, "translationX", -180f).start()
            ObjectAnimator.ofFloat(binding.fabMain, View.ROTATION, 0f, 45f).start()
        }

        isFabOpen = !isFabOpen

    }
}