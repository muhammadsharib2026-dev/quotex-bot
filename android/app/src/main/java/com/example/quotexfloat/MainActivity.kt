package com.example.quotexfloat
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
class MainActivity:Activity() {
 private val captureRequest=500
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
 val layout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(35,55,35,30)}
 layout.addView(TextView(this).apply{text="Quotex Float • BTC/USD and BTC/USD OTC\nScreen-based OTC analysis requires permission. Signals are not guaranteed.";textSize=17f})
 layout.addView(TextView(this).apply{text="Direct Gemini AI (no backend). Screenshot is sent to Google only when you tap VERIFY WITH AI. Free-tier eligibility and limits depend on your account."})
 val key=EditText(this).apply{hint="Gemini API key from Google AI Studio";setSingleLine(true);inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;setText(getSharedPreferences("ai",0).getString("gemini_key",""))}
 layout.addView(key)
 layout.addView(Button(this).apply{text="Save API key locally";setOnClickListener{
 val value=key.text.toString().trim()
 getSharedPreferences("ai",0).edit().putString("gemini_key",value).apply()
 Toast.makeText(this@MainActivity,if(value.isBlank())"Direct AI disabled" else "Direct AI configured",Toast.LENGTH_SHORT).show()
 }})
 layout.addView(Button(this).apply{text="Start floating analyzer";setOnClickListener{if(!Settings.canDrawOverlays(this@MainActivity))startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName"))) else requestCapture()}})
 layout.addView(Button(this).apply{text="Stop analyzer";setOnClickListener{stopService(Intent(this@MainActivity,FloatingService::class.java))}})
 setContentView(layout)}
 private fun requestCapture(){val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager;startActivityForResult(mgr.createScreenCaptureIntent(),captureRequest)}
 @Deprecated("Legacy callback")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==captureRequest&&resultCode==RESULT_OK&&data!=null){val i=Intent(this,FloatingService::class.java);i.putExtra("resultCode",resultCode);i.putExtra("projectionData",data);startForegroundService(i)}}
}