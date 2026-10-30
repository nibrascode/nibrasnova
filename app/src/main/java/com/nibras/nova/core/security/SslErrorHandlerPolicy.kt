package com.nibras.nova.core.security

import com.nibras.nova.R
import com.nibras.nova.core.common.L

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
            SslError.SSL_EXPIRED -> L.s(R.string.t_certificate_expired) to
                L.s(R.string.t_the_security_certificate_for_this_websit)
            SslError.SSL_IDMISMATCH -> L.s(R.string.t_hostname_mismatch) to
                L.s(R.string.t_the_certificate_presented_by_the_server)
            SslError.SSL_NOTYETVALID -> L.s(R.string.t_certificate_not_yet_valid) to
                L.s(R.string.t_the_security_certificate_s_activation_ti)
            SslError.SSL_UNTRUSTED -> L.s(R.string.t_untrusted_certificate_authority) to
                L.s(R.string.t_the_certificate_authority_that_signed_th)
            SslError.SSL_DATE_INVALID -> L.s(R.string.t_invalid_certificate_date) to
                L.s(R.string.t_the_certificate_validity_dates_are_malfo)
            else -> L.s(R.string.t_invalid_ssl_tls_connection) to
                L.s(R.string.t_a_cryptographic_tls_error_occurred_while)
        }
    }
}
