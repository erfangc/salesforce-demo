package com.example.salesforcedemo

import com.example.salesforcedemo.models.SalesforceToken
import org.cometd.bayeux.Channel
import org.cometd.bayeux.Message
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.client.BayeuxClient
import org.cometd.client.transport.LongPollingTransport
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Service
class SalesforceCometDService(
    @param:Value("\${salesforce.api-version}") private val apiVersion: String,
) {
    private val logger = LoggerFactory.getLogger(SalesforceCometDService::class.java)
    private var client: BayeuxClient? = null
    private val cdcChannel = "/data/CaseChangeEvent"

    fun connect(token: SalesforceToken) {
        logger.info("Connecting to Salesforce Streaming API...")

        val streamingEndpoint = "${token.instanceUrl}/cometd/$apiVersion"
        logger.info("Streaming endpoint: $streamingEndpoint")

        // Create an HTTP client with SSL support for HTTPS
        val sslContextFactory = SslContextFactory.Client()
        val httpClient = HttpClient(sslContextFactory)
        httpClient.start()

        // Create custom transport that adds OAuth header to each request
        val transport = object : LongPollingTransport(null, httpClient) {
            override fun customize(request: Request) {
                super.customize(request)
                request.header("Authorization", "Bearer ${token.accessToken}")
            }
        }

        // Create BayeuxClient
        client = BayeuxClient(streamingEndpoint, transport)

        // Add connection listener
        client?.getChannel(Channel.META_CONNECT)?.addListener(object : ClientSessionChannel.MessageListener {
            override fun onMessage(channel: ClientSessionChannel, message: Message) {
                if (!message.isSuccessful) {
                    logger.error("Connection failed: ${message.json}")
                } else {
                    logger.debug("Connected successfully")
                }
            }
        })

        // Add handshake listener
        val handshakeLatch = CountDownLatch(1)
        client?.getChannel(Channel.META_HANDSHAKE)?.addListener(object : ClientSessionChannel.MessageListener {
            override fun onMessage(channel: ClientSessionChannel, message: Message) {
                if (message.isSuccessful) {
                    logger.info("Handshake successful")
                    handshakeLatch.countDown()
                } else {
                    logger.error("Handshake failed: ${message.json}")
                }
            }
        })

        // Perform handshake
        client?.handshake()

        // Wait for the handshake to complete
        val handshakeSuccess = handshakeLatch.await(30, TimeUnit.SECONDS)
        if (!handshakeSuccess) {
            throw RuntimeException("Handshake timeout")
        }

        // Subscribe to Case CDC channel
        subscribeToCaseChanges()
    }

    private fun subscribeToCaseChanges() {
        logger.info("Subscribing to channel: $cdcChannel")

        val subscriptionLatch = CountDownLatch(1)

        client?.getChannel(Channel.META_SUBSCRIBE)?.addListener(object : ClientSessionChannel.MessageListener {
            override fun onMessage(channel: ClientSessionChannel, message: Message) {
                if (message.isSuccessful) {
                    logger.info("Successfully subscribed to $cdcChannel")
                    subscriptionLatch.countDown()
                } else {
                    logger.error("Subscription failed: ${message.json}")
                }
            }
        })

        // Subscribe and add message listener
        client?.getChannel(cdcChannel)?.subscribe { channel, message ->
            logger.info("Received message on channel channel.channelId=${channel.channelId} channel.session.id=${channel.session.id}")
            handleCaseChange(message)
        }

        // Wait for subscription to complete
        val subscriptionSuccess = subscriptionLatch.await(10, TimeUnit.SECONDS)
        if (!subscriptionSuccess) {
            logger.warn("Subscription acknowledgment timeout (may still be subscribed)")
        }

        logger.info("Listening for Case changes...")
    }

    private fun handleCaseChange(message: Message) {
        logger.info("=".repeat(80))
        logger.info("Received Case Change Event")
        logger.info("=".repeat(80))

        val data = message.dataAsMap
        logger.info("Full message: ${message.json}")

        // Parse the change event data
        val payload = data["payload"] as? Map<*, *>
        if (payload != null) {
            logger.info("Event Type: ${payload["ChangeEventHeader"]}")
            logger.info("Case Data: $payload")

            // Extract change event header
            val header = payload["ChangeEventHeader"] as? Map<*, *>
            if (header != null) {
                logger.info("  Change Type: ${header["changeType"]}")
                logger.info("  Entity Name: ${header["entityName"]}")
                logger.info("  Changed Fields: ${header["changedFields"]}")
                logger.info("  Record IDs: ${header["recordIds"]}")
            }
        } else {
            logger.info("Raw data: $data")
        }

        logger.info("=".repeat(80))
    }

    fun disconnect() {
        logger.info("Disconnecting from Salesforce Streaming API...")
        client?.disconnect()
        logger.info("Disconnected")
    }

    fun isConnected(): Boolean {
        return client?.isConnected ?: false
    }
}
