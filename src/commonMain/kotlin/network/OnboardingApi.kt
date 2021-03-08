package network

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import kotlinx.serialization.json.Json

class OnboardingApi {

    companion object {
        private const val BASE_URL = "https://dev-onboardings.vidaas.com.br/api/v1/enrolls/"
    }

    private val httpClient = HttpClient {
        install(JsonFeature) {
            val json = Json { ignoreUnknownKeys = true }
            serializer = KotlinxSerializer(json)
            Logging {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }
        }
    }

    suspend fun sendEnroll(definition: String, onResult: (SendEnrollResponse) -> Unit) {
        val result = httpClient.post<SendEnrollResponse> {
            url(BASE_URL)
            headers {
                append("Content-Type", "application/json")
            }
            body = SendEnrollRequest(definition)
        }

        onResult(result)
    }

    suspend fun sendEnrollAndWait(definition: String): SendEnrollResponse {
        return httpClient.post<SendEnrollResponse> {
            url(BASE_URL)
            headers {
                append("Content-Type", "application/json")
            }
            body = SendEnrollRequest(definition)
        }
    }

}