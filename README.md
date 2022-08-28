## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

### PROLOGUE 😁!
* I am new to Kotlin, so I might not have written the most optimal kotlin code I believe I will get used to it with time if given the opportunity.


##REST ENDPOINTS
* An Endpoint was added to allow fetching of invoices by their status using this path `/rest/v1/invoices/status/{:status}` the endpoint returns a list of invoices with the status `{:status}`.
Invoices can have 5 statuses `PENDING`, `FAILED`,`PAID`,`UNPAID`, `CURRENCY_MISMATCH` and `INVALID_CUSTOMER`
* An Endpoint to allow the manual billing of single invoice with invoice_id  using this path `/rest/v1/invoice/{:id}/bill`
* An Endpoint to allow the manual billing of all invoices with a specific status that's not `PAID` using this path `/rest/v1/invoices/status/{:status}/bill`. The main aim of this endpoint is to bill invoices that are not `PENDING` or `PAID` it's a fail-safe for billPendingInvoice cron job. i.e if some invoices where not billed during the cron job an admin can trigger manually.
* An Endpoint to allow the manual billing of all invoices with status=`PENDING` using this path `/rest/v1/invoices/status/pending-invoices/bill`


### Billing Service
* public billInvoice -> This method takes in invoice id
    * fetch invoice by id, if not found, a NotFoundException is cascaded from the invoiceService
    * check the status of the invoice to ensure that it's still pending,
        * if status is pending, call the private processInvoice method asynchronously and return a message; This is because the process of charging may take time.
        
        
* billPendingInvoices -> Fetches all the pending invoices in the database and sends them to processInvoice to charge. This method leveraged on coroutineScope for concurrency.
* billInvoicesByStatus -> Fetches all invoices by status in the database and if the status is not `PAID` it sends them to processInvoice to charge.
* private processInvoice -> This method tries to charge an invoice
    * if invoice charge is successful then update the invoice status to `PAID`
    * if there is a CustomerNotFoundException or CurrencyMismatchException the invoice status is updated to `INVALID_CUSTOMER`,`CURRENCY_MISMATCH` respectively, log it and maybe send it to datadog for proper alert. This 2 exceptions were handled separately in case business wants to have a custom action per exception type
    * if a network error occurs from the payment provider, retry for a specified number of times (the max number of times can be configured via an Environmental variable [MAX_RETRIES]); This is because NetworkException may be temporal.
        * If the request has been retried max retry times as configured[MAX_RETRIES], and the payment provider still failed with NetworkException, the status of the invoice is updated to `FAILED` and we can send some metrics to datadog to capture the failure rate of the payment provider due to this error. This will help in ensuring that we have enough visibility to either change the provider based on SLA breached or probe further
  * if the charge is not successful (i.e due to insufficient balance) the status of the invoice is updated to `UNPAID`, an email could be sent to the customer to fund their account

*Other Suggestions:
* When processing the invoices, we can leverage on queueing system to help scale the number of invoices that can be processed at once. Also, to ensure that we cater for system failures.
* We can also load the  invoices in batches, so the system has enough resources to process and can recover quickly from failure.If we have lots of invoices, it would be too much for us to load all of them at once before processing.

##Billing Scheduler

* BillingSchedulerConfig is the entry point of the cron,The cron is configurable via environmental variable `[INVOICE_BILLING_CRON]`
  this implementation has :
    * Job – Represents the actual job to be executed
    * JobDetail – Conveys the detail properties of a given Job instance
    * Trigger – Triggers are the mechanism by which Jobs are scheduled
* BillingSchedulerJob contains the actual implementation that calls the BillingService.billPendingInvoices() method  

##Billing Scheduler Suggestion
- In most use cases, we would want to disallow the execution of more than one instances of the same job at the same time, to prevent race conditions on saved data. This might occur when the jobs take too long to finish or are triggered too often.


- In order to properly diagnose and trace issues in applications that use Quartz well Any code that gets executed inside jobs must be logged.Quartz has its own logs when an event occurs i.e. a scheduler gets created, a job gets executed etc.


- Scheduler monitor/manager like [QuartzDesk](https://www.quartzdesk.com/) is important when the application is deployed to live quartz scheduler GUI helps us manage and monitor Quartz schedulers, jobs and triggers in all types of Java applications.


## InvoiceService / AntaeusDal
* fetchByStatus - Fetches invoices by status from the database


* updateStatus - Update's invoice status by fetching the invoice using the invoice id, if the invoice is found it updates the status or else it throws an `InvoiceNotFoundException`  

##EXTRAS
Additional Dependencies :-
* Quartz scheduler dependencies - quartz scheduler for scheduling.
* junit.jupiter dependencies - for running unit test.
* kotlinx-coroutines dependencies