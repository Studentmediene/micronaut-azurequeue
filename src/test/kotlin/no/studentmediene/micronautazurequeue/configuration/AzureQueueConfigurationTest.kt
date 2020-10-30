package no.studentmediene.micronautazurequeue.configuration

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class AzureQueueConfigurationTest {
  @Test
  fun `Should correctly discover queue properties`() {
    val properties = mapOf(
      "azure.queue.test-client.enabled" to "true",
      "azure.queue.test-client.queueName" to "test-queue",
      "azure.queue.test-client.handler" to "SomeHandler",
      "azure.queue.test-client.connectionString" to "Some connection string"
    )

    val context = ApplicationContext.build(properties)
      .packages("no.studentmediene.micronautazurequeue")
      .build()
      .start()

    val configurations = context
      .getBeansOfType(AzureQueueConfiguration::class.java)
      .filter { it.label == "test-client" }

    assertEquals(1, configurations.size)
    val config = configurations.first()

    assertEquals(true, config.enabled)
    assertEquals("test-queue", config.queueName)
    assertEquals("SomeHandler", config.handler)
    assertEquals("Some connection string", config.connectionString)
  }

  @Test
  fun `Should discover multiple queue configurations`() {
    val properties = mapOf(
      "azure.queue.test-client-1.enabled" to "true",
      "azure.queue.test-client-1.queueName" to "test-queue",
      "azure.queue.test-client-1.handler" to "SomeHandler",
      "azure.queue.test-client-1.connectionString" to "Some connection string",

      "azure.queue.test-client-2.enabled" to "true",
      "azure.queue.test-client-2.queueName" to "test-queue",
      "azure.queue.test-client-2.handler" to "SomeHandler",
      "azure.queue.test-client-2.connectionString" to "Some connection string"
    )

    val context = ApplicationContext.build(properties)
      .packages("no.studentmediene.micronautazurequeue")
      .build()
      .start()

    val configurations = context
      .getBeansOfType(AzureQueueConfiguration::class.java)
      .filter { it.label.startsWith("test-client") }

    assertEquals(2, configurations.size)

    assertNotNull(configurations.find { it.label == "test-client-1" })
    assertNotNull(configurations.find { it.label == "test-client-2" })
  }
}
