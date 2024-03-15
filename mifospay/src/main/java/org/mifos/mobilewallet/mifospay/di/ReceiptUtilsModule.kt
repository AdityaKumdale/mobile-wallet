package org.mifos.mobilewallet.mifospay.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.mifos.mobilewallet.mifospay.receipt.ReceiptUtils

@Module
@InstallIn(SingletonComponent::class)
object ReceiptUtilsModule {
    @Provides
    fun provideReceiptUtils(@ApplicationContext context: Context): ReceiptUtils {
        return ReceiptUtils(context)
    }
}