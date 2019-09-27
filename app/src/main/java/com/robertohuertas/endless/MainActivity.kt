package com.robertohuertas.endless

import android.Manifest
import android.Manifest.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityCompat.*
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.provider.Settings
import android.support.annotation.NonNull

import com.robertohuertas.endless.SmsConfig
import com.robertohuertas.endless.SMSReceiver
import com.robertohuertas.endless.SmsTool

class MainActivity : AppCompatActivity() {
    private lateinit var smsSenderTextView: TextView
    private lateinit var smsMessageTextView: TextView
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == SMSReceiver.INTENT_ACTION_SMS) {
                val receivedSender = intent.getStringExtra(SMSReceiver.KEY_SMS_SENDER)
                val receivedMessage = intent.getStringExtra(SMSReceiver.KEY_SMS_MESSAGE)
                smsSenderTextView.text = getString(
                    R.string.text_sms_sender_number,
                    receivedSender ?: "NO NUMBER"
                )
                smsMessageTextView.text = getString(
                    R.string.text_sms_message,
                    receivedMessage ?: "NO MESSAGE"
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SmsConfig.INSTANCE.initializeSmsConfig(
            "BEGIN-MESSAGE",
            "END-MESSAGE",
            "0900123456", "0900654321", "0900900900"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            SmsTool.requestSMSPermission(this)
        }
        initViews();
        title = "Fries APP"
        actionOnService(Actions.START)
        findViewById<Button>(R.id.btnStartService).let {
            it.setOnClickListener {
                log("START THE FOREGROUND SERVICE ON DEMAND")
                actionOnService(Actions.START)
                registerReceiver()
                onResume()
            }
        }

        findViewById<Button>(R.id.btnStopService).let {
            it.setOnClickListener {
                log("STOP THE FOREGROUND SERVICE ON DEMAND")
                actionOnService(Actions.STOP)
            }
        }
    }

    private fun initViews() {
        smsSenderTextView = findViewById(R.id.sms_sender_text_view)
        smsMessageTextView = findViewById(R.id.sms_message_text_view)

        smsSenderTextView.setText(getString(R.string.text_sms_sender_number, ""))
        smsMessageTextView.setText(getString(R.string.text_sms_message, ""))
    }

    private fun registerReceiver() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        val intentFilter = IntentFilter()
        intentFilter.addAction(SMSReceiver.INTENT_ACTION_SMS)
        localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun unRegisterReceiver() {
        localBroadcastManager.unregisterReceiver(broadcastReceiver)
    }

    override fun onResume() {
        registerReceiver()
        log("Starting Reveiver")
        super.onResume()
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, EndlessService::class.java).also {
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                log("Starting the service in >=26 Mode")
                startForegroundService(it)
                return
            }
            log("Starting the service in < 26 Mode")
            startService(it)
            onResume()

        }
    }
}
