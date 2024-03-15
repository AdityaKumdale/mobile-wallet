package org.mifos.mobilewallet.mifospay.receipt.presenter

import androidx.lifecycle.ViewModel
import com.mifos.mobilewallet.model.domain.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mifos.mobilewallet.core.base.UseCase
import org.mifos.mobilewallet.core.base.UseCaseHandler
import org.mifos.mobilewallet.core.domain.usecase.account.DownloadTransactionReceipt
import org.mifos.mobilewallet.core.domain.usecase.account.FetchAccountTransaction
import org.mifos.mobilewallet.core.domain.usecase.account.FetchAccountTransfer
import org.mifos.mobilewallet.datastore.PreferencesHelper
import org.mifos.mobilewallet.mifospay.receipt.ReceiptUtils
import org.mifos.mobilewallet.mifospay.utils.Constants
import javax.inject.Inject

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val mUseCaseHandler: UseCaseHandler,
    private val preferencesHelper: PreferencesHelper,
    private val downloadTransactionReceiptUseCase: DownloadTransactionReceipt,
    private val fetchAccountTransactionUseCase: FetchAccountTransaction,
    private val fetchAccountTransferUseCase: FetchAccountTransfer,
    private  val receiptUtil : ReceiptUtils
):ViewModel(){
    private val _receiptState = MutableStateFlow<ReceiptUiState>(ReceiptUiState.Loading)
    val receiptState: StateFlow<ReceiptUiState> = _receiptState.asStateFlow()

    fun downloadReceipt(transactionId: String?) {
        mUseCaseHandler.execute(downloadTransactionReceiptUseCase,
            DownloadTransactionReceipt.RequestValues(transactionId),
            object : UseCase.UseCaseCallback<DownloadTransactionReceipt.ResponseValue> {
                override fun onSuccess(response: DownloadTransactionReceipt.ResponseValue) {
                    val filename = Constants.RECEIPT + transactionId + Constants.PDF
                    receiptUtil.writeReceiptToPDF(response.responseBody, filename)
                }
                override fun onError(message: String) {
                    _receiptState.value = ReceiptUiState.Error(message)
                }
            })
    }

    fun fetchTransaction(transactionId: String?) {
        val accountId = preferencesHelper.accountId

            if (transactionId != null){
                mUseCaseHandler.execute(fetchAccountTransactionUseCase,
                    FetchAccountTransaction.RequestValues(accountId, transactionId.toLong()),
                    object : UseCase.UseCaseCallback<FetchAccountTransaction.ResponseValue> {
                        override fun onSuccess(response: FetchAccountTransaction.ResponseValue) {
                            _receiptState.value = ReceiptUiState.TransactionReceipt(response.transaction)
                            fetchTransfer(response.transaction.transferId)
                        }

                        override fun onError(message: String) {
                            if (message == Constants.UNAUTHORIZED_ERROR) {
                                _receiptState.value = ReceiptUiState.OpenPassCodeActivity
                            } else {
                                _receiptState.value = ReceiptUiState.Error(message)
                            }
                        }
                    }
                )
            }
    }

    fun fetchTransfer(transferId: Long) {
        mUseCaseHandler.execute(fetchAccountTransferUseCase,
            FetchAccountTransfer.RequestValues(transferId),
            object : UseCase.UseCaseCallback<FetchAccountTransfer.ResponseValue?> {
                override fun onSuccess(response: FetchAccountTransfer.ResponseValue?) {

                }

                override fun onError(message: String) {
                    _receiptState.value = ReceiptUiState.Error(message)
                }
            })
    }
}
sealed interface ReceiptUiState {
    data class TransactionReceipt(
        val transaction: Transaction?
    ) : ReceiptUiState
    data object OpenPassCodeActivity : ReceiptUiState
    data class Error(
        val message: String
    ) : ReceiptUiState
    data object Loading : ReceiptUiState
}

