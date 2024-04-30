package com.mostafa.csv2phone

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.InputStream
import android.Manifest
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.TextView
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private var phoneNumbers = mutableListOf<String>()
    private var currentCallIndex = 0
    private val dataList = mutableListOf<String>()
    private val CALL_PHONE_REQUEST_CODE = 101
    private val calledNumbers = mutableSetOf<String>() // Store the numbers that have been called

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            readCSVFile(it)
        } ?: showToast("File selection failed")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            openFilePicker()
        }

        findViewById<Button>(R.id.btnStartCalls).setOnClickListener {
            checkAndRequestPermission()
        }
    }

    private fun openFilePicker() {
        filePickerLauncher.launch(arrayOf("*/*"))
    }

    private fun readCSVFile(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            phoneNumbers.clear() // Clear existing phone numbers
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            try {
                while (reader.readLine().also { line = it } != null) {
                    // Split the line into tokens using the comma as the delimiter
                    val tokens = line!!.split(",")
                    if (tokens.size >= 3) { // Ensure there are at least 3 tokens (name, phone, job)
                        val name = tokens[0]
                        val phoneNumber = tokens[1]
                        val job = tokens[2]
                        // Format and store the data as needed (e.g., in a list, database)
                        val personInfo = "Name: $name\nPhone: $phoneNumber\nJob: $job\n\n"
                        phoneNumbers.add(phoneNumber) // Add phone number for calling later
                        dataList.add(personInfo) // Add formatted info for display or storage
                    }
                }
                displayCSVData(dataList)
                showToast("CSV file loaded successfully")
            } catch (e: Exception) {
                showToast("Error reading CSV file")
                e.printStackTrace()
            }
        }
    }


    private fun startSequentialCalls() {
        if (phoneNumbers.isEmpty()) {
            showToast("No phone numbers to call")
            return
        }
        if (currentCallIndex < phoneNumbers.size) {
            val phoneNumber = phoneNumbers[currentCallIndex]
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$phoneNumber")
            startActivity(intent)
            updateTextColor(phoneNumber)
            currentCallIndex++
        } else {
            showToast("All calls completed")
            currentCallIndex = 0 // Reset index for future calls
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), CALL_PHONE_REQUEST_CODE)
        } else {
            startSequentialCalls()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PHONE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSequentialCalls()
            } else {
                showToast("Permission denied to make phone calls")
            }
        }
    }

    private fun displayCSVData(dataList: List<String>) {
        val textView = findViewById<TextView>(R.id.txtCSVData)
        val spannableStringBuilder = SpannableStringBuilder()

        dataList.forEach { data ->
            val startIndex = spannableStringBuilder.length
            spannableStringBuilder.append(data)

            // Add phone icon
            val phoneIcon = ContextCompat.getDrawable(this, R.drawable.ic_phone)
            phoneIcon?.let {
                it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
                val imageSpan = ImageSpan(it, ImageSpan.ALIGN_BOTTOM)
                spannableStringBuilder.setSpan(imageSpan, startIndex, startIndex + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }

            // Make phone number clickable
            val phoneStartIndex = data.indexOf("Phone: ") + 7 // Adjust for "Phone: "
            val phoneEndIndex = data.indexOf("\n", phoneStartIndex)
            if (phoneStartIndex >= 0 && phoneEndIndex > phoneStartIndex) {
                val phoneNumber = data.substring(phoneStartIndex, phoneEndIndex)
                val phoneClickSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        initiatePhoneCall(phoneNumber)
                        calledNumbers.add(phoneNumber) // Mark the number as called
                        updateTextColor(phoneNumber) // Update text color after call
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        // Remove underline from clickable text
                        ds.isUnderlineText = false
                        // Set text color based on whether the number has been called
                        ds.color = if (calledNumbers.contains(phoneNumber)) {
                            ContextCompat.getColor(applicationContext, R.color.colorCalled) // Color for called numbers
                        } else {
                            ContextCompat.getColor(applicationContext, android.R.color.white) // Default color
                        }
                    }
                }
                spannableStringBuilder.setSpan(phoneClickSpan, startIndex + phoneStartIndex, startIndex + phoneEndIndex, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }

        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.text = spannableStringBuilder
    }

    private fun initiatePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        startActivity(intent)
    }

    private fun updateTextColor(phoneNumber: String) {
        // Update text color for the specified phoneNumber if needed
        val textView = findViewById<TextView>(R.id.txtCSVData)
        val spannable = textView.text as Spannable
        val phoneStartIndex = spannable.indexOf(phoneNumber)
        if (phoneStartIndex != -1) {
            val phoneEndIndex = phoneStartIndex + phoneNumber.length
            val ds = spannable.getSpans(phoneStartIndex, phoneEndIndex, ClickableSpan::class.java)
            if (ds.isNotEmpty()) {
                val clickedSpan = ds[0]
                spannable.removeSpan(clickedSpan)
                spannable.setSpan(clickedSpan, phoneStartIndex, phoneEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                // Update text color here (similar to the ClickableSpan logic)
                val textColor = ContextCompat.getColor(applicationContext, R.color.colorCalled)
                spannable.setSpan(ForegroundColorSpan(textColor), phoneStartIndex, phoneEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

}

