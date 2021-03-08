package network

import kotlinx.serialization.Serializable

@Serializable
data class SendEnrollResponse(
	val requesteds: List<SendRequestedsItem>? = null,
	val uuid: String? = null,
	val status: String? = null
)

@Serializable
data class SendRequestedsItem(
	val key: String? = null,
	val required: Boolean? = null
)

