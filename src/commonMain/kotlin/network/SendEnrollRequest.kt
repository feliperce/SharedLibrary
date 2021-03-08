package network

import kotlinx.serialization.Serializable

@Serializable
data class SendEnrollRequest(
	val definition: String? = null
)