package com.example.salesforcedemo

import com.example.salesforcedemo.models.SalesforceToken
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

@Service
class SalesforceAuthService(
    @param:Value("\${salesforce.username}") private val username: String,
    @param:Value("\${salesforce.consumer-key}") private val consumerKey: String,
    @param:Value("\${salesforce.login-url}") private val loginUrl: String,
    @param:Value("\${salesforce.private-key-path}") private val privateKeyResource: Resource,
) {
    private val logger = LoggerFactory.getLogger(SalesforceAuthService::class.java)
    private val restTemplate = RestTemplate()

    fun authenticate(): SalesforceToken {
        logger.info("Starting JWT authentication for user: $username")

        // Load private key
        val privateKey = loadPrivateKey()

        // Create JWT
        val jwt = createJWT(privateKey)

        // Exchange JWT for access token
        val token = exchangeJWTForToken(jwt)

        logger.info("Successfully authenticated. Instance URL: ${token.instanceUrl}")
        return token
    }

    private fun loadPrivateKey(): RSAPrivateKey {
        logger.info("Loading private key from: $privateKeyResource")

        val keyContent = privateKeyResource.inputStream.bufferedReader().use { it.readText() }
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")

        val keyBytes = Base64.getDecoder().decode(keyContent)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")

        return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
    }

    private fun createJWT(privateKey: RSAPrivateKey): String {
        val now = Date()
        val expirationTime = Date(now.time + 300000) // 5 minutes

        val claimsSet = JWTClaimsSet.Builder()
            .issuer(consumerKey)
            .subject(username)
            .audience(loginUrl)
            .expirationTime(expirationTime)
            .build()

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.RS256).build(),
            claimsSet
        )

        signedJWT.sign(RSASSASigner(privateKey))

        return signedJWT.serialize()
    }

    private fun exchangeJWTForToken(jwt: String): SalesforceToken {
        val tokenUrl = "$loginUrl/services/oauth2/token"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val body = LinkedMultiValueMap<String, String>()
        body.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
        body.add("assertion", jwt)

        val request = HttpEntity(body, headers)

        logger.debug("Exchanging JWT for access token at: $tokenUrl")

        try {
            val response = restTemplate.postForEntity(tokenUrl, request, Map::class.java)
            val responseBody = response.body ?: throw RuntimeException("Empty response from Salesforce")

            val accessToken = responseBody["access_token"] as? String
                ?: throw RuntimeException("No access token in response")
            val instanceUrl = responseBody["instance_url"] as? String
                ?: throw RuntimeException("No instance URL in response")

            return SalesforceToken(accessToken, instanceUrl)
        } catch (e: Exception) {
            logger.error("Failed to authenticate with Salesforce", e)
            throw RuntimeException("Authentication failed: ${e.message}", e)
        }
    }
}
