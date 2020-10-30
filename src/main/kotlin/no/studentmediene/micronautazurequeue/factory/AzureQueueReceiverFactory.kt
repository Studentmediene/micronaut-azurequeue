package no.studentmediene.micronautazurequeue.factory

import com.azure.storage.queue.QueueAsyncClient
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.EachBean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.inject.qualifiers.Qualifiers
import no.studentmediene.micronautazurequeue.AzureQueueMessageHandler
import no.studentmediene.micronautazurequeue.configuration.AzureQueueConfiguration
import no.studentmediene.micronautazurequeue.service.AzureQueueMessageReceiver
import org.slf4j.LoggerFactory

@Factory
internal class AzureQueueReceiverFactory(
  private val applicationContext: ApplicationContext,
  private val applicationEventPublisher: ApplicationEventPublisher
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @EachBean(AzureQueueConfiguration::class)
  fun getAzureQueueMessageHandler(configuration: AzureQueueConfiguration): AzureQueueMessageReceiver {
    log.info("Creating queue message-receiver ${configuration.label}")
    val handler = applicationContext.getBean(AzureQueueMessageHandler::class.java, Qualifiers.byName(configuration.handler))

    log.trace("Discovered handler ${handler::class.qualifiedName}")
    val queueClient = applicationContext.getBean(QueueAsyncClient::class.java, Qualifiers.byName(configuration.label))
    log.trace("Discovered matching client ${queueClient::class.qualifiedName}")

    return AzureQueueMessageReceiver(configuration, queueClient, handler, applicationEventPublisher)
  }
}
