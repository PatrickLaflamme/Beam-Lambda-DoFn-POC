package ai.triton.platform.models

data class AWSConfig(
    val region: String,
    val endpoint: String?,
): java.io.Serializable