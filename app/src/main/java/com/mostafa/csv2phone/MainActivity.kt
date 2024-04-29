package com.mostafa.csv2phone

import android.app.Activity

// MainActivity.kt
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private val READ_REQUEST_CODE = 42 // Arbitrary request code
    lateinit var btn_upload : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_upload=findViewById(R.id.btnUpload)

        btn_upload.setOnClickListener {
            selectCSVFile()
        }
    }

    fun selectCSVFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        Log.d("FilePicker", "Starting file picker intent")
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // Handle the selected file URI
                handleCSVFile(uri)
            }
        }
    }

    private fun handleCSVFile(fileUri: Uri) {
        val inputStream = contentResolver.openInputStream(fileUri)
        inputStream?.let {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val dataList = mutableListOf<String>()
            var line: String?
            try {
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split(",")
                    if (tokens.size >= 3) {
                        val name = tokens[0]
                        val phoneNumber = tokens[1]
                        val job = tokens[2]
                        val formattedData = "Name: $name\nPhone: $phoneNumber\njob : $job\n"
                        dataList.add(formattedData)
                    }
                }
                displayCSVData(dataList)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
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
                val phoneClickSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val phoneNumber = data.substring(phoneStartIndex, phoneEndIndex)
                        initiatePhoneCall(phoneNumber)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        // Remove underline from clickable text
                        ds.isUnderlineText = false
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



}
