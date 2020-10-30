package no.studentmediene.micronautazurequeue.event

enum class AzureQueueConsumerEventType {
  STARTED,
  STOPPED
}

class AzureQueueConsumerEvent(
  val label: String,
  val queueName: String,
  val type: AzureQueueConsumerEventType
)
