package com.example.salesforcedemo

import com.example.salesforcedemo.models.SalesforceToken
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class SalesforceRestService(
    @param:Value("\${salesforce.api-version}") private val apiVersion: String,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(SalesforceRestService::class.java)
    private val httpClient = HttpClient.newHttpClient()

    fun closeCase(caseId: String, reason: String?, token: SalesforceToken) {
        logger.info("Closing case: $caseId with reason: $reason")

        val url = "${token.instanceUrl}/services/data/v$apiVersion/sobjects/Case/$caseId"

        // Create a request body
        val body = mutableMapOf<String, Any>()
        body["Status"] = "Closed"
        if (reason != null) {
            body["Description"] = "Closed: $reason"
        }

        // Convert body to JSON
        val jsonBody = objectMapper.writeValueAsString(body)

        try {
            // Build PATCH request using Java's built-in HttpClient
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer ${token.accessToken}")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build()

            // Send request
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                logger.info("Successfully closed case: $caseId")
            } else {
                throw RuntimeException("Failed to close case. Status: ${response.statusCode()}, Body: ${response.body()}")
            }
        } catch (e: Exception) {
            logger.error("Failed to close case: $caseId", e)
            throw RuntimeException("Failed to close case: ${e.message}", e)
        }
    }
}
