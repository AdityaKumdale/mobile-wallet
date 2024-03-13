package org.mifos.mobilewallet.mifospay.receipt.ui

//import org.mifos.mobilewallet.mifospay.theme.MifosTheme
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mifos.mobilewallet.mifospay.base.BaseActivity
import org.mifos.mobilewallet.mifospay.databinding.ActivityReceiptBinding
import org.mifos.mobilewallet.mifospay.designsystem.theme.MifosTheme
import org.mifos.mobilewallet.mifospay.receipt.presenter.ReceiptViewModel

@AndroidEntryPoint
class ReceiptActivity : BaseActivity() {

    private val rviewModel: ReceiptViewModel by viewModels()
    private lateinit var binding:ActivityReceiptBinding

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.receiptCompose.setContent {
            MifosTheme {
                    ReceiptScreen(
                        intent = intent,
                        rviewModel,
                    )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        dismissProgressDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissProgressDialog()
    }
}