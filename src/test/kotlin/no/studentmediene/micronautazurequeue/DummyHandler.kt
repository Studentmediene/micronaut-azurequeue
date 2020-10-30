package no.studentmediene.micronautazurequeue

class DummyHandler : AzureQueueMessageHandler {
  val receivedMessages = mutableListOf<String>()

  override fun onMessage(message: String) {
    receivedMessages.add(message)

    if (message == "--fail-me--") {
      throw RuntimeException("[Test] I'm failing")
    }
  }

  fun reset() {
    receivedMessages.clear()
  }
}
