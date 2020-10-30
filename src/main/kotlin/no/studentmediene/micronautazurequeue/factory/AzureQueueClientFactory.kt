package no.studentmediene.micronautazurequeue.factory

import com.azure.storage.queue.QueueAsyncClient
import com.azure.storage.queue.QueueServiceClientBuilder
import io.micronaut.context.annotation.EachBean
import io.micronaut.context.annotation.Factory
import no.studentmediene.micronautazurequeue.configuration.AzureQueueConfiguration
import org.slf4j.LoggerFactory

@Factory
internal class AzureQueueClientFactory {
  private val log = LoggerFactory.getLogger(this::class.java)

  @EachBean(AzureQueueConfiguration::class)
  fun getAzureQueueClient(configuration: AzureQueueConfiguration): QueueAsyncClient {
    log.trace("Discovered configuration ${configuration.label}, for queue ${configuration.queueName}")

    val connectionString = configuration.connectionString
    check(connectionString.isNotBlank()) {
      "Received an azure queue configuration (${configuration.label}) " +
        "which is missing a connection string"
    }

    val queueName = configuration.queueName
    check(queueName.isNotBlank()) {
      "Received an azure-queue configuration (${configuration.label}) " +
        "which is missing a queue-name"
    }

    return queueServiceAsyncClient(connectionString).getQueueAsyncClient(queueName)
  }

  private fun queueServiceAsyncClient(connectionString: String) = QueueServiceClientBuilder()
    .connectionString(connectionString)
    .buildAsyncClient()
}
