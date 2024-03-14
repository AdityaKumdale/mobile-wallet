package org.mifos.mobilewallet.mifospay.receipt.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mifos.mobilewallet.model.domain.Transaction
import com.mifos.mobilewallet.model.domain.TransactionType
import org.mifos.mobilewallet.mifospay.R
import org.mifos.mobilewallet.mifospay.designsystem.component.MifosOverlayLoadingWheel
import org.mifos.mobilewallet.mifospay.receipt.ReceiptUtils
import org.mifos.mobilewallet.mifospay.receipt.presenter.ReceiptUiState
import org.mifos.mobilewallet.mifospay.receipt.presenter.ReceiptViewModel



@Composable
fun ReceiptScreen(
    intent: Intent,
    viewModel: ReceiptViewModel = hiltViewModel()
){
    val context = LocalContext.current
    val rUiState by viewModel.receiptState.collectAsState()
    var transactionId: String? = null
    val deepLinkURI: Uri?
    val data = intent.data
    deepLinkURI = data
    var uri: String? = null
    val receiptUtils =  ReceiptUtils(context)




    if (data != null) {
            val scheme = data.scheme // "https"
            val host = data.host // "receipt.mifospay.com"
            val params: List<String>
            try {
                params = data.pathSegments
                transactionId = params[0] // "transactionId"
                uri = data.toString()
            } catch (e: IndexOutOfBoundsException) {
                Toast.makeText(context, stringResource(R.string.invalid_link),Toast.LENGTH_SHORT).show()
            }
            Toast.makeText(context, stringResource(R.string.please_wait),Toast.LENGTH_SHORT).show()
            viewModel.fetchTransaction(transactionId)
        }

    when (val state = rUiState){
        ReceiptUiState.Loading -> {
            MifosOverlayLoadingWheel(contentDesc = stringResource(R.string.loading))
        }
        ReceiptUiState.OpenPassCodeActivity -> {
            receiptUtils.openPassCodeActivity(context,deepLinkURI)
        }
        is ReceiptUiState.Error -> {
            val message = state.message
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        is ReceiptUiState.TransactionReceipt -> {
            val transaction = state.transaction
            if (transaction != null) {
                if (uri != null) {
                    Receipt(rUiState = rUiState,transaction = transaction,context,uri,receiptUtils,viewModel)
                }
            }
        }
        else -> {}
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Receipt(
    rUiState: ReceiptUiState,
    transaction: Transaction,
    context:Context,
    uri:String,
    receiptUtils:ReceiptUtils,
    viewModel:ReceiptViewModel
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.receipt))},
                navigationIcon = {
                    IconButton(onClick = { /* Handle back navigation */ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = { receiptUtils.initiateDownload(context, transaction ,viewModel) },
                contentColor = Color.Black,
                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download),
                        contentDescription = stringResource(R.string.download_receipt)
                    )
                }
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier) {
                Image(
                    painter = painterResource(id = R.drawable.mifospay_round_logo),
                    contentDescription = stringResource(R.string.pan_id),
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.CenterHorizontally)
                )
                showHeadtexts(transaction)
                Spacer(modifier = Modifier.size(height = 15.dp, width = 14.dp))
                showInfoTexts(transaction,context,uri,receiptUtils)
            }
        }
    }
}

@Composable
fun showHeadtexts(transaction: Transaction){

    Column (Modifier.fillMaxWidth()){
        val centerWithPaddingModifier = Modifier
            .padding(horizontal = 8.dp)
            .align(Alignment.CenterHorizontally)

        Text(
            text = transaction.amount.toString(),
            fontSize = 34.sp,
            modifier = centerWithPaddingModifier.padding(top = 10.dp)
        )

        Text(
            text = when (transaction.transactionType) {
                TransactionType.DEBIT -> stringResource(R.string.paid_to)
                TransactionType.CREDIT -> stringResource(R.string.credited_by)
                TransactionType.OTHER -> stringResource(R.string.other)
            },
            color = when (transaction.transactionType) {
                TransactionType.DEBIT -> Color.Red
                TransactionType.CREDIT -> Color.Cyan
                TransactionType.OTHER -> Color.Black
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier =  centerWithPaddingModifier.padding(top = 8.dp)
        )

        Text(
            text =
            if (transaction.transactionType == TransactionType.DEBIT) {
                transaction.transferDetail.toClient.displayName
            } else {
                transaction.transferDetail.fromClient.displayName
            },
            fontSize = 20.sp,
            modifier =  centerWithPaddingModifier.padding(top = 8.dp)
        )
    }
}


@Composable
fun showInfoTexts(
    transaction: Transaction,
    context:Context,
    uri:String,
    receiptUtils:ReceiptUtils
){
    Column(
        modifier = Modifier
            .padding(horizontal = 30.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ){
        Text(
            text = stringResource(R.string.transaction_id),
            fontSize = 20.sp
        )
        Text(
            text = transaction.transactionId.toString(),
            fontSize = 20.sp
        )
        Text(
            text = stringResource(R.string.transaction_date),
            fontSize = 20.sp
        )
        Text(
            text = transaction.date.toString(),
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.size(height = 30.dp, width = 14.dp))
        Text(
            text = stringResource(R.string.to),
            fontSize = 20.sp
        )
        Text(
            text = stringResource(R.string.name) + transaction.transferDetail.toClient.displayName,
            fontSize = 20.sp
        )
        Text(
            text = stringResource(R.string.account_number) + transaction.transferDetail.toAccount.accountNo,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.size(height = 50.dp, width = 14.dp))

        Text(
            text = stringResource(R.string.from),
            fontSize = 20.sp
        )
        Text(
            text = stringResource(R.string.name) + transaction.transferDetail.fromClient.displayName,
            fontSize = 20.sp
        )
        Text(
            text = stringResource(R.string.account_number) + transaction.transferDetail.fromAccount.accountNo,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.size(height = 50.dp, width = 14.dp))

        Text(
            text = stringResource(R.string.uri),
            fontSize = 20.sp
        )
    }
    bottomIcons(transaction,context,uri, receiptUtils )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun bottomIcons(
    transaction: Transaction,
    context:Context,
    uri:String,
    receiptUtils: ReceiptUtils
){
    FlowRow (
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp)
    ){
        Text(
            text = uri,
            style = MaterialTheme.typography.headlineSmall
        )

        Icon(
            Icons.Filled.ContentCopy,
            contentDescription = stringResource(R.string.copy),
            modifier = Modifier
                .size(25.dp)
                .clickable { receiptUtils.copyReceiptLink(uri, context) },
            tint = Color.Black
        )

        val to = stringResource(R.string.to)
        val shareMessage = stringResource(R.string.receipt_sharing_message)
        val colon =  stringResource(R.string.colon)
        Icon(
            Icons.Filled.Share,
            contentDescription = stringResource(R.string.share_receipt),
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(25.dp)
                .clickable {
                    receiptUtils.shareReceiptLink(
                        shareMessage =
                        shareMessage
                                + transaction.transferDetail.fromClient.displayName
                                + to
                                + transaction.transferDetail.toClient.displayName
                                + colon
                                + uri.trim { it <= ' ' },
                        context
                    )
                },
            tint = Color.Black
        )
    }
}

@Preview
@Composable
fun ReceiptPreview() {
    val context = LocalContext.current
    Receipt(
                rUiState = ReceiptUiState.TransactionReceipt(transaction = Transaction()),
                transaction = Transaction(),
                context,
                uri = "",
                receiptUtils = ReceiptUtils(context),
                viewModel = hiltViewModel(),
            )
}


