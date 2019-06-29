package com.anwesh.uiprojects.linkedballlineupdownview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.balllineupdownview.BallLineUpDownView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BallLineUpDownView.create(this)
    }
}
