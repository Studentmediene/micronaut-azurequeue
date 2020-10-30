package no.studentmediene.micronautazurequeue

/**
 * Interface which all message handlers has to implement,
 * to be able to receive messages from an Azure Queue.
 * */
interface AzureQueueMessageHandler {
  /**
   * Called when a new message has been de-queued from the queue.
   *
   * Any de-queued message is only deleted if onMessage successfully finishes.
   *
   * @throws Any Any exception can be thrown to prevent a de-queued message from being deleted
   * @param message The parse payload of the message.
   *                If the message was base64 encoded, will it have been decoded beforehand.
   * */
  fun onMessage(message: String)
}
