package ooo.simone.vibescout.core.api

import ooo.simone.vibescout.core.log.e

object ApiClient {
    var apiManager: VibescoutApiClient? = null
        get() {
            if (field == null) {
                field = createApiManagerClient()
            }
            return field
        }
        private set

    private fun createApiManagerClient(): VibescoutApiClient? {
        var client: VibescoutApiClient? = null
        runCatching {
            client = ServiceGenerator.createService(
                VibescoutApiClient::class.java
            )
        }.onFailure {
            e(it)
        }

        return client
    }
}