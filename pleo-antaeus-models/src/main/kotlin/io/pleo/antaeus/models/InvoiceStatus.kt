package io.pleo.antaeus.models

enum class InvoiceStatus {
    PENDING,
    PAID,
    FAILED,
    UNPAID,
    CURRENCY_MISMATCH,
    INVALID_CUSTOMER
}
