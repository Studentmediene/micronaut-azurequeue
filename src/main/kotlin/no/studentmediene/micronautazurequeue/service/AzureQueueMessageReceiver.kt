package no.studentmediene.micronautazurequeue.service

import com.azure.storage.queue.QueueAsyncClient
import com.azure.storage.queue.models.QueueMessageItem
import io.micronaut.context.event.ApplicationEventPublisher
import no.studentmediene.micronautazurequeue.AzureQueueMessageHandler
import no.studentmediene.micronautazurequeue.configuration.AzureQueueConfiguration
import no.studentmediene.micronautazurequeue.event.AzureQueueConsumerEvent
import no.studentmediene.micronautazurequeue.event.AzureQueueConsumerEventType
import org.slf4j.LoggerFactory
import java.util.*

private const val SECONDS_IN_MILLISECONDS = 1000L

internal class AzureQueueMessageReceiver(
  val configuration: AzureQueueConfiguration,
  private val queueClient: QueueAsyncClient,
  private val handler: AzureQueueMessageHandler,
  private val eventPublisher: ApplicationEventPublisher
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private var numberOfFails = 0

  fun startPollingMessages() {
    if (configuration.enabled) {
      log.info("Queue polling is active for ${configuration.label}")
      publishStartedQueue()
    } else {
      log.info("Queue polling is disabled for ${configuration.label}")
    }

    while (configuration.enabled) {
      log.trace("Polling next ${configuration.numberOfMessagesToPoll} messages from queue: ${queueClient.queueName} ...")
      receiveAndProcessMessages()

      log.trace("Waiting ${configuration.pollWaitTimeSeconds} seconds before polling again")
      Thread.sleep(configuration.pollWaitTimeSeconds * SECONDS_IN_MILLISECONDS)
    }
  }

  @Suppress("TooGenericExceptionCaught")
  internal fun receiveAndProcessMessages() {
    try {
      val messages = queueClient
        .receiveMessages(configuration.numberOfMessagesToPoll)
        // To iterable ensures that we block between each .next() call
        .toIterable()
        .map { processMessage(it) }

      log.trace("Polled ${messages.size} messages")
      resetFailure()
    } catch (ex: Exception) {
      log.error("[${configuration.label}] An exception occurred during polling", ex)
      incrementFailure()

      // We do not wish to overly stress our queue system
      if (hasReachedFailureThreshold()) {
        log.error("[${configuration.label}] Reached max number of failures. Disabling")
        disable()
        // Notify any listeners that this queue has failed
        publishFailedAndStoppedQueue()
      } else {
        log.info("[${configuration.label}] Pausing for ${configuration.pollingFailureWaitTimeSeconds} seconds")
        Thread.sleep(configuration.pollingFailureWaitTimeSeconds * SECONDS_IN_MILLISECONDS)
      }
    }
  }

  /**
   * Will base64 decode that message and pass the payload
   * to our message handler of type [AzureQueueMessageHandler].
   *
   * If the message handler does not fail, then we will remove the de-queued message
   * from the queue (Messages are not automatically deleted from the queued).
   *
   * NOTE: This is internal because our tests call [processMessage],
   *       such that we can skip the
   * */
  private fun processMessage(message: QueueMessageItem) {
    log.trace("Received message: ${message.messageText}")

    // Attempt to base64 decode the message or just default to the plain message
    val parsedPayload = runCatching { message.messageText.base64Decode() }
      .fold(
        { message.messageText.base64Decode() },
        { message.messageText }
      )

    handler.onMessage(parsedPayload)

    log.trace("Deleting successfully de-queued message: ${message.messageId}")
    queueClient.deleteMessage(message.messageId, message.popReceipt).block()
  }

  fun disable() {
    configuration.enabled = false
  }

  private fun publishStartedQueue() {
    eventPublisher.publishEvent(
      AzureQueueConsumerEvent(configuration.label, configuration.queueName, AzureQueueConsumerEventType.STARTED)
    )
  }

  private fun publishFailedAndStoppedQueue() {
    eventPublisher.publishEvent(
      AzureQueueConsumerEvent(configuration.label, configuration.queueName, AzureQueueConsumerEventType.STOPPED)
    )
  }

  private fun String.base64Decode(): String = Base64
    .getDecoder()
    .decode(this)
    .let { String(it, Charsets.UTF_8) }

  private fun hasReachedFailureThreshold() = numberOfFails >= configuration.disableAfterNumberOfFails

  private fun incrementFailure() {
    numberOfFails += 1
  }

  private fun resetFailure() {
    numberOfFails = 0
  }
}
