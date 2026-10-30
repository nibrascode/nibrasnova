package com.nibras.nova.core.security

import android.net.http.SslError
import javax.inject.Inject
import javax.inject.Singleton

data class SslCertificateChallenge(
    val failingUrl: String,
    val primaryErrorCode: Int,
    val errorTitle: String,
    val technicalDescription: String,
    val issuedTo: String,
    val issuedBy: String,
    val onCancelNavigation: () -> Unit,
    val onProceedWithExplicitRisk: () -> Unit
)

@Singleton
class SslErrorHandlerPolicy @Inject constructor() {

    fun describeSslError(primaryError: Int): Pair<String, String> {
        return when (primaryError) {
            SslError.SSL_EXPIRED -> "Certificate Expired" to
                "The security certificate for this website has expired and can no longer verify its identity."
            SslError.SSL_IDMISMATCH -> "Hostname Mismatch" to
                "The certificate presented by the server does not match the domain name you are visiting."
            SslError.SSL_NOTYETVALID -> "Certificate Not Yet Valid" to
                "The security certificate's activation timestamp is in the future. Check your device clock or server configuration."
            SslError.SSL_UNTRUSTED -> "Untrusted Certificate Authority" to
                "The certificate authority that signed this website's certificate is not trusted by Android's system trust store."
            SslError.SSL_DATE_INVALID -> "Invalid Certificate Date" to
                "The certificate validity dates are malformed or out of bounds."
            else -> "Invalid SSL/TLS Connection" to
                "A cryptographic TLS error occurred while verifying this server's certificate."
        }
    }
}
