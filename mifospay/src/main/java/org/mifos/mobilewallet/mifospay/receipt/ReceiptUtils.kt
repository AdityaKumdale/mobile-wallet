package org.mifos.mobilewallet.mifospay.receipt

import org.mifos.mobilewallet.mifospay.utils.Constants
import org.mifos.mobilewallet.mifospay.utils.FileUtils
import org.mifos.mobilewallet.mifospay.utils.Toaster
import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import com.mifos.mobile.passcode.BasePassCodeActivity
import com.mifos.mobile.passcode.utils.PassCodeConstants
import com.mifos.mobilewallet.model.domain.Transaction
import okhttp3.ResponseBody
import org.mifos.mobilewallet.mifospay.R
import org.mifos.mobilewallet.mifospay.passcode.ui.PassCodeActivity
import org.mifos.mobilewallet.mifospay.receipt.presenter.ReceiptViewModel
import org.mifos.mobilewallet.mifospay.utils.Toaster.showToast
import java.io.File
import javax.inject.Inject

class ReceiptUtils (private val rContext: Context ) {

    fun writeReceiptToPDF(
        responseBody: ResponseBody?,
        filename: String?
    ) {
        val mifosDirectory = File(Environment.getExternalStorageDirectory(), Constants.MIFOSPAY)
        if (!mifosDirectory.exists()) {
            mifosDirectory.mkdirs()
        }

        val documentFile = filename?.let { File(mifosDirectory.path, it) }
        if (!FileUtils.writeInputStreamDataToFile(responseBody!!.byteStream(), documentFile)) {
            Toast.makeText(rContext, getString(rContext,R.string.downloading_receipt),Toast.LENGTH_SHORT).show()
            showToast(rContext, Constants.ERROR_DOWNLOADING_RECEIPT)
        } else {
            Toaster.show(
                rContext as View,
                Constants.RECEIPT_DOWNLOADED_SUCCESSFULLY,
                Snackbar.LENGTH_LONG,
                Constants.VIEW,
                View.OnClickListener {
                    if (documentFile != null) {
                        openFile(rContext, documentFile)
                    }
                }
            )
        }
    }

    fun initiateDownload(context: Context,transactionId:Transaction,rviewModel: ReceiptViewModel) {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Permission is not granted
            ActivityCompat.requestPermissions(
                context as Activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE
            )
        } else {
            // Permission already granted
            val file = File(
                Environment.getExternalStorageDirectory()
                    .toString() + getString(context,R.string.mifospay),
                getString(context,R.string.receipt)+ transactionId + getString(context,R.string.pdf)
            )
            if (file.exists()) {
                openFile(context, file)
            } else {
                Toast.makeText(context, getString(context,R.string.downloading_receipt),Toast.LENGTH_SHORT).show()
                rviewModel.downloadReceipt(transactionId.transactionId.toString())
            }
        }
    }

    private fun openFile(
        context: Context,
        file: File
    ) {
        val data = FileProvider.getUriForFile(
            context,
            "org.mifos.mobilewallet.mifospay.provider",
            file
        )
        context.grantUriPermission(
            context.packageName,
            data,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(data, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .let { Intent.createChooser(it, context.getString(R.string.view_receipt)) }

        context.startActivity(intent)
    }

    fun copyReceiptLink(uri:String,context:Context) {
        val cm = context.let {
            BasePassCodeActivity.CLIPBOARD_SERVICE
        } as ClipboardManager
        val clipData = ClipData.newPlainText(
            Constants.UNIQUE_RECEIPT_LINK,
            uri.trim { it <= ' ' }
        )
        cm.setPrimaryClip(clipData)
        Toast.makeText(context, getString(context,R.string.Unique_Receipt_Link_copied_to_clipboard),Toast.LENGTH_SHORT).show()
    }

    fun shareReceiptLink(shareMessage: String,context:Context) {

        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, shareMessage)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val chooserIntent = Intent.createChooser(intent, context.getString(R.string.share_receipt))
        context.startActivity(chooserIntent)
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        rviewModel: ReceiptViewModel,
        transactionId:Transaction,
        context:Context
    ) {
        when (requestCode) {
            REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(context, getString(context,R.string.downloading_receipt),Toast.LENGTH_SHORT).show()
                    rviewModel.downloadReceipt(transactionId.transactionId.toString())
                } else {
                    Toast.makeText(context, getString(context,R.string.external_storage_permission_to_download_receipt),Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun openPassCodeActivity(context: Context,deepLinkURI:Uri?) {
        val i = Intent(context as Activity, PassCodeActivity::class.java)
        i.putExtra("uri", deepLinkURI.toString())
        /**
         * this is actually not true but has to be set true so as to make the passcode
         * open a new receipt activity
         */
        i.putExtra(PassCodeConstants.PASSCODE_INITIAL_LOGIN, true)
        context.startActivity(i)
    }


        private val REQUEST_WRITE_EXTERNAL_STORAGE = 48

}