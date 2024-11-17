package com.viifo.videocompress.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.Utils
import com.viifo.tgvideocompress.MediaController
import com.viifo.tgvideocompress.OnProgressListener
import com.viifo.tgvideocompress.VideoEditedInfo
import com.viifo.videocompress.R
import com.viifo.videocompress.databinding.ActivityMainBinding
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URISyntaxException


class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var startTime = 0L
    private var inputPath: String? = null

    @SuppressLint("SetTextI18n")
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { it ->
        if(it.resultCode == RESULT_OK) {
            it.data?.data?.takeIf { data -> !TextUtils.isEmpty(data.toString()) }?.let { uri ->
                try {
                    // 视频会被复制到 "/data/data/com.viifo.videocompress/cache/" 下中进行压缩
                    inputPath = copyUri2Cache(uri)?.absolutePath
                    binding.tvPath.text = "uri = ${uri}\n\norigin path = $inputPath"
                    binding.progressHorizontal.progress = 100
                    binding.tvProgress.text = "0%"
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.tvPath.text = inputPath ?: ""
        binding.tvSelect.setOnClickListener { selectVideoFile() }
        binding.tvCompress.setOnClickListener {
            inputPath?.takeIf { it.isNotEmpty() }?.let { path ->
                startTime = System.currentTimeMillis()
                val info = MediaController.makeTgVideoEditedInfo(path)
                MediaController.getInstance().scheduleVideoConvert(info)
            } ?: run {
                Toast.makeText(this@MainActivity, "Please select a video first !", Toast.LENGTH_SHORT).show()
            }
        }

        MediaController.getInstance().setListener(object : OnProgressListener {

            @SuppressLint("DefaultLocale", "SetTextI18n")
            override fun progress(info: VideoEditedInfo?, progress: Float) {
                binding.progressHorizontal.isVisible = true
                binding.tvProgress.isVisible = true
                binding.progressHorizontal.progress = progress.toInt()
                binding.tvProgress.text = String.format("%.2f", progress) + "%"
            }

            @SuppressLint("SetTextI18n")
            override fun success(info: VideoEditedInfo?) {
                binding.progressHorizontal.progress = 100
                binding.tvProgress.text = "100%"
                binding.progressHorizontal.isVisible = false
                binding.tvProgress.isVisible = false
                binding.tvPath.text = "${binding.tvPath.text}\n\nresult path = ${info?.resultPath}"
                val time = System.currentTimeMillis() - startTime
                Toast.makeText(this@MainActivity, "Compression completed, took ${time/1000}s", Toast.LENGTH_SHORT).show()
            }

            override fun error(info: VideoEditedInfo?) {
                Toast.makeText(this@MainActivity, "Compression failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun selectVideoFile() {
        val intent = Intent().apply {
            setAction(Intent.ACTION_OPEN_DOCUMENT)
            addCategory(Intent.CATEGORY_OPENABLE)
            setType("video/*")
        }
        launcher.launch(Intent.createChooser(intent, "选择要导入的视频"))
    }

    private fun copyUri2Cache(uri: Uri): File? {
        Log.d("UriUtils", "copyUri2Cache() called")
        var `is`: InputStream? = null
        try {
            val type = contentResolver.getType(uri)
            val extensionName = MimeTypeMap.getSingleton().getExtensionFromMimeType(type).orEmpty()
            `is` = Utils.getApp().contentResolver.openInputStream(uri)
            val file = File(Utils.getApp().cacheDir, "" + System.currentTimeMillis() + "." + extensionName)
            FileIOUtils.writeFileFromIS(file.absolutePath, `is`);
            return file
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        } finally {
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}