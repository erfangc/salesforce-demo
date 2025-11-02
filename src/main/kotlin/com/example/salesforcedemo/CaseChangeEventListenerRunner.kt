package com.example.salesforcedemo

import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class CaseChangeEventListenerRunner(
    private val authService: SalesforceAuthService,
    private val cometDService: SalesforceCometDService
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(CaseChangeEventListenerRunner::class.java)

    override fun run(vararg args: String?) {
        logger.info("Starting Salesforce CDC Listener...")

        try {
            // Authenticate
            val token = authService.authenticate()
            logger.info("Authentication successful!")

            // Connect to streaming API
            cometDService.connect(token)
            logger.info("Connected to Salesforce Streaming API")

            // Keep the application running
            logger.info("Application is now listening for Case changes. Press Ctrl+C to exit.")

            // Add a shutdown hook for graceful disconnect
            Runtime.getRuntime().addShutdownHook(Thread {
                logger.info("Shutdown signal received...")
                cometDService.disconnect()
            })

        } catch (e: Exception) {
            logger.error("Failed to start CDC listener", e)
            throw e
        }
    }

    @PreDestroy
    fun cleanup() {
        logger.info("Cleaning up...")
        if (cometDService.isConnected()) {
            cometDService.disconnect()
        }
    }
}
