Micronaut Azure Queue
===

Making Queue-polling from Azure to Micronaut easy.

## Installation

```
repositories {
  maven {
    url = "https://dl.bintray.com/studentmediene/micronaut-azurequeue"
  }
}

dependencies {
    implementation("no.studentmediene:micronaut-azurequeue:0.0.1")
}
```

## Configuration

This library naturally follows Micronaut configuration conventions. 
You can configure one or multiple queues in `src/main/kotlin/resources/application.yml`

```yaml
# application.yml
micronaut:
  # ...

# ...

azure:
  queue:
    queue1:
      # (optional) Explicitly enable or disable a queue.
      enabled: false
      # (required) Name of the Azure Storage Queue
      queueName: my-queue-1
      # (required) Connection string we use to connect to Azure Storage Queue
      connectionString: ...
      # (required) Simple class-name of the module handling
      # messages polled from Azure Storage Queue.
      # It has to implement the interface AzureQueueMessageHandler
      handler: MyQueue1Handler
    queue2:
      # Configurations for queue2 ...
    # Declare any more queues  ...
```

A simple queue handler can look as follows:

```kotlin
import no.studentmediene.micronautazurequeue.AzureQueueMessageHandler
import javax.inject.Singleton

// (optional) Declare the class as a singleton
@Singleton
internal class MyQueue1Handler(
  // (optional) Inject any dependencies you require
) : AzureQueueMessageHandler {
  
  // Called for each polled message
  override fun onMessage(message: String) {
    println("Received message: $message") 
  } 
}
```

## Features

### Retry in case of failures

A dequeued message are deleted only after `onMessage` 
completes without throwing an exception, 
following the convention documented by [Azure](https://docs.microsoft.com/en-us/azure/storage/queues/storage-java-how-to-use-queue-storage?tabs=java#how-to-dequeue-the-next-message). 
After some period of time Azure will make the same message available in the same queue.

### Circuit breaking queue polling

After 10 consecutive failures of a queue will the library disable polling 
and publish an `AzureQueueConsumerEvent`.

Queue polling statuses are also reflected on micronaut's `/health` endpoint, 
which liveliness probes can take advantage of.

### Explicitly enable or disable queue polling in configuration

By default are queue polling disabled in tests. However, 
in certain situations will you need to explicitly control where queue polling is enabled or disabled.

This example enables queue polling in some cloud environment, but disables it for all others.

```yaml
# application.yml
azure:
  queue:
    queue1:
      enabled: false
      queueName: myqueue1
      handler: QueueHandler
---
# application-cloud.yml
azure:
  queue:
    queue1:
      enabled: true
```

## Known problems

* Any queue declared in the configurations
  currently need a valid connection-sstring, even though it is disabled.
* We have experienced some out of `OutOfMemoryException`, which seems to be 
  caused by Azure's `QueueAsyncClient` when it is waiting for messages to be polled.

## Testing

### Testing a handler

The simplest strategy is simply to just initialize the handler in your test suite
and manually call `onMessage` with a serialized string.

```kotlin
@MicronautTest
internal class MyQueue1HandlerTest(
  @Inject private val handler: MyQueue1Handler
) {
  @Test
  fun `Should fail on invalid message`() {
    assertThrows { handler.onMessage("Invalid message") }
  }
  
  @Test
  fun `Should process valid message`() {
    handler.onMessage("Invalid message")

    // Do some assertions against your database or some 
    // other side-effect of your handler.
  }
}
```
