package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.utils.createInvoice
import io.pleo.antaeus.core.utils.createInvoices
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.InvoiceStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BillingServiceTest {
    private val pendingInvoices = createInvoices(InvoiceStatus.PENDING)
    private val paidInvoice = createInvoice(InvoiceStatus.PAID)
    private val paymentProvider = mockk<PaymentProvider> {}
    private val dal = mockk<AntaeusDal> {
        every { updateInvoiceStatus(any(), any()) } returns paidInvoice
        every { fetchInvoicesByStatus(any()) } returns pendingInvoices
    }

    private val invoiceService = InvoiceService(dal = dal)

    private val billingService = BillingService(paymentProvider = paymentProvider, invoiceService = invoiceService)


    @Test
    fun `will bill customer invoice and update status as paid`() {
        val invoice = createInvoice(InvoiceStatus.PENDING)

        val paymentProvider = mockk<PaymentProvider> { every { charge(any()) } returns true }
        val billingService = BillingService(paymentProvider, invoiceService)
        val result = billingService.billInvoice(invoice.id)
        Thread.sleep(20)
        verify(exactly = 1) { invoiceService.updateStatus(result.id, InvoiceStatus.PAID.toString()) }
    }

    @Test
    fun `it does not bill a paid invoice`() {
        val invoice = paidInvoice
        every { invoiceService.fetch(invoice.id) } returns invoice
        billingService.billInvoice(paidInvoice.id)
        verify(exactly = 1) { invoiceService.fetch(paidInvoice.id) }
        verify(exactly = 0) { paymentProvider.charge(paidInvoice) }
        verify(exactly = 0) { invoiceService.updateStatus(paidInvoice.id, InvoiceStatus.PAID.toString()) }

    }

    @Test
    fun `will bill invoices status to PAID when invoices are charged successfully`(){
        val paymentProvider = mockk<PaymentProvider> { every { charge(any()) } returns true }
        val billingService = BillingService(paymentProvider, invoiceService)
        billingService.billInvoicesByStatus("PENDING")

        verify(exactly = 5) {
            invoiceService.updateStatus(any(), InvoiceStatus.PAID.toString())
        }

    }

    @Test
    fun `will bill customer invoice and  update status as failed`(){
        val paymentProvider = mockk<PaymentProvider> { every { charge(any()) } returns false }
        val billingService = BillingService(paymentProvider, invoiceService)
        billingService.billPendingInvoices()

        verify(exactly = 5) {
            invoiceService.updateStatus(any(), InvoiceStatus.UNPAID.toString())
        }

    }

    @Test
    fun `updates invoice status to currency mismatch if CurrencyMismatchException is thrown`() {
        val paymentProvider = mockk<PaymentProvider> { every { charge(any()) } throws CurrencyMismatchException(1, 1) }
        val billingService = BillingService(paymentProvider, invoiceService)
        billingService.billPendingInvoices()
        Thread.sleep(10)

        assertThrows<CurrencyMismatchException> {
            paymentProvider.charge(pendingInvoices[1])
        }

        verify(exactly = 5) {
            invoiceService.updateStatus(any(), InvoiceStatus.CURRENCY_MISMATCH.toString())
        }
    }

    @Test
    fun `updates invoice status to invalid customer if CustomerNotFoundException is thrown`() {
        val paymentProvider = mockk<PaymentProvider> { every { charge(any()) } throws CustomerNotFoundException(1) }
        val billingService = BillingService(paymentProvider, invoiceService)
        billingService.billPendingInvoices()
        Thread.sleep(10)
        assertThrows<CustomerNotFoundException> {
            paymentProvider.charge(pendingInvoices[1])
        }
        verify(exactly = 5) {
            invoiceService.updateStatus(any(), InvoiceStatus.INVALID_CUSTOMER.toString())
        }
    }

    @Test
    fun `updates invoice status to failed  if NetworkException is thrown`() {
        val paymentProvider = mockk<PaymentProvider> { every { charge(any()) } throws NetworkException() }
        val billingService = BillingService(paymentProvider, invoiceService)
        billingService.billPendingInvoices()
        Thread.sleep(30)
        assertThrows<NetworkException> {
            paymentProvider.charge(pendingInvoices[1])
        }
        verify(exactly = 5) {
            invoiceService.updateStatus(any(), InvoiceStatus.FAILED.toString())
        }
    }


}