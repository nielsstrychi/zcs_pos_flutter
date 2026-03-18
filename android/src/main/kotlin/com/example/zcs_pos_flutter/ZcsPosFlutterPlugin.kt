// android/src/main/kotlin/com/example/zcs_pos_flutter/ZcsPosFlutterPlugin.kt

package com.example.zcs_pos_flutter

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfRenderer
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
import com.zcs.sdk.HQrsanner
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
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import android.view.ViewGroup


class ZcsPosFlutterPlugin : FlutterPlugin, MethodCallHandler {
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
    private lateinit var mScanner: HQrsanner

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
            "printBigText" -> printBigText(call, result)
            "openCashDrawer" -> openCashDrawer(result)
            "printHtmlContent" -> printHtmlContent(call, result)
            "printPdfFromPathOrAsset" -> printPdfFromPathOrAsset(call, result)
            "printPdfFromUrl" -> printPdfFromUrl(call, result)
            "printBarcode" -> printBarcode(call, result)
            "scanBarcode" -> scanBarcode(result)
            else -> result.notImplemented()
        }
    }

    private fun scanBarcode(result: Result) {
        if (!isSdkInitialized) {
            result.error("SDK_NOT_INITIALIZED", "SDK not initialized", null)
            return
        }

        Thread {
            try {
                // Initialize scanner connection
                var connectResult = mScanner.QRscanConnect()
                if (connectResult != SdkResult.SDK_OK) {
                    mainHandler.post {
                        result.error("SCANNER_INIT_FAILED", "Failed to connect to scanner", null)
                    }
                    return@Thread
                }

                // Turn on power and LED for scanner
                mScanner.QRScanerPowerCtrl(1.toByte())

                try {
                    // Timeout array (e.g. 15 seconds)
                    val len = IntArray(1)
                    val recvData = ByteArray(1024)

                    // The timeout sets internal wait but also sets scanning length if QRstartDecdingAndReciveData is called with appropriate timeout param
                    // Based on standard usage, param 1 is timeout in seconds/ms based on SDK spec
                    // Let's assume the method is: QRstartDecdingAndReciveData(timeout_in_sec, recvData, len)
                    val decodeResult = mScanner.QRstartDecdingAndReciveData(15, recvData, len)

                    if (decodeResult == SdkResult.SDK_OK && len[0] > 0) {
                        val decodedString = String(recvData, 0, len[0])
                        mainHandler.post {
                            result.success(decodedString)
                        }
                    } else {
                        mainHandler.post {
                            result.success(null)
                        }
                    }
                } finally {
                    // Turn off scanner power and disconnect
                    mScanner.QRScanerPowerCtrl(0.toByte())
                    mScanner.QRscanDisconect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Barcode scan error: ${e.message}")
                mainHandler.post {
                    result.error("SCAN_ERROR", e.message, null)
                }
            }
        }.start()
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
            mScanner = mDriverManager.hQrsannerDriver
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
            val merchantName = call.argument<String>("merchantName") ?: "Admin"
            val billerName = call.argument<String>("billerName") ?: "Admin"
            val items = call.argument<List<Map<String, Any>>>("items") ?: emptyList()
            val netAmount = call.argument<String>("netAmount") ?: "0.00"
            val cashPaid = call.argument<String>("cashPaid") ?: "0.00"

            val printStatus = mPrinter.printerStatus
            if (printStatus == SdkResult.SDK_PRN_STATUS_PAPEROUT) {
                Log.w(TAG, "Printer out of paper")
                result.success(false)
                return
            }

            // Format settings for monospace font (assuming 41 characters width)
            val receiptWidth = 41
            val itemNameWidth = 25  // Width for item name column
            val qtyWidth = 4       // Width for QTY column
            val spWidth = 5        // Width for SP column
            val amtWidth = 5       // Width for Amount column

            val bodyFormat = PrnStrFormat().apply {
                textSize = 25
                ali = Layout.Alignment.ALIGN_NORMAL
                style = PrnTextStyle.NORMAL
                font = PrnTextFont.MONOSPACE // Use monospace for alignment
            }

            val boldFormat = PrnStrFormat().apply {
                textSize = 25
                ali = Layout.Alignment.ALIGN_NORMAL
                style = PrnTextStyle.BOLD
                font = PrnTextFont.MONOSPACE
            }

            // Print header
            val separator = "-".repeat(receiptWidth)
            mPrinter.setPrintAppendString(separator, bodyFormat)
            mPrinter.setPrintAppendString(
                formatReceiptLine(
                    "${getCurrentDate()}",
                    "${getCurrentTime()}",
                    receiptWidth
                ), bodyFormat
            )
            mPrinter.setPrintAppendString(
                "Biller Name:$billerName".padEnd(receiptWidth),
                bodyFormat
            )
            mPrinter.setPrintAppendString(separator, bodyFormat)

            // Print column headers
            val headerLine = formatItemLine(
                "Item Name",
                "QTY",
                "SP",
                "Amt",
                itemNameWidth,
                qtyWidth,
                spWidth,
                amtWidth
            )
            mPrinter.setPrintAppendString(headerLine, boldFormat)
            mPrinter.setPrintAppendString(separator, bodyFormat)

            // Print items with text wrapping
            var totalItems = 0
            var totalQty = 0

            items.forEach { item ->
                val itemName = item["name"]?.toString() ?: "Unknown Item"
                val qty = item["qty"]?.toString() ?: "1"
                val price = item["price"]?.toString() ?: "0.00"
                val amount = item["amount"]?.toString() ?: "0.00"

                totalItems++
                totalQty += qty.toIntOrNull() ?: 1

                // Handle long item names with wrapping
                val wrappedLines = wrapItemName(
                    itemName,
                    qty,
                    price,
                    amount,
                    itemNameWidth,
                    qtyWidth,
                    spWidth,
                    amtWidth
                )
                wrappedLines.forEach { line ->
                    mPrinter.setPrintAppendString(line, bodyFormat)
                }
            }

            mPrinter.setPrintAppendString(separator, bodyFormat)

            // Print summary
            mPrinter.setPrintAppendString("Item/QTY:$totalItems/$totalQty", bodyFormat)
            mPrinter.setPrintAppendString(separator, bodyFormat)

            // Print totals (right aligned)
            mPrinter.setPrintAppendString(
                formatReceiptLine("Net Amount:", netAmount, receiptWidth),
                bodyFormat
            )
            mPrinter.setPrintAppendString(separator, bodyFormat)
            mPrinter.setPrintAppendString(
                formatReceiptLine("Cash Paid:", cashPaid, receiptWidth),
                bodyFormat
            )

            val printResult = mPrinter.setPrintStart()
            val success = printResult == SdkResult.SDK_OK

            if (success) {
                mPrinter.openPrnCutter(1)
                mBeeper.beep(2000, 300)
            }

            result.success(success)
        } catch (e: Exception) {
            Log.e(TAG, "Print error: ${e.message}")
            result.success(false)
        }
    }

    private fun printBigText(call: MethodCall, result: Result) {
        if (!isSdkInitialized) {
            result.success(false)
            return
        }

        val boldFormat = PrnStrFormat().apply {
            textSize = 80
            ali = Layout.Alignment.ALIGN_CENTER
            style = PrnTextStyle.BOLD
            font = PrnTextFont.MONOSPACE
        }
        val text = call.argument<String>("text") ?: "0.00"
        mPrinter.setPrintAppendString(text, boldFormat)

        val printResult = mPrinter.setPrintStart()
        val success = printResult == SdkResult.SDK_OK

        if (success) {
            mPrinter.openPrnCutter(1)
            mBeeper.beep(2000, 300)
        }

        result.success(success)
    }

    private fun printBarcode(call: MethodCall, result: Result) {

    }




    // Helper function to wrap long item names
    private fun wrapItemName(
        itemName: String, qty: String, price: String, amount: String,
        itemNameWidth: Int, qtyWidth: Int, spWidth: Int, amtWidth: Int
    ): List<String> {
        val lines = mutableListOf<String>()

        if (itemName.length <= itemNameWidth) {
            // Item name fits in one line
            lines.add(
                formatItemLine(
                    itemName,
                    qty,
                    price,
                    amount,
                    itemNameWidth,
                    qtyWidth,
                    spWidth,
                    amtWidth
                )
            )
        } else {
            // Item name needs wrapping
            val wrappedNames = wrapText(itemName, itemNameWidth)

            // First line with qty, price, amount
            lines.add(
                formatItemLine(
                    wrappedNames[0],
                    qty,
                    price,
                    amount,
                    itemNameWidth,
                    qtyWidth,
                    spWidth,
                    amtWidth
                )
            )

            // Additional lines with only item name (other columns empty but centered)
            for (i in 1 until wrappedNames.size) {
                lines.add(
                    formatItemLine(
                        wrappedNames[i],
                        "",
                        "",
                        "",
                        itemNameWidth,
                        qtyWidth,
                        spWidth,
                        amtWidth
                    )
                )
            }
        }

        return lines
    }

    // Helper function to wrap text to specified width
    private fun wrapText(text: String, maxWidth: Int): List<String> {
        val lines = mutableListOf<String>()
        var remainingText = text

        while (remainingText.length > maxWidth) {
            // Find the best break point (prefer space)
            var breakPoint = maxWidth
            for (i in maxWidth downTo maxWidth - 10) {
                if (i < remainingText.length && remainingText[i] == ' ') {
                    breakPoint = i
                    break
                }
            }

            lines.add(remainingText.substring(0, breakPoint).trim())
            remainingText = remainingText.substring(breakPoint).trim()
        }

        if (remainingText.isNotEmpty()) {
            lines.add(remainingText)
        }

        return lines
    }

    // Helper function to format item line with proper column alignment
    private fun formatItemLine(
        itemName: String, qty: String, price: String, amount: String,
        itemNameWidth: Int, qtyWidth: Int, spWidth: Int, amtWidth: Int
    ): String {
        val paddedItemName = itemName.take(itemNameWidth).padEnd(itemNameWidth)
        val paddedQty = qty.take(qtyWidth).padStart(qtyWidth)
        val paddedPrice = price.take(spWidth).padStart(spWidth)
        val paddedAmount = amount.take(amtWidth).padStart(amtWidth)

        return "$paddedItemName $paddedQty $paddedPrice $paddedAmount"
    }

    // Helper function to format receipt line with left and right alignment
    private fun formatReceiptLine(leftText: String, rightText: String, totalWidth: Int): String {
        val availableWidth = totalWidth - rightText.length
        val paddedLeftText = leftText.take(availableWidth).padEnd(availableWidth)
        return "$paddedLeftText$rightText"
    }

    // Helper function to get current date
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    // Helper function to get current time
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    // Helper function to get current date and time (for backward compatibility)
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        return sdf.format(Date())
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
        // Implementation commented out as in original code
        result.success(null)
    }

    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // FIXED HTML PRINTING METHOD
    private fun printHtmlContent(call: MethodCall, result: Result) {
        if (!isSdkInitialized) {
            Log.e(TAG, "SDK not initialized for HTML printing")
            result.success("error: SDK not initialized")
            return
        }

        val htmlContent = call.argument<String>("htmlContent") ?: "<h1>No Content</h1>"
        Log.d(TAG, "Starting HTML print with content length: ${htmlContent.length}")
        showToast("Preparing HTML for printing...")

        try {
            // Run on main thread since WebView requires it
            mainHandler.post {
                try {
                    val webView = WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            useWideViewPort = false
                            loadWithOverviewMode = false
                        }
                        setBackgroundColor(Color.WHITE)

                        // Set fixed width for thermal printer (80mm ≈ 576px)
                        layoutParams =
                            ViewGroup.LayoutParams(576, ViewGroup.LayoutParams.WRAP_CONTENT)
                    }
                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            view?.postDelayed({
                                try {
                                    Log.d(TAG, "WebView page finished loading")

                                    // Force measure and layout
                                    val widthSpec = View.MeasureSpec.makeMeasureSpec(
                                        576,
                                        View.MeasureSpec.EXACTLY
                                    )
                                    val heightSpec = View.MeasureSpec.makeMeasureSpec(
                                        0,
                                        View.MeasureSpec.UNSPECIFIED
                                    )
                                    view.measure(widthSpec, heightSpec)

                                    val measuredHeight = view.measuredHeight.coerceAtLeast(400)
                                    view.layout(0, 0, 576, measuredHeight)

                                    Log.d(TAG, "WebView dimensions: 576 x $measuredHeight")

                                    // Create bitmap and capture content
                                    val bitmap = Bitmap.createBitmap(
                                        576,
                                        measuredHeight,
                                        Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = Canvas(bitmap)
                                    canvas.drawColor(Color.WHITE)
                                    view.draw(canvas)

                                    Log.d(TAG, "Bitmap created, sending to printer")

                                    // Print the bitmap
                                    val printResult = printBitmapOnPos(bitmap)
                                    Log.d(TAG, "Print result: $printResult")
                                    result.success(printResult)

                                } catch (e: Exception) {
                                    Log.e(TAG, "Error in WebView capture: ${e.message}")
                                    result.success("error: Failed to capture WebView - ${e.message}")
                                }
                            }, 1000) // Increased delay to ensure content is fully loaded
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            Log.e(TAG, "WebView error: $description")
                            result.success("error: WebView error - $description")
                        }
                    }

                    // Load the HTML content
                    val styledHtml = """
            <html>
              <head>
                <style>
                  body { 
                    margin: 0; 
                    padding: 10px; 
                    font-family: Arial, sans-serif; 
                    font-size: 14px;
                    width: 556px; /* 576px - 20px padding */
                    background: white;
                  }
                  * { box-sizing: border-box; }
                </style>
              </head>
              <body>$htmlContent</body>
            </html>
          """.trimIndent()

                    webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)

                } catch (e: Exception) {
                    Log.e(TAG, "Error creating WebView: ${e.message}")
                    result.success("error: Failed to create WebView - ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTML print error: ${e.message}")
            result.success("error: ${e.message}")
        }
    }

    // FIXED PDF FROM PATH/ASSET METHOD
    private fun printPdfFromPathOrAsset(call: MethodCall, result: Result) {
        if (!isSdkInitialized) {
            Log.e(TAG, "SDK not initialized for PDF printing")
            result.success("error: SDK not initialized")
            return
        }

        val pdfPath = call.argument<String>("pdfPath") ?: ""
        val isAsset = call.argument<Boolean>("isAsset") ?: false

        if (pdfPath.isEmpty()) {
            Log.e(TAG, "No PDF path provided")
            result.success("error: No PDF path provided")
            return
        }

        // Run PDF processing in background thread
        Thread {
            try {
                Log.d(TAG, "Processing PDF from path: $pdfPath, isAsset: $isAsset")
                showToast("Processing PDF...")

                val file: File = if (isAsset) {
                    Log.d(TAG, "Copying asset: $pdfPath")
                    val fileName = pdfPath.substringAfterLast("/").ifEmpty { "temp.pdf" }
                    val tempFile = File(context.cacheDir, fileName)

                    context.assets.open(pdfPath).use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "Asset copied to: ${tempFile.absolutePath}")
                    tempFile
                } else {
                    File(pdfPath)
                }

                if (!file.exists()) {
                    Log.e(TAG, "PDF file not found: ${file.absolutePath}")
                    showToast("❌ PDF file not found")
                    result.success("error: PDF file not found at ${file.absolutePath}")
                    return@Thread
                }

                Log.d(TAG, "Opening PDF file: ${file.absolutePath}")
                val fileDescriptor =
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(fileDescriptor)

                Log.d(TAG, "PDF opened successfully. Pages: ${pdfRenderer.pageCount}")
                showToast("PDF has ${pdfRenderer.pageCount} pages")

                // Check printer status before starting
                val printerStatus = mPrinter.printerStatus
                if (printerStatus == SdkResult.SDK_PRN_STATUS_PAPEROUT) {
                    Log.e(TAG, "Printer out of paper")
                    showToast("❌ Printer out of paper")
                    result.success("error: printer out of paper")
                    return@Thread
                }

                // Process each page
                for (pageIndex in 0 until pdfRenderer.pageCount) {
                    val page = pdfRenderer.openPage(pageIndex)
                    Log.d(TAG, "Processing page ${pageIndex + 1}: ${page.width}x${page.height}")

                    // Calculate appropriate size for thermal printer
                    val maxWidth = 576 // 80mm thermal printer width
                    val scale = maxWidth.toFloat() / page.width.toFloat()
                    val scaledWidth = (page.width * scale).toInt()
                    val scaledHeight = (page.height * scale).toInt()

                    Log.d(TAG, "Scaled dimensions: ${scaledWidth}x${scaledHeight}")

                    val bitmap =
                        Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                    Log.d(TAG, "Page ${pageIndex + 1} rendered, adding to print queue")
                    mPrinter.setPrintAppendBitmap(bitmap, Layout.Alignment.ALIGN_CENTER)

                    page.close()
                    showToast("Processed page ${pageIndex + 1}/${pdfRenderer.pageCount}")
                }

                pdfRenderer.close()
                fileDescriptor.close()

                Log.d(TAG, "All pages processed, starting print job")
                showToast("Sending to printer...")

                val printResult = mPrinter.setPrintStart()
                Log.d(TAG, "Print result: $printResult")

                if (printResult == SdkResult.SDK_OK) {
                    mPrinter.openPrnCutter(1)
                    mBeeper.beep(2000, 300)
                    showToast("✅ PDF printed successfully")
                    result.success("success: PDF printed successfully")
                } else {
                    Log.e(TAG, "Print failed with result: $printResult")
                    showToast("❌ Print failed")
                    result.success("error: Print failed with code $printResult")
                }

                // Clean up temp file if it was an asset
                if (isAsset && file.exists()) {
                    file.delete()
                }

            } catch (e: Exception) {
                Log.e(TAG, "PDF print error: ${e.message}", e)
                showToast("❌ PDF print error: ${e.message}")
                result.success("error: ${e.message}")
            }
        }.start()
    }

    // FIXED PDF FROM URL METHOD
    private fun printPdfFromUrl(call: MethodCall, result: Result) {
        if (!isSdkInitialized) {
            Log.e(TAG, "SDK not initialized for PDF URL printing")
            result.success("error: SDK not initialized")
            return
        }

        val pdfUrl = call.argument<String>("pdfUrl") ?: ""
        if (pdfUrl.isEmpty()) {
            Log.e(TAG, "No PDF URL provided")
            result.success("error: No PDF URL provided")
            return
        }

        // Run download and print in background thread
        Thread {
            try {
                Log.d(TAG, "Starting PDF download from: $pdfUrl")
                showToast("Downloading PDF...")

                // Download PDF from URL
                val url = URL(pdfUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    requestMethod = "GET"
                    doInput = true
                    setRequestProperty("User-Agent", "ZCS-POS-Printer/1.0")
                }

                connection.connect()
                Log.d(TAG, "HTTP Response: ${connection.responseCode}")

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Download failed with HTTP ${connection.responseCode}")
                    showToast("❌ Download failed: HTTP ${connection.responseCode}")
                    result.success("error: HTTP ${connection.responseCode}")
                    return@Thread
                }

                val contentLength = connection.contentLength
                Log.d(TAG, "Content length: $contentLength bytes")

                val tempFile =
                    File(context.cacheDir, "downloaded_pdf_${System.currentTimeMillis()}.pdf")

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                connection.disconnect()

                if (!tempFile.exists() || tempFile.length() == 0L) {
                    Log.e(TAG, "Downloaded file is invalid")
                    showToast("❌ Downloaded file is invalid")
                    result.success("error: Downloaded file is invalid")
                    return@Thread
                }

                Log.d(TAG, "PDF downloaded successfully: ${tempFile.length()} bytes")
                showToast("PDF downloaded, processing...")

                // Open and process PDF
                val fileDescriptor =
                    ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(fileDescriptor)

                Log.d(TAG, "PDF opened, pages: ${pdfRenderer.pageCount}")
                showToast("Processing ${pdfRenderer.pageCount} pages...")

                // Check printer status
                val printerStatus = mPrinter.printerStatus
                if (printerStatus == SdkResult.SDK_PRN_STATUS_PAPEROUT) {
                    Log.e(TAG, "Printer out of paper")
                    showToast("❌ Printer out of paper")
                    result.success("error: printer out of paper")
                    return@Thread
                }

                // Process each page
                for (i in 0 until pdfRenderer.pageCount) {
                    val page = pdfRenderer.openPage(i)

                    // Scale for thermal printer
                    val maxWidth = 576
                    val scale = maxWidth.toFloat() / page.width.toFloat()
                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                    mPrinter.setPrintAppendBitmap(bitmap, Layout.Alignment.ALIGN_CENTER)
                    page.close()

                    showToast("Processed page ${i + 1}/${pdfRenderer.pageCount}")
                }

                pdfRenderer.close()
                fileDescriptor.close()

                // Start printing
                Log.d(TAG, "Starting print job")
                showToast("Printing...")

                val printResult = mPrinter.setPrintStart()

                if (printResult == SdkResult.SDK_OK) {
                    mPrinter.openPrnCutter(1)
                    mBeeper.beep(2000, 300)
                    showToast("✅ PDF printed successfully")
                    result.success("success: PDF printed successfully")
                } else {
                    Log.e(TAG, "Print failed: $printResult")
                    showToast("❌ Print failed")
                    result.success("error: Print failed with code $printResult")
                }

                // Clean up temp file
                tempFile.delete()

            } catch (e: Exception) {
                Log.e(TAG, "PDF URL print error: ${e.message}", e)
                showToast("❌ Error: ${e.message}")
                result.success("error: ${e.message}")
            }
        }.start()
    }

    private fun printBitmapOnPos(bitmap: Bitmap): String {
        return try {
            val printStatus = mPrinter.printerStatus
            if (printStatus == SdkResult.SDK_PRN_STATUS_PAPEROUT) {
                Log.w(TAG, "Printer out of paper")
                return "error: printer out of paper"
            }

            Log.d(TAG, "Adding bitmap to print queue")
            mPrinter.setPrintAppendBitmap(bitmap, Layout.Alignment.ALIGN_CENTER)

            Log.d(TAG, "Starting print job")
            val printResult = mPrinter.setPrintStart()

            when (printResult) {
                SdkResult.SDK_OK -> {
                    mPrinter.openPrnCutter(1)
                    mBeeper.beep(2000, 300)
                    Log.d(TAG, "Print successful")
                    "success: printed successfully"
                }

                SdkResult.SDK_PRN_STATUS_PAPEROUT -> {
                    Log.w(TAG, "Paper out during printing")
                    "error: paper out while printing"
                }

                else -> {
                    Log.e(TAG, "Print failed with code: $printResult")
                    "error: printer error code $printResult"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in printBitmapOnPos: ${e.message}", e)
            "error: ${e.message}"
        }
    }

    private fun extractCardData(cardInfoEntity: CardInfoEntity): Map<String, Any> {
        val cardData = mutableMapOf<String, Any>()

        cardData["resultCode"] = cardInfoEntity.resultcode
        cardData["cardType"] = cardInfoEntity.cardExistslot?.name ?: "Unknown"

        cardInfoEntity.cardNo?.let { cardData["cardNumber"] = it }
        cardInfoEntity.rfCardType?.let { cardData["rfCardType"] = getRfCardTypeName(it) }
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


}