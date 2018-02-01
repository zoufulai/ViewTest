package com.szshy.viewtest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.szshy.viewtest.widget.LeftDrawLayout
import java.util.*

class MainActivity : AppCompatActivity(),View.OnClickListener {
    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.test->ld!!.closeDrawer()
            R.id.id_content_tv->ld!!.openDrawer()
        }
    }

    private var ld:LeftDrawLayout?=null;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        ld = findViewById(R.id.left_draw)
        findViewById<LinearLayout>(R.id.test).setOnClickListener(this)
        findViewById<TextView>(R.id.id_content_tv).setOnClickListener(this)


        val time:Timer?=Timer()
        time!!.schedule( object :TimerTask(){
            override fun run() {
                    ld!!.openDrawer()
                    time.cancel()
            }

        },1000)

    }

}
