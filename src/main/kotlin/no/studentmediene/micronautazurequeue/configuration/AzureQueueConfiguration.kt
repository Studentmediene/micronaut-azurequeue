package no.studentmediene.micronautazurequeue.configuration

import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.core.annotation.Introspected

/**
 * Represents the configurations of a single instance of
 * an azure queue.
 *
 * @property label Symbolic name for the queue. This is the dictionary key in `azure.queue.<key>`
 * @property enabled Tells any queue orchestrator whether this queue is active or not.
 *                   Disabled queues will no be polled from
 * @property queueName Name of the queue in Azure
 * @property connectionString Used to authenticate against the queue
 * @property handler Simple class name of the handler which should be used to handle message
 *                   for this specific queue. The handler should be a [javax.inject.Singleton]
 * @property disableAfterNumberOfFails Amount of consecutive failures before this queue is disabled
 * @property numberOfMessagesToPoll Amount of messages to poll on each iteration
 * @property pollWaitTimeSeconds How long we should wait before polling the next messages
 * @property pollingFailureWaitTimeSeconds How long to wait after a failure has occurred, before polling again
 * */
@Introspected
@EachProperty("azure.queue")
internal data class AzureQueueConfiguration(@param:Parameter val label: String) {
  var enabled: Boolean = false
  lateinit var queueName: String
  lateinit var connectionString: String
  lateinit var handler: String

  var disableAfterNumberOfFails = QUEUE_DISABLE_AFTER_NUMBER_OF_FAILS
  var numberOfMessagesToPoll = QUEUE_MAX_NUMBER_OF_MESSAGES
  var pollWaitTimeSeconds = QUEUE_DEFAULT_WAIT_SECONDS
  var pollingFailureWaitTimeSeconds = ERROR_PAUSE_DURATION_MILLISECONDS

  companion object {
    /**
     * @var Amount of messages to poll each time
     * */
    const val QUEUE_MAX_NUMBER_OF_MESSAGES = 10

    /**
     * @var Amount of consecutive fails before the polling should be disabled
     *      to prevent spamming of endpoint
     * */
    const val QUEUE_DISABLE_AFTER_NUMBER_OF_FAILS = 10

    /**
     * @var How long should we by default wait before polling.
     *      Can be changed by rate-limiting strategies, etc.
     * */
    const val QUEUE_DEFAULT_WAIT_SECONDS = 10L

    const val ERROR_PAUSE_DURATION_MILLISECONDS = 30000L
  }

  override fun toString(): String = "AzureQueueConfiguration(label=$label, connectionString=$connectionString, enabled=$enabled)"
}
