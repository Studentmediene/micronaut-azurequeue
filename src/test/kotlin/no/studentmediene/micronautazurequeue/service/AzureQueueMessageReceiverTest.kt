package no.studentmediene.micronautazurequeue.service

import com.azure.storage.queue.QueueAsyncClient
import com.azure.storage.queue.models.QueueMessageItem
import io.micronaut.context.event.ApplicationEventPublisher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.studentmediene.micronautazurequeue.DummyHandler
import no.studentmediene.micronautazurequeue.configuration.AzureQueueConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

internal class AzureQueueMessageReceiverTest {
  private val azureQueueClient = mockk<QueueAsyncClient>()
  private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
  private val handler = DummyHandler()

  private lateinit var configuration: AzureQueueConfiguration
  private lateinit var receiver: AzureQueueMessageReceiver

  @BeforeEach
  fun setup() {
    configuration = AzureQueueConfiguration(
      label = "test-queue"
    ).apply {
      enabled = true
      connectionString = "someconfigurationString"
      queueName = "test-queue"
      handler = "DummyHandler"
    }

    receiver = AzureQueueMessageReceiver(
      configuration = configuration,
      queueClient = azureQueueClient,
      handler = handler,
      eventPublisher = eventPublisher
    )
    handler.reset()

    every { azureQueueClient.deleteMessage(any(), any()) } returns Mono.empty()
    every { azureQueueClient.queueName } returns configuration.queueName
  }

  @Test
  fun `Should notify handler of new message`() {
    val testMessages = listOf(
      QueueMessageItem().setMessageId("test-1").setMessageText("Test message 1").setPopReceipt("pop-1"),
      QueueMessageItem().setMessageId("test-2").setMessageText("Test message 1").setPopReceipt("pop-2")
    )

    every { azureQueueClient.receiveMessages(any()).toIterable() } returns testMessages

    receiver.receiveAndProcessMessages()

    assertEquals(2, handler.receivedMessages.size)

    testMessages.forEach {
      verify(exactly = 1) { azureQueueClient.deleteMessage(it.messageId, it.popReceipt) }
    }
  }

  @Test
  fun `Should handle base64 encoded message`() {
    val testMessage = QueueMessageItem()
      .setMessageId("test-1")
      .setMessageText("QmFzZTY0IGVuY29kZWQgbWVzc2FnZQ==")
      .setPopReceipt("pop-1")

    every { azureQueueClient.receiveMessages(any()).toIterable() } returns listOf(testMessage)

    receiver.receiveAndProcessMessages()

    assertEquals("Base64 encoded message", handler.receivedMessages.first())
  }

  @Test
  fun `Should disable queue-polling when too many failures has occurred`() {
    configuration.pollingFailureWaitTimeSeconds = 1
    configuration.disableAfterNumberOfFails = 2
    // Take a single message each time,
    // because we fail by number of requests and not amount of messages polled
    configuration.numberOfMessagesToPoll = 1

    val failingMessages = (0..configuration.disableAfterNumberOfFails + 1)
      .map {
        QueueMessageItem()
          .setMessageId("failing-$it")
          .setMessageText("--fail-me--")
          .setPopReceipt("pop-failing-$it")
      }

    failingMessages
      .chunked(configuration.numberOfMessagesToPoll)
      .forEach {
        every { azureQueueClient.receiveMessages(any()).toIterable() } returns it
        receiver.receiveAndProcessMessages()
      }

    assertFalse(configuration.enabled)
  }
}
