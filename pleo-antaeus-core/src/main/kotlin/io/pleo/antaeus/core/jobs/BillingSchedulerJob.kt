package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.services.BillingService
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext

class BillingSchedulerJob : Job {

    private val logger = KotlinLogging.logger{}

    override fun execute(context: JobExecutionContext) {
        logger.info { "Started executing billing job service" }
        val service = context.jobDetail.jobDataMap["billingService"] as BillingService
        service.billPendingInvoices()
        logger.info { "Finished executing billing job service" }
    }
}