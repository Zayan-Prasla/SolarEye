package com.zayan.solareye

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    class RotateTransformation(private val rotateRotationAngle: Float) : BitmapTransformation() {
        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update("rotate_$rotateRotationAngle".toByteArray(Charsets.UTF_8))
        }

        override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(rotateRotationAngle)
            return Bitmap.createBitmap(toTransform, 0, 0, toTransform.width, toTransform.height, matrix, true)
        }
    }

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var eventHistoryImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        FirebaseFirestore.setLoggingEnabled(true)
        auth = FirebaseAuth.getInstance()

        eventHistoryImage = findViewById(R.id.eventHistoryImage)

        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.cameraControlButton).setOnClickListener {
            showCameraControlDialog()
        }

        findViewById<ImageButton>(R.id.lightControlButton).setOnClickListener {
            showLightControlDialog()
        }

        findViewById<ImageButton>(R.id.sosButton).setOnClickListener {
            showSOSConfirmationDialog()
        }

        findViewById<Button>(R.id.modesOfOperationButton).setOnClickListener {
            showModesOfOperationDialog()
        }

        findViewById<View>(R.id.eventHistorySection).setOnClickListener {
            val intent = Intent(this, EventHistoryActivity::class.java)
            startActivity(intent)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        initChargingStatsGraph()
        fetchRecentEventThumbnail()
    }

    private fun fetchRecentEventThumbnail() {
        db.collection("video_sensor_data")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error fetching recent event: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val document = snapshots.documents[0]
                    val videoUrl = document.getString("video_url")
                    if (videoUrl != null) {
                        Glide.with(this)
                            .load(videoUrl)
                            .transform(RotateTransformation(180f))
                            .into(eventHistoryImage)
                    }
                }
            }
    }

    private fun initChargingStatsGraph() {
        val barChart = findViewById<BarChart>(R.id.batteryLevelChart)
        val chartEntries = ArrayList<BarEntry>()
        val xLabels = ArrayList<String>()

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val now = Date()
        val twentyFourHoursAgo = Calendar.getInstance().apply {
            time = now
            add(Calendar.HOUR_OF_DAY, -24)
        }.time

        db.collection("voltage_sensor_data")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No documents found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val filteredDocs = documents.filter {
                    val ts = it.getString("timestamp") ?: return@filter false
                    try {
                        val docTime = sdf.parse(ts) ?: return@filter false
                        docTime.after(twentyFourHoursAgo)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Failed to parse timestamp: $ts", Toast.LENGTH_SHORT).show()
                        false
                    }
                }

                if (filteredDocs.isEmpty()) {
                    Toast.makeText(this, "No data in last 24 hours", Toast.LENGTH_SHORT).show()
                }

                var index = 0f
                for (doc in filteredDocs.sortedBy { it.getString("timestamp") }) {
                    val ts = doc.getString("timestamp") ?: continue
                    val voltage = doc.getDouble("voltage_state")?.toFloat() ?: continue
                    val timeLabel = ts.substring(11, 16)
                    chartEntries.add(BarEntry(index, voltage))
                    xLabels.add(timeLabel)
                    index++
                }

                if (chartEntries.isEmpty()) {
                    barChart.clear()
                    barChart.setNoDataText("Data exists, but chart has no entries.")
                    return@addOnSuccessListener
                }

                val barDataSet = BarDataSet(chartEntries, "Battery Level (%)").apply {
                    color = Color.parseColor("#00CC00")
                    valueTextColor = Color.BLACK
                    valueTextSize = 10f
                }

                val barData = BarData(barDataSet)
                barChart.data = barData

                barChart.apply {
                    description.isEnabled = false
                    setDrawValueAboveBar(true)
                    axisRight.isEnabled = false
                    setPinchZoom(false)
                    setDrawGridBackground(false)
                    setScaleEnabled(false)
                    animateY(1000)

                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        setDrawGridLines(false)
                        granularity = 1f
                        valueFormatter = IndexAxisValueFormatter(xLabels)
                        textColor = Color.BLACK
                        textSize = 10f
                        labelRotationAngle = 0f
                    }

                    axisLeft.apply {
                        axisMaximum = 100f
                        axisMinimum = 0f
                        granularity = 10f
                        setDrawGridLines(true)
                        textColor = Color.BLACK
                        textSize = 12f
                    }

                    legend.isEnabled = false
                    invalidate()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Firestore error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
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
        db.collection("control").document("led_control")
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
        db.collection("control").document("pir_control")
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
    private fun showModesOfOperationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_modes_of_operation, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        val mode1Button = dialogView.findViewById<Button>(R.id.mode1Button)
        val mode2Button = dialogView.findViewById<Button>(R.id.mode2Button)
        val mode3Button = dialogView.findViewById<Button>(R.id.mode3Button)

        val operationRef = db.collection("control").document("operation_mode")

        mode1Button.setOnClickListener {
            operationRef.set(mapOf("mode" to 1))
                .addOnSuccessListener {
                    Toast.makeText(this, "Switched to Low Power Mode", Toast.LENGTH_SHORT).show()
                }
            dialog.dismiss()
        }

        mode2Button.setOnClickListener {
            operationRef.set(mapOf("mode" to 2))
                .addOnSuccessListener {
                    Toast.makeText(this, "Switched to Battery Saver Mode", Toast.LENGTH_SHORT).show()
                }
            dialog.dismiss()
        }

        mode3Button.setOnClickListener {
            operationRef.set(mapOf("mode" to 3))
                .addOnSuccessListener {
                    Toast.makeText(this, "Switched to Live Streaming Mode", Toast.LENGTH_SHORT).show()
                }
            dialog.dismiss()
            //  Navigate to livestream page
            startActivity(Intent(this, LiveStreamActivity::class.java))
        }

        dialog.show()
    }
}