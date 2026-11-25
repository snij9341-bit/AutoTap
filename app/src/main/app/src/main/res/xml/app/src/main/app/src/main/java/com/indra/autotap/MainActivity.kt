package com.indra.autotap

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("autotap", MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 120, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "AutoTap"
            textSize = 28f
            gravity = Gravity.START
        })
        root.addView(TextView(this).apply {
            text = "1) Neeche button se Accessibility me AutoTap ON karo.\n" +
                "2) Wapas aakar 'Auto-tap chalu' ON karo.\n" +
                "3) Free Fire kholo."
            setPadding(0, 24, 0, 24)
        })

        root.addView(Button(this).apply {
            text = "Accessibility settings kholo"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        fun addSwitch(label: String, key: String, default: Boolean) {
            root.addView(Switch(this).apply {
                text = label
                textSize = 18f
                setPadding(0, 32, 0, 0)
                isChecked = prefs.getBoolean(key, default)
                setOnCheckedChangeListener { _: CompoundButton, on: Boolean ->
                    prefs.edit().putBoolean(key, on).apply()
                }
            })
        }

        addSwitch("Auto-tap chalu", "enabled", false)
        addSwitch("Play Again", "playagain", true)
        addSwitch("Continue", "continue", true)
        addSwitch("Lobby", "lobby", true)

        setContentView(root)
    }
}a
