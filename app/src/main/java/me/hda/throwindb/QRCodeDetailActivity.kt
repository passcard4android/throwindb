package me.hda.throwindb

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import me.hda.throwindb.extra.QRCodeListDatasource
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset


class QRCodeDetailActivity : AppCompatActivity() {
    companion object {
        const val QRCODE_DATA = "qrCodeData"
        const val QRCODE_TIMESTAMP = "qrCodeTimestamp"
    }

    private lateinit var qrCodeDetailText: TextView
    private lateinit var qrCodeDetailImage: ImageView
    private lateinit var qrCodeDetailFab: View
    private lateinit var qrCodeDetailButton: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode_detail)

        qrCodeDetailText = findViewById(R.id.qrCodeDetailText)
        qrCodeDetailImage = findViewById(R.id.qrCodeDetailImage)
        qrCodeDetailFab = findViewById(R.id.qrCodeDetailFab)
        qrCodeDetailButton = findViewById(R.id.video_play_button)

        //setBrightness()


        val qrCodeData = intent.getStringExtra(QRCODE_DATA) ?: "ERROR"
        val qrCodeTimestamp = intent.getLongExtra(QRCODE_TIMESTAMP, Instant.now().epochSecond)
        val qrCode = QRCodeData(
            qrCodeData,
            OffsetDateTime.ofInstant(Instant.ofEpochSecond(qrCodeTimestamp), ZoneOffset.UTC)
        )

        qrCodeDetailFab.setOnClickListener {
            QRCodeListDatasource.remove(qrCodeTimestamp)
            this.finish()
        }

        val jObject = JSONObject(qrCode.data)

        qrCodeDetailText.text = jObject.getString("text")

        if (jObject.has("photoUrl")) {
            qrCodeDetailImage.setImageURI(Uri.parse(jObject.getString("photoUrl")))
        } else {
            qrCodeDetailImage.setImageBitmap(qrCode.bitmap.scale(250, 250))
        }

        if (jObject.has("videoUrl")) {
            qrCodeDetailButton.setVisibility(View.VISIBLE)
            qrCodeDetailButton.setOnClickListener {
                val videoIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(jObject.getString("videoUrl")))
                startActivity(videoIntent)
            }
        }
    }

    fun startShareActivity(item: MenuItem) {
        startShare(this)
    }

    fun showQR(item: MenuItem) {
        val qrCodeData = intent.getStringExtra(QRCODE_DATA) ?: "ERROR"
        val qrCodeTimestamp = intent.getLongExtra(QRCODE_TIMESTAMP, Instant.now().epochSecond)
        val qrCode = QRCodeData(
            qrCodeData,
            OffsetDateTime.ofInstant(Instant.ofEpochSecond(qrCodeTimestamp), ZoneOffset.UTC)
        )
        qrCodeDetailImage.setImageBitmap(qrCode.bitmap.scale(250, 250))
    }

    private fun setBrightness() {
        try {
            val layout = window.attributes
            layout.screenBrightness = 1.0f
            window.attributes = layout
        } catch (_: Exception) {
            // NOOP
        }
    }

    private fun startShare(context: Context) {
        val qrCodeData = intent.getStringExtra(QRCODE_DATA) ?: "ERROR"
        val qrCodeTimestamp = intent.getLongExtra(QRCODE_TIMESTAMP, Instant.now().epochSecond)
        val qrCode = QRCodeData(
            qrCodeData,
            OffsetDateTime.ofInstant(Instant.ofEpochSecond(qrCodeTimestamp), ZoneOffset.UTC)
        )
        try {
            val cachePath = File(context.getCacheDir(), "images")
            cachePath.mkdirs()
            val stream = FileOutputStream(File(cachePath, "qr.png"))
            qrCode.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val imagePath = File(context.cacheDir, "images")
        val newFile = File(imagePath, "qr.png")
        val contentUri =
            FileProvider.getUriForFile(context, "me.hda.throwindb.fileprovider", newFile)
        if (contentUri != null) {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.setDataAndType(contentUri, contentResolver.getType(contentUri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            startActivity(Intent.createChooser(shareIntent, "Choose an app"))
        }
    }
}