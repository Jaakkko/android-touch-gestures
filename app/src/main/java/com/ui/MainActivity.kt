package com.ui

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.touchgestures.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SampleView(this))
    }
}
