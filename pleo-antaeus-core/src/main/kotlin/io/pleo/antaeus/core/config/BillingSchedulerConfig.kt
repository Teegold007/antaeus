package io.pleo.antaeus.core.config

import io.pleo.antaeus.core.jobs.BillingSchedulerJob
import io.pleo.antaeus.core.services.BillingService
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory

class BillingSchedulerConfig {
    fun initialize(billingService: BillingService,cronExpression: String) {
        val jobDetail: JobDetail = JobBuilder.newJob(BillingSchedulerJob::class.java)
            .build()
        jobDetail.jobDataMap["billingService"] = billingService
        val trigger: Trigger = TriggerBuilder.newTrigger()
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
            .build()
        val scheduler: Scheduler = StdSchedulerFactory().scheduler
        scheduler.start()
        scheduler.scheduleJob(jobDetail, trigger)
    }
}