package com.example.toolkit_v1

import android.annotation.SuppressLint
import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.Math.abs
import java.net.*
import java.util.*

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val exifButton = findViewById<Button>(R.id.exifButton)
        exifButton.setOnClickListener {
            exifFileLauncher.launch("image/*")
        }

        val domainToIpButton = findViewById<Button>(R.id.domainToIp)
        domainToIpButton.setOnClickListener {
            domainToIp()
        }

        val internetCheckButton = findViewById<Button>(R.id.internetCheckButton)
        internetCheckButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                val result = getWebPage("https://api.ipify.org/")

                if (result != null) {
                    withContext(Dispatchers.Main) {
                        outputDialog("結果", "インターネットに接続されています。\n IPアドレス： $result")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        outputDialog("結果", "インターネットに接続されていません")
                    }
                }

            }
        }

        val whoisButton = findViewById<Button>(R.id.whoisButton)
        whoisButton.setOnClickListener {
            getWhois()
        }

        val portScanButton = findViewById<Button>(R.id.portScanButton)
        portScanButton.setOnClickListener {
            portScan()
        }

        val tcpConnectButton = findViewById<Button>(R.id.tcpConnectButton)
        tcpConnectButton.setOnClickListener {
            showConnectionDialog()
        }
    }

    private fun showConnectionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Connect to Server")

        val view = layoutInflater.inflate(R.layout.dialog_layout, null)
        val hostEditText = view.findViewById<EditText>(R.id.domain_edit_text)
        val portEditText = view.findViewById<EditText>(R.id.port_edit_text)

        builder.setView(view)
        builder.setPositiveButton("Connect") { dialog, which ->
            val host = hostEditText.text.toString()
            val port = portEditText.text.toString().toInt()
            showSendDialog(host, port)
        }
        builder.setNegativeButton("Cancel", null)

        builder.show()
    }

    @SuppressLint("MissingInflatedId")
    private fun showSendDialog(host: String, port: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Send Data")
        builder.setMessage("※改行が必要なリクエスト(HTTPなど）は改行を入れてください。")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Send") { dialog, which ->
            CoroutineScope(Dispatchers.IO).launch {
                val data = input.text.toString()
                val response = sendTcpData(host, port, data)
                withContext(Dispatchers.Main) {
                    outputDialog("Response",response)
                }
            }
        }
        builder.setNegativeButton("Cancel", null)

        builder.show()
    }

    private fun sendTcpData(host: String, port: Int, data: String): String {
        Socket(host, port).use { socket ->
            val outputStream = socket.getOutputStream()
            val inputStream = socket.getInputStream()

            val queryBytes = "$data\r\n\r\n".toByteArray()
            outputStream.write(queryBytes)

            val byteArrayOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (true) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) {
                    break
                }
                byteArrayOutputStream.write(buffer, 0, bytesRead)
            }
            val response = byteArrayOutputStream.toString("UTF-8")
            return response
        }

    }

    private fun portScan(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter domain name and port number")

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_layout, null)
        builder.setView(view)

        builder.setView(view)
        builder.setPositiveButton("Scan") { _, _ ->
            val domainEditText = view.findViewById<EditText>(R.id.domain_edit_text)
            val portEditText = view.findViewById<EditText>(R.id.port_edit_text)

            val domainName = domainEditText.text.toString()
            val port = portEditText.text.toString().toIntOrNull() ?: -1

            if (domainName.isNotBlank() && port in 1..65535) {
                scanTask(domainName, port)
            } else {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { _, _ -> }

        builder.show()
    }

    private fun scanTask(domainName: String, port: Int){
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(domainName, port), 5000)
                socket.close()
                outputDialog("result", "Port $port is open")
            } catch (e: IOException) {
                outputDialog("result", "Port $port is closed")
            }
    }

    private fun getWhois(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("ドメインを入力")
        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, which ->
            //非同期処理
            CoroutineScope(Dispatchers.IO).launch {
                val inputText = input.text.toString()
                val result = getWhoisData(inputText)

                withContext(Dispatchers.Main) {
                    if (result != null) {
                        outputDialog("結果", result)
                    } else {
                        outputDialog("結果", "取得できませんでした。")
                    }
                }
            }
        }
        builder.setNegativeButton("キャンセル") { dialog, which ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun getWhoisData(query: String, server: String = "whois.iana.org", port: Int = 43): String {
        Socket(server, port).use { socket ->
            val outputStream = socket.getOutputStream()
            val inputStream = socket.getInputStream()

            val queryBytes = "$query\r\n".toByteArray()
            outputStream.write(queryBytes)

            val byteArrayOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (true) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) {
                    break
                }
                byteArrayOutputStream.write(buffer, 0, bytesRead)
            }
            val response = byteArrayOutputStream.toString("UTF-8")
            return response
        }
    }


    // Webページにアクセスして結果を取得する関数
    private fun getWebPage(urlString: String): String? {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connect()
            val inputStream = conn.inputStream
            return inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
                conn.disconnect()
        }
        return null
    }

    private fun domainToIp() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("ドメインを入力")
        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, which ->
            val inputText = input.text.toString()
            //非同期処理
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = getIpAddressFromDomain(inputText)
                    withContext(Dispatchers.Main) {
                        if (result != null) {
                            outputDialog("結果", "IPアドレス： $result")
                        } else {
                            outputDialog("結果", "取得できませんでした。")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        outputDialog("結果","エラーが発生しました。")
                    }
                }
            }
        }
        builder.setNegativeButton("キャンセル") { dialog, which ->
            dialog.cancel()
        }
        builder.show()
    }

    fun getIpAddressFromDomain(domain: String): String? {
        try {
            val address = InetAddress.getByName(domain)
            return address.hostAddress
        } catch (e: Exception) {
            return null
        }
    }


    private val exifFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null){
            val context = applicationContext
            val fileName = getFileName(context, uri)
            val result = getExifInformation(context, uri)

            outputDialog("Exif情報", result)
        }
    }

    private fun outputDialog(title: String, message: String){
        val builder = AlertDialog.Builder(this)
        val context = applicationContext
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") {dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }


    private fun getExifInformation(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val exif = ExifInterface(inputStream!!)

        //作成者を取得
        val artist = exif.getAttribute(ExifInterface.TAG_ARTIST) ?: "N/A"

        //画像の作成日時を取得する
        val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: "N/A"

        //カメラのメーカーを取得する
        val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: "N/A"

        //緯度と経度の取得
        val latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)?.let {
            convertToDegree(it)
        } ?: "N/A"
        val longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)?.let {
            convertToDegree(it)
        } ?: "N/A"

        //著作権情報
        val copyright = exif.getAttribute(ExifInterface.TAG_COPYRIGHT)

        //画像のユーザーコメントを取得する
        val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT) ?: "N/A"

        inputStream.close()
        return "作成者: $artist\n画像の作成日時： $dateTime\n緯度、経度： $latitude、$longitude\nカメラのメーカー名： $make\n著作権: $copyright\n画像のユーザーコメント： $userComment"
    }

    //緯度、経度を度数に変換
    private fun convertToDegree(stringDMS: String): String {
        val parts = stringDMS.split(",").map { it.trim() }

        val d = parts[0].toDoubleOrNull() ?: return "不明"
        val m = parts[1].toDoubleOrNull() ?: return "不明"
        val s = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0

        val sign = if (d < 0) -1 else 1
        val degree = sign * (abs(d) + (m / 60) + (s / 3600))

        return "%.6f".format(degree)
    }



    //ファイル名を取得する
    @SuppressLint("Range")
    fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                fileName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
        return fileName
    }
}

