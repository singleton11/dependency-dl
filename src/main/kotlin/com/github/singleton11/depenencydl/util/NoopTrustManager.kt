package com.github.singleton11.depenencydl.util

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class NoopTrustManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // Do nothing
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // Do nothing
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}