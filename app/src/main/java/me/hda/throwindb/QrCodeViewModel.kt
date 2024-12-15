package me.hda.throwindb

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.barcode.common.Barcode
import me.hda.throwindb.extra.QRCodeListDatasource
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.regex.Pattern

class QrCodeViewModel(barcode: Barcode) {
    var boundingRect: Rect = barcode.boundingBox!!
    var qrContent: String = ""
    var qrContentEndValue: String = ""
    var qrContentEndTimestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
    var qrCodeTouchCallback = { _: View, _: MotionEvent -> false } //no-op
    private val liveData: MutableLiveData<List<QRCodeData>> = MutableLiveData(mutableListOf())

    init {
        when (barcode.valueType) {/*            Barcode.TYPE_URL -> {
                            qrContent = barcode.url!!.url!!
                            qrCodeTouchCallback = { v: View, e: MotionEvent ->
                                if (e.action == MotionEvent.ACTION_DOWN && boundingRect.contains(e.getX().toInt(), e.getY().toInt())) {
                                    val openBrowserIntent = Intent(Intent.ACTION_VIEW)
                                    openBrowserIntent.data = Uri.parse(qrContent)
                                    v.context.startActivity(openBrowserIntent)
                                }
                                true // return true from the callback to signify the event was handled
                            }
                        }
            */
            Barcode.TYPE_TEXT -> {
                qrContent = barcode.displayValue.toString()
                val jObject = JSONObject(qrContent)
                if (jObject.has("id")) {
                    if (Pattern.matches(
                            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}",
                            jObject.getString("id")
                        )
                    ) {
                        qrContentEndValue = jObject.toString() ?: QRCodeListDatasource.ERROR

                    }

                }


            }// Add other QR Code types here to handle other types of data,
            // like Wifi credentials.
            else -> {
                qrContent = "Unsupported data type: ${barcode.rawValue.toString()}"
            }
        }
    }
}