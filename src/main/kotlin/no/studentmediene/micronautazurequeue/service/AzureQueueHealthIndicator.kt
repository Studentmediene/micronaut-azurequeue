package no.studentmediene.micronautazurequeue.service

import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthIndicator
import io.micronaut.management.health.indicator.HealthResult
import io.micronaut.runtime.event.annotation.EventListener
import io.reactivex.Flowable
import no.studentmediene.micronautazurequeue.event.AzureQueueConsumerEvent
import no.studentmediene.micronautazurequeue.event.AzureQueueConsumerEventType
import org.reactivestreams.Publisher
import javax.inject.Singleton

/**
 * Gives the application's liveliness probe information about
 * whether the queue polling is alive, which can notify any orchestrator
 * to restart the application in case of failures.
 *
 * @property startedQueueConsumers List of Azure queues which is still alive
 * @property stoppedQueueConsumers List of Azure queues which has failed and is disabled
 * */
@Singleton
internal class AzureQueueHealthIndicator : HealthIndicator {
  private val startedQueueConsumers = mutableListOf<AzureQueueConsumer>()
  private val stoppedQueueConsumers = mutableListOf<AzureQueueConsumer>()

  override fun getResult(): Publisher<HealthResult> {
    return Flowable.just(AzureQueueHealthResult(startedQueueConsumers, stoppedQueueConsumers))
  }

  @EventListener
  fun onQueueConsumerChange(event: AzureQueueConsumerEvent) {
    when (event.type) {
      AzureQueueConsumerEventType.STARTED -> startedQueueConsumers.add(AzureQueueConsumer(event.label))
      AzureQueueConsumerEventType.STOPPED -> {
        val consumer = AzureQueueConsumer(event.label)
        stoppedQueueConsumers.add(consumer)
        startedQueueConsumers.remove(consumer)
      }
    }
  }
}

private data class AzureQueueConsumer(
  val label: String
)

private data class AzureQueueHealthResult(
  private val startedConsumers: List<AzureQueueConsumer>,
  private val stoppedConsumers: List<AzureQueueConsumer>
) : HealthResult {

  override fun getStatus(): HealthStatus = when (stoppedConsumers.size) {
    0 -> HealthStatus.UP
    else -> HealthStatus.DOWN
  }

  override fun getName(): String = "azure-storage-queue"

  override fun getDetails(): Any = mapOf(
    "started" to startedConsumers,
    "stopped" to stoppedConsumers
  )
}
