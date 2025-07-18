// android/src/main/kotlin/com/example/zcs_pos_flutter/ZcsPosFlutterPlugin.kt

package com.example.zcs_pos_flutter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.util.Log
import androidx.annotation.NonNull
import com.zcs.sdk.DriverManager
import com.zcs.sdk.Sys
import com.zcs.sdk.Printer
import com.zcs.sdk.print.PrnStrFormat
import com.zcs.sdk.print.PrnTextFont
import com.zcs.sdk.print.PrnTextStyle
import com.zcs.sdk.card.CardInfoEntity
import com.zcs.sdk.card.CardReaderManager
import com.zcs.sdk.card.CardReaderTypeEnum
//import com.zcs.sdk.card.OnSearchCardListener
import com.zcs.sdk.card.RfCard
import com.zcs.sdk.SdkResult
import com.zcs.sdk.SdkData
import com.zcs.sdk.util.StringUtils
import com.zcs.sdk.Led
import com.zcs.sdk.Beeper
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ZcsPosFlutterPlugin: FlutterPlugin, MethodCallHandler {
  private lateinit var channel: MethodChannel
  private lateinit var context: Context
  private lateinit var mainHandler: Handler

  // ZCS SDK components
  private lateinit var mDriverManager: DriverManager
  private lateinit var mSys: Sys
  private lateinit var mPrinter: Printer
  private lateinit var mCardReadManager: CardReaderManager
  private lateinit var mLed: Led
  private lateinit var mBeeper: Beeper

  private var isSdkInitialized = false
  private val READ_TIMEOUT = 60 * 1000 // 60 seconds
  private val TAG = "ZcsPosFlutter"

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "zcs_pos_flutter")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    mainHandler = Handler(Looper.getMainLooper())

    // Initialize ZCS SDK components
    initializeSDKComponents()
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "initializeSdk" -> initializeSdk(result)
      "printReceipt" -> printReceipt(call, result)
      "showAmountOnDisplay" -> showAmountOnDisplay(call, result)
      "readNfcCard" -> readNfcCard(result)
      "openCashDrawer" -> openCashDrawer(result)
      else -> result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private fun initializeSDKComponents() {
    try {
      mDriverManager = DriverManager.getInstance()
      mSys = mDriverManager.baseSysDevice
      mPrinter = mDriverManager.printer
      mCardReadManager = mDriverManager.cardReadManager
      mLed = mDriverManager.ledDriver
      mBeeper = mDriverManager.beeper
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize SDK components: ${e.message}")
    }
  }

  private fun initializeSdk(result: Result) {
    try {
      var status = mSys.sdkInit()
      if (status != SdkResult.SDK_OK) {
        mSys.sysPowerOn()
        Thread.sleep(1000)
        status = mSys.sdkInit()
      }

      isSdkInitialized = (status == SdkResult.SDK_OK)

      if (isSdkInitialized) {
        Log.d(TAG, "SDK initialized successfully")
        // Initialize beeper for feedback
        mBeeper.beep(2000, 200)
      } else {
        Log.e(TAG, "SDK initialization failed with status: $status")
      }

      result.success(isSdkInitialized)
    } catch (e: Exception) {
      Log.e(TAG, "SDK initialization error: ${e.message}")
      result.success(false)
    }
  }

  private fun printReceipt(call: MethodCall, result: Result) {
    if (!isSdkInitialized) {
      result.success(false)
      return
    }

    try {
      val merchantName = call.argument<String>("merchantName") ?: "Unknown Merchant"
      val amount = call.argument<String>("amount") ?: "₹0.00"
      val items = call.argument<List<String>>("items") ?: emptyList()

      val printStatus = mPrinter.printerStatus
      if (printStatus == SdkResult.SDK_PRN_STATUS_PAPEROUT) {
        Log.w(TAG, "Printer out of paper")
        result.success(false)
        return
      }

      // Print header
      val headerFormat = PrnStrFormat().apply {
        textSize = 30
        ali = Layout.Alignment.ALIGN_CENTER
        style = PrnTextStyle.BOLD
        font = PrnTextFont.DEFAULT
      }
      mPrinter.setPrintAppendString("=== RECEIPT ===", headerFormat)
      mPrinter.setPrintAppendString(" ", headerFormat)

      // Print merchant info
      val bodyFormat = PrnStrFormat().apply {
        textSize = 25
        ali = Layout.Alignment.ALIGN_NORMAL
        style = PrnTextStyle.NORMAL
        font = PrnTextFont.DEFAULT
      }
      mPrinter.setPrintAppendString("MERCHANT: $merchantName", bodyFormat)
      mPrinter.setPrintAppendString("DATE: ${getCurrentDateTime()}", bodyFormat)
      mPrinter.setPrintAppendString("-----------------------------", bodyFormat)

      // Print items
      items.forEach { item ->
        mPrinter.setPrintAppendString(item, bodyFormat)
      }

      mPrinter.setPrintAppendString("-----------------------------", bodyFormat)

      // Print total amount
      val totalFormat = PrnStrFormat().apply {
        textSize = 28
        ali = Layout.Alignment.ALIGN_CENTER
        style = PrnTextStyle.BOLD
        font = PrnTextFont.DEFAULT
      }
      mPrinter.setPrintAppendString("TOTAL: $amount", totalFormat)
      mPrinter.setPrintAppendString(" ", bodyFormat)
      mPrinter.setPrintAppendString("Thank you for your business!", bodyFormat)
      mPrinter.setPrintAppendString(" ", bodyFormat)
      mPrinter.setPrintAppendString(" ", bodyFormat)

      val printResult = mPrinter.setPrintStart()
      val success = printResult == SdkResult.SDK_OK

      if (success) {
        mBeeper.beep(2000, 300) // Success beep
      }

      result.success(success)
    } catch (e: Exception) {
      Log.e(TAG, "Print error: ${e.message}")
      result.success(false)
    }
  }

  private fun showAmountOnDisplay(call: MethodCall, result: Result) {
    if (!isSdkInitialized) {
      result.success(false)
      return
    }

    try {
      val amount = call.argument<String>("amount") ?: "₹0.00"

      // Create bitmap for customer display (128x64 px)
      val bitmap = createAmountDisplayBitmap(amount)

      // Show on LED customer display
      mSys.showBitmapOnLcd(bitmap, true)

      result.success(true)
    } catch (e: Exception) {
      Log.e(TAG, "Display error: ${e.message}")
      result.success(false)
    }
  }

  private fun createAmountDisplayBitmap(amount: String): Bitmap {
    val bitmap = Bitmap.createBitmap(128, 64, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
      color = Color.BLACK
      textSize = 20f
      typeface = Typeface.DEFAULT_BOLD
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
    }

    canvas.drawColor(Color.WHITE)
    canvas.drawText("AMOUNT", 64f, 25f, paint)
    canvas.drawText(amount, 64f, 50f, paint)

    return bitmap
  }

  private fun readNfcCard(result: Result) {
   /* if (!isSdkInitialized) {
      result.success(null)
      return
    }

    try {
      mCardReadManager.cancelSearchCard()

      val future = CompletableFuture<Map<String, Any>?>()

      mCardReadManager.searchCard(CardReaderTypeEnum.RF_CARD, READ_TIMEOUT,
        object : OnSearchCardListener {
          override fun onCardInfo(cardInfoEntity: CardInfoEntity) {
            try {
              val cardData = extractCardData(cardInfoEntity)
              future.complete(cardData)
            } catch (e: Exception) {
              Log.e(TAG, "Card data extraction error: ${e.message}")
              future.complete(null)
            }
          }

          override fun onError(errorCode: Int) {
            Log.e(TAG, "Card read error: $errorCode")
            future.complete(null)
          }

          override fun onNoCard(cardReaderType: CardReaderTypeEnum, timeout: Boolean) {
            Log.w(TAG, "No card detected")
            future.complete(null)
          }
        })

      // Wait for result with timeout
      val cardData = future.get(READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)

      if (cardData != null) {
        mBeeper.beep(2500, 200) // Success beep
        mLed.setLed(com.zcs.sdk.led.LedLightModeEnum.GREEN, true)
        Handler(Looper.getMainLooper()).postDelayed({
          mLed.setLed(com.zcs.sdk.led.LedLightModeEnum.GREEN, false)
        }, 1000)
      } else {
        mBeeper.beep(1000, 500) // Error beep
        mLed.setLed(com.zcs.sdk.led.LedLightModeEnum.RED, true)
        Handler(Looper.getMainLooper()).postDelayed({
          mLed.setLed(com.zcs.sdk.led.LedLightModeEnum.RED, false)
        }, 1000)
      }

      result.success(cardData)
    } catch (e: Exception) {
      Log.e(TAG, "NFC read error: ${e.message}")
      result.success(null)
    }*/
  }

  private fun extractCardData(cardInfoEntity: CardInfoEntity): Map<String, Any> {
    val cardData = mutableMapOf<String, Any>()

    cardData["resultCode"] = cardInfoEntity.resultcode
    cardData["cardType"] = cardInfoEntity.cardExistslot?.name ?: "Unknown"

    cardInfoEntity.cardNo?.let { cardData["cardNumber"] = it }
    cardInfoEntity.rfCardType?.let { cardData["rfCardType"] = getRfCardTypeName(it) }
//    cardInfoEntity.rfuid?.let { cardData["uid"] = StringUtils.convertBytesToHex(it) }
    cardInfoEntity.atr?.let { cardData["atr"] = it }
    cardInfoEntity.tk1?.let { cardData["track1"] = it }
    cardInfoEntity.tk2?.let { cardData["track2"] = it }
    cardInfoEntity.tk3?.let { cardData["track3"] = it }
    cardInfoEntity.expiredDate?.let { cardData["expiryDate"] = it }
    cardInfoEntity.serviceCode?.let { cardData["serviceCode"] = it }

    return cardData
  }

  private fun getRfCardTypeName(rfCardType: Byte): String {
    return when (rfCardType) {
      SdkData.RF_TYPE_A -> "RF_TYPE_A"
      SdkData.RF_TYPE_B -> "RF_TYPE_B"
      SdkData.RF_TYPE_MEMORY_A -> "RF_TYPE_MEMORY_A"
      SdkData.RF_TYPE_FELICA -> "RF_TYPE_FELICA"
      SdkData.RF_TYPE_MEMORY_B -> "RF_TYPE_MEMORY_B"
      else -> "Unknown"
    }
  }

  private fun openCashDrawer(result: Result) {
    if (!isSdkInitialized) {
      result.success(false)
      return
    }

    try {
      val drawerResult = mPrinter.openBox()
      val success = drawerResult == SdkResult.SDK_OK

      if (success) {
        mBeeper.beep(2000, 300) // Success beep
      }

      result.success(success)
    } catch (e: Exception) {
      Log.e(TAG, "Cash drawer error: ${e.message}")
      result.success(false)
    }
  }

  private fun getCurrentDateTime(): String {
    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
    return dateFormat.format(java.util.Date())
  }
}