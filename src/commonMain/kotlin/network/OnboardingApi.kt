package network

import co.touchlab.stately.ensureNeverFrozen
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

class OnboardingApi {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default+job)

    companion object {
        private const val BASE_URL = "https://dev-onboardings.vidaas.com.br/api/v1/enrolls/"
    }

    init {
        //ensureNeverFrozen()
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

    @Throws(Exception::class)
    fun sendEnroll(definition: String, onResult: (SendEnrollResponse) -> Unit)  {
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                httpClient.post<SendEnrollResponse> {
                    url(BASE_URL)
                    headers {
                        append("Content-Type", "application/json")
                    }

                    body = SendEnrollRequest(definition)
                }
            }

            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
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