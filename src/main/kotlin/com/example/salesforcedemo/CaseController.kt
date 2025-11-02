package com.example.salesforcedemo

import com.example.salesforcedemo.models.CloseCaseRequest
import com.example.salesforcedemo.models.CloseCaseResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/cases")
class CaseController(
    private val authService: SalesforceAuthService,
    private val restService: SalesforceRestService,
) {
    private val logger = LoggerFactory.getLogger(CaseController::class.java)

    @PostMapping("/{caseId}/close")
    fun closeCase(
        @PathVariable caseId: String,
        @RequestBody request: CloseCaseRequest,
    ): ResponseEntity<CloseCaseResponse> {
        logger.info("Received request to close case: $caseId with reason: ${request.reason}")

        return try {
            // Authenticate to get a fresh token
            val token = authService.authenticate()

            // Close the case via Salesforce REST API
            restService.closeCase(caseId, request.reason, token)

            // Return success response
            ResponseEntity.ok(
                CloseCaseResponse(
                    success = true,
                    message = "Case closed successfully",
                    caseId = caseId,
                ),
            )
        } catch (e: Exception) {
            logger.error("Failed to close case: $caseId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                CloseCaseResponse(
                    success = false,
                    message = "Failed to close case: ${e.message}",
                    caseId = caseId,
                ),
            )
        }
    }
}
