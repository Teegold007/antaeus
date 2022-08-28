package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {

    private val logger = KotlinLogging.logger("BillingService");
    private val billingScope = CoroutineScope(Dispatchers.IO)


    private val maxRetries : String = System.getenv("MAX_RETRIES") ?: "3";

    fun billPendingInvoices(): List<Invoice> {
        val invoices = invoiceService.fetchByStatus("PENDING")
        invoices.forEach{
            billingScope.launch {
               val billingFlow = processInvoice(it)
                billingFlow.collect()
                }
            }
        return invoiceService.fetchByStatus(InvoiceStatus.PAID.toString())
    }
    fun billInvoicesByStatus(status: String): List<Invoice> {
        if(status != InvoiceStatus.PAID.toString()){
            val invoices = invoiceService.fetchByStatus(status)
            invoices.forEach{
                billingScope.launch {
                    val billingFlow = processInvoice(it)
                    billingFlow.collect()
                }
            }
        }

        return invoiceService.fetchByStatus(status);
    }

    fun billInvoice(id : Int) : Invoice {
        val invoice = invoiceService.fetch(id);
        billingScope.launch {
            if(invoice.status != InvoiceStatus.PAID){
                val billingFlow = processInvoice(invoice)
                billingFlow.collect()
            }
        }
        return invoice;
    }

    private fun processInvoice(invoice : Invoice) : Flow<Result<Boolean>> {
        return flow {
            val paymentProviderResponse = paymentProvider.charge(invoice)
            when(paymentProviderResponse){
                true -> { invoiceService.updateStatus(invoice.id, InvoiceStatus.PAID.toString()) }
                false -> {
                    //you can send a notification to the user to Fund Account
                    sendFundAccountNotification(invoice.customerId);
                    invoiceService.updateStatus(invoice.id, InvoiceStatus.UNPAID.toString()) }
            }
            emit(Result.success(paymentProviderResponse))
        }.retryWhen { cause, attempt ->
            if (cause is NetworkException && attempt < maxRetries.toInt()) {
                //A monitoring metrics can be added here
                logger.error ("retrying to charge invoice ")
                return@retryWhen true
            } else if(cause is CustomerNotFoundException) {
                //Report this to the proper channel by creating a ticket on jira or use Datadog
                logger.error(cause) { CustomerNotFoundException(invoice.customerId) }
                invoiceService.updateStatus(invoice.id, InvoiceStatus.INVALID_CUSTOMER.toString())
                return@retryWhen false
            } else if(cause is CurrencyMismatchException) {
                //Report this to the proper channel by creating a ticket on jira or use Datadog
                logger.error(cause) { CurrencyMismatchException(invoice.id, invoice.customerId) }
                invoiceService.updateStatus(invoice.id, InvoiceStatus.CURRENCY_MISMATCH.toString())
                return@retryWhen false
            } else {
                //This should be reported
                logger.error(cause) { NetworkException() }
                invoiceService.updateStatus(invoice.id, InvoiceStatus.FAILED.toString())
                return@retryWhen false
            }
        }
    }

    private fun sendFundAccountNotification(customerId : Int){
        //send email to customer to inform them about insufficient balance
    }
}




