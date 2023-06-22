//import android.content.Context
//import android.graphics.Bitmap
//import android.os.CancellationSignal
//import android.os.ParcelFileDescriptor
//import android.print.PrintAttributes
//import android.print.PrintDocumentAdapter
//import android.print.PrintJob
//import android.print.PrintManager
//import android.print.pdf.PrintedPdfDocument
//import android.print.PrintDocumentInfo
//import android.print.PageRange
//import android.print.PrinterId
//import android.print.PrinterInfo
//import android.printservice.PrinterDiscoverySession
//import android.util.Log
//import java.io.FileOutputStream
//import java.io.IOException
//import java.net.InetAddress
//import java.net.UnknownHostException
//import java.util.*
//
//class PrinterHelper {
//
//    companion object {
//        fun printToIPAddress(
//            context: Context,
//            bitmap: Bitmap,
//            jobName: String,
//            ipAddress: String,
//            mediaSize: PrintAttributes.MediaSize
//        ) {
//            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
//
//            // Create a custom printer discovery session to discover printers based on IP address
//            val discoverySession = object : PrinterDiscoverySession() {
//                override fun onStartPrinterDiscovery(priorityList: MutableList<PrinterId>?) {
//                    // Discover printers here
//                    // You can use the ipAddress parameter to discover printers by IP address
//                    // Create PrinterInfo objects and call addPrinters(List<PrinterInfo>)
//                    // with the discovered printers
//                    val printerInfos = mutableListOf<PrinterInfo>()
//                    val printerId = generatePrinterId(ipAddress) // Generate a unique PrinterId
//                    val printerInfo =
//                        PrinterInfo.Builder(printerId, "Printer Name", PrinterInfo.STATUS_IDLE)
//                            .build()
//                    printerInfos.add(printerInfo)
//                    addPrinters(printerInfos)
//                }
//
//                override fun onStopPrinterDiscovery() {
//                    // Stop printer discovery
//                }
//
//                override fun onDestroy() {
//                    // Clean up resources
//                }
//            }
//
//            // Start the printer discovery session
//            discoverySession.onStartPrinterDiscovery(null)
//
//            // Create a custom PrintDocumentAdapter to handle printing
//            val documentAdapter = object : PrintDocumentAdapter() {
//                override fun onLayout(
//                    oldAttributes: PrintAttributes?,
//                    newAttributes: PrintAttributes,
//                    cancellationSignal: CancellationSignal?,
//                    callback: LayoutResultCallback,
//                    extras: Bundle?
//                ) {
//                    if (cancellationSignal?.isCanceled == true) {
//                        callback.onLayoutCancelled()
//                        return
//                    }
//
//                    val document = PrintedPdfDocument(context, newAttributes)
//                    try {
//                        val info = PrintDocumentInfo.Builder(jobName)
//                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
//                            .build()
//
//                        callback.onLayoutFinished(info, true)
//                    } catch (e: Exception) {
//                        callback.onLayoutFailed(e.message)
//                        Log.e("PrinterHelper", "Error creating PDF document", e)
//                    } finally {
//                        document.close()
//                    }
//                }
//
//                override fun onWrite(
//                    pages: Array<out PageRange>?,
//                    destination: ParcelFileDescriptor?,
//                    cancellationSignal: CancellationSignal?,
//                    callback: WriteResultCallback?
//                ) {
//                    var output: FileOutputStream? = null
//                    try {
//                        output = FileOutputStream(destination?.fileDescriptor)
//                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
//                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
//                    } catch (e: IOException) {
//                        callback?.onWriteFailed(e.message)
//                        Log.e("PrinterHelper", "Error writing PDF document", e)
//                    } finally {
//                        output?.close()
//                    }
//                }
//            }
//
//            // Create a print job
//            printManager.print(jobName, documentAdapter, null)
//        }
//
//        private fun generatePrinterId(ipAddress: String): PrinterId {
//            val printerIdString = "printer_${UUID.randomUUID()}"
//            return PrinterId(printerIdString, ipAddress, "Printer Name")
//        }
//    }
//}