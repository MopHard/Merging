package com.example.myfridgeapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myfridgeapp.databinding.CartViewBinding
import com.example.refrigerator_manage.CartData
import java.util.*

class cart_view : AppCompatActivity() {
    lateinit var binding: CartViewBinding
    val data: ArrayList<CartData> = ArrayList()
    lateinit var cartItemAdapter: MyCartItemAdapter
    lateinit var myCartDBHelper: MyCartDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CartViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init() {
        myCartDBHelper = MyCartDBHelper(this)
        //myCartDBHelper.insertProduct(CartData(0,"당근",20))
        myCartDBHelper.savetorecycler()
        initRecyclerView()
    }

    private fun initRecyclerView() {
        binding.recyclerview.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL, false
        )
        cartItemAdapter = MyCartItemAdapter(data)
        cartItemAdapter.increaseLinstener = object : MyCartItemAdapter.OnItemClickListener {
            override fun OnItemClick(data: CartData, pos: Int) {
                var now_amt = data.menuCnt
                var now_pid = data.pid
                var now_name = data.nameMenu

                now_amt++
                data.menuCnt++
                var new_menu = CartData(now_pid, now_name, now_amt)
                cartItemAdapter.notifyItemChanged(pos)
            }
        }

        cartItemAdapter.decreaseListener = object : MyCartItemAdapter.OnItemClickListener {
            override fun OnItemClick(data: CartData, pos: Int) {
                var now_amt = data.menuCnt
                var now_pid = data.pid
                var now_name = data.nameMenu

                now_amt--
                var new_menu = CartData(now_pid, now_name, now_amt)

                if (now_amt == 0) {
                    cartItemAdapter.removeItem(pos);
                } else {
                    data.menuCnt--
                }
                cartItemAdapter.notifyItemChanged(pos)

            }

        }
        cartItemAdapter.hrefListener = object : MyCartItemAdapter.OnItemClickListener {
            override fun OnItemClick(data: CartData, pos: Int) {
                //구매 링크 이동
                var now_item_name = data.nameMenu
                var uri_smaple = "https://msearch.shopping.naver.com/search/all?query="
                var full_uri = uri_smaple + now_item_name
                var intent = Intent(Intent.ACTION_VIEW, Uri.parse(full_uri))
                startActivity(intent)
                cartItemAdapter.notifyItemChanged(pos)
            }
        }
        cartItemAdapter.deleteListener = object : MyCartItemAdapter.OnItemClickListener {
            override fun OnItemClick(data: CartData, pos: Int) {
                var now_pid = data.pid

                val result = myCartDBHelper.deleteProduct(now_pid.toString())
                cartItemAdapter.removeItem(pos);
                cartItemAdapter.notifyItemChanged(pos)

            }

        }
        binding.recyclerview.adapter = cartItemAdapter
    }
}