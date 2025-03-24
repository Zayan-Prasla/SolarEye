package com.zayan.solareye

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        FirebaseFirestore.setLoggingEnabled(true)
        auth = FirebaseAuth.getInstance()

        // Navigate to Settings Activity
        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Handle Camera Control Button click
        findViewById<ImageButton>(R.id.cameraControlButton).setOnClickListener {
            showCameraControlDialog()
        }

        // Handle Light Control Button click
        findViewById<ImageButton>(R.id.lightControlButton).setOnClickListener {
            showLightControlDialog()
        }

        // Handle SOS Button click
        findViewById<ImageButton>(R.id.sosButton).setOnClickListener {
            showSOSConfirmationDialog()
        }

        // Make the entire Event History Section clickable
        val eventHistorySection = findViewById<View>(R.id.eventHistorySection)
        eventHistorySection.setOnClickListener {
            val intent = Intent(this, EventHistoryActivity::class.java)
            startActivity(intent)
        }

        // Handle back button to move the app to the background instead of closing it
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        // Initialize and update the charging stats graph
        initChargingStatsGraph()
    }

    private fun initChargingStatsGraph() {
        val barChart = findViewById<BarChart>(R.id.batteryLevelChart)

        // Dummy battery level data
        val batteryLevels = listOf(90f, 80f, 75f, 60f, 55f, 70f, 85f, 95f, 100f)
        val times = listOf("09", "12", "15", "18", "21", "00", "03", "06", "09")

        // Creating bar entries from dummy data
        val barEntries = batteryLevels.mapIndexed { index, level ->
            BarEntry(index.toFloat(), level)
        }

        val barDataSet = BarDataSet(barEntries, getString(R.string.charging_stats))
        barDataSet.color = Color.parseColor("#00CC00")
        barDataSet.valueTextColor = Color.BLACK
        barDataSet.valueTextSize = 10f

        val barData = BarData(barDataSet)
        barChart.data = barData

        // Styling the chart
        barChart.description.isEnabled = false
        barChart.setDrawValueAboveBar(true)
        barChart.axisRight.isEnabled = false
        barChart.setPinchZoom(false)
        barChart.setDrawGridBackground(false)
        barChart.setScaleEnabled(false)

        // Styling X-Axis
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(times)

        // Styling Y-Axis
        val yAxis = barChart.axisLeft
        yAxis.axisMaximum = 100f
        yAxis.axisMinimum = 0f
        yAxis.granularity = 20f
        yAxis.setDrawGridLines(true)

        barChart.invalidate() // Refresh chart with data
    }

    private fun showCameraControlDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.camera_control_dialog, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        val dialog = builder.create()

        // Handle On/Off button clicks
        val onButton = dialogView.findViewById<Button>(R.id.cameraOnButton)
        val offButton = dialogView.findViewById<Button>(R.id.cameraOffButton)

        onButton.setOnClickListener {
            updateCameraState(true)
            dialog.dismiss()
        }

        offButton.setOnClickListener {
            updateCameraState(false)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showLightControlDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.light_control_dialog, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        val dialog = builder.create()

        // Handle On/Off button clicks
        val onButton = dialogView.findViewById<Button>(R.id.lightOnButton)
        val offButton = dialogView.findViewById<Button>(R.id.lightOffButton)

        onButton.setOnClickListener {
            updateLEDState(1)
            dialog.dismiss()
        }

        offButton.setOnClickListener {
            updateLEDState(0)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateLEDState(state: Int) {
        val ledState = hashMapOf("state" to state)
        db.collection("led_control").document("led_state")
            .set(ledState)
            .addOnSuccessListener {
                Toast.makeText(this, "LED state updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating LED state: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateCameraState(state: Boolean) {
        val cameraState = hashMapOf("state" to state)
        db.collection("camera_control").document("camera_state")
            .set(cameraState)
            .addOnSuccessListener {
                Toast.makeText(this, "Camera state updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating camera state: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSOSConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Emergency Call")
        builder.setMessage("Are you sure you want to call 911?")
        builder.setPositiveButton("Yes") { _, _ ->
            makeEmergencyCall()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun makeEmergencyCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
        } else {
            initiateCall()
        }
    }

    private fun initiateCall() {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:911")
        try {
            startActivity(callIntent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission denied to make calls", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initiateCall()
        } else {
            Toast.makeText(this, "Permission required to make emergency calls", Toast.LENGTH_SHORT).show()
        }
    }
}