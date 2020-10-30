package no.studentmediene.micronautazurequeue.service

import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.discovery.event.ServiceReadyEvent
import io.micronaut.runtime.event.annotation.EventListener
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.annotation.PreDestroy
import javax.inject.Inject
import javax.inject.Singleton

private const val THREAD_KEEP_ALIVE_TIME_SECONDS = 10L

/**
 * Is responsible for activating and de-activating each [AzureQueueMessageReceiver],
 * so that they can start polling for messages concurrently.
 *
 * It is also responsible for doing any cleanup before a server i shutdown.
 *
 * @property receivers Collection of individual receivers. Each symbolizes an individual Azure Queue
 * */
@Singleton
@Requires(notEnv = [Environment.TEST])
internal class AzureQueueMessageScheduler(
  @Inject private val receivers: List<AzureQueueMessageReceiver>
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val workQueue = LinkedBlockingDeque<Runnable>()
  /**
   * @var Pool of threads which each message receiver can run in
   * */
  private val queuePollExecutor = ThreadPoolExecutor(receivers.size, receivers.size, THREAD_KEEP_ALIVE_TIME_SECONDS, TimeUnit.SECONDS, workQueue)

  /**
   * When micronaut is ready will this setup all Queue clients
   * be setup if they are active
   * */
  @EventListener
  fun onQueuePollStartup(event: ServiceReadyEvent) {
    log.trace("Setting up Azure Queue polling")
    receivers.forEach {
      if (it.configuration.enabled) {
        log.info("[${it.configuration.label}] is enabled. Starting...")
        queuePollExecutor.execute { it.startPollingMessages() }
      } else {
        log.info("[${it.configuration.label}] is disabled")
      }
    }
  }

  @PreDestroy
  fun onSystemShutdown() {
    log.info("Turning of Azure Queue polling")
    receivers.forEach { it.disable() }
  }
}
