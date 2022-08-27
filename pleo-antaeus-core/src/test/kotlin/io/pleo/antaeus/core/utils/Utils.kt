package io.pleo.antaeus.core.utils

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import kotlin.random.Random

internal fun createInvoice(invoiceStatus: InvoiceStatus) : Invoice{
    return Invoice(id= Random.nextInt(),customerId = Random.nextInt(),status = invoiceStatus,amount = createMoney())
}
internal fun createInvoices(invoiceStatus: InvoiceStatus) : List<Invoice>{
    return List(size = 5) {Invoice(id= Random.nextInt(),customerId = Random.nextInt(),status = invoiceStatus,amount = createMoney())}
}

internal fun createMoney() : Money {
    return Money(value = BigDecimal(Random.nextDouble(5.0,100.0)),currency = Currency.values()[Random.nextInt(4)]);
}