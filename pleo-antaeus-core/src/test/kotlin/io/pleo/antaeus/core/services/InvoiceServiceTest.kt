package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.utils.createInvoice
import io.pleo.antaeus.core.utils.createInvoices
import io.pleo.antaeus.core.utils.createMoney
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

class InvoiceServiceTest {
    private val invoices = createInvoices(InvoiceStatus.PENDING);

    private val paidInvoice = createInvoice(InvoiceStatus.PAID)

    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
        every { fetchInvoice(any()) } returns paidInvoice
        every { fetchInvoices() } returns invoices
        every { fetchInvoicesByStatus("PENDING") } returns invoices
    }



    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will fetch an invoice`() {
        assertNotNull(invoiceService.fetch(1));
    }

    @Test
    fun `will fetch invoices by status`() {
        val invoices = invoiceService.fetchByStatus("PENDING")
        assertTrue(invoices.size==5);
    }

    @Test
    fun `update invoice status`(){
        every { dal.updateInvoiceStatus(1,InvoiceStatus.PAID.toString()) } returns paidInvoice
        invoiceService.updateStatus(1,InvoiceStatus.PAID.toString())
        verify(exactly=1) { dal.updateInvoiceStatus(1,InvoiceStatus.PAID.toString())}
    }
}
