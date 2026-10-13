package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.federation.IgnoreErrorsErrorHandler;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocCertificateProfileChecks;
import net.openid.conformance.util.MdocIacaCrl;
import net.openid.conformance.util.MdocUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Fetches the CRL named by the CRL distribution point of the document signer certificate in a
 * received mdoc's x5chain, trying each URI the extension lists in order (RFC 5280 4.2.1.13:
 * each name is a way to obtain the same CRL) and stopping at the first that answers with a
 * body. Stores {@code mdoc_ds_certificate_crl}, {@code mdoc_ds_certificate_crl_uri} and
 * {@code mdoc_ds_certificate_crl_endpoint_response}, clearing any state left by a previously
 * validated credential first so the downstream checks skip cleanly when nothing was fetched.
 *
 * <p>The CRL and its checks depend only on the document signer certificate, so each certificate
 * is handled once per test ({@code mdoc_ds_certificate_crl_checked}): for a later credential
 * signed by a certificate already seen, this condition logs that and stores no CRL, and the
 * downstream checks skip. The certificate is recorded before fetching, so a CRL that cannot be
 * retrieved is reported once rather than once per credential.
 *
 * <p>A certificate naming no distribution point URI is a failure here as well as in
 * {@link ValidateMdocDsCertificateProfile}: Table B.3 makes the extension mandatory, and a reader
 * is as unable to establish revocation status as when the named CRL cannot be retrieved.
 */
public class FetchMdocDsCertificateCrl extends AbstractMdocDsCertificateCrlCondition {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		env.removeObject(ENV_RESPONSE);
		env.removeNativeValue(ENV_CRL);
		env.removeNativeValue(ENV_URI);
		env.removeNativeValue(ENV_AUTHENTICATED);

		X509Certificate dsCert = extractDsCertificate(decodeIssuerSigned(env));
		String subject = dsCert.getSubjectX500Principal().getName();
		String fingerprint = fingerprint(dsCert);
		JsonObject checked = env.containsObject(ENV_CHECKED) ? env.getObject(ENV_CHECKED) : new JsonObject();
		if (checked.has(fingerprint)) {
			log("The CRL named by this document signer certificate was already fetched and checked for an earlier credential"
					+ " in this test, so the checks are not repeated",
				args("subject", subject, "fingerprint_sha256", fingerprint, "earlier_result", checked.get(fingerprint)));
			return env;
		}
		JsonObject record = new JsonObject();
		record.addProperty("subject", subject);
		checked.add(fingerprint, record);
		env.putObject(ENV_CHECKED, checked);

		List<String> uris;
		try {
			uris = MdocCertificateProfileChecks.crlDistributionPointUris(dsCert);
		} catch (IllegalArgumentException e) {
			throw error("The document signer certificate's CRL distribution points extension could not be parsed", e);
		}
		if (uris.isEmpty()) {
			throw error("The document signer certificate names no CRL distribution point URI, so an mdoc reader cannot"
					+ " establish the certificate's revocation status; ISO 18013-5 Table B.3 requires the extension",
				args("subject", subject));
		}

		List<String> failures = new ArrayList<>();
		for (String uri : uris) {
			byte[] body = fetch(env, uri, failures);
			if (body != null) {
				env.putString(ENV_CRL, Base64.getEncoder().encodeToString(body));
				env.putString(ENV_URI, uri);
				record.addProperty("uri", uri);
				logSuccess("Fetched the CRL named by the document signer certificate's CRL distribution point",
					args("uri", uri, "length", body.length, "earlier_uris_that_failed", failures));
				return env;
			}
		}

		throw error("Unable to retrieve the CRL named by the document signer certificate's CRL distribution point from any of its URIs;"
				+ " an mdoc reader cannot establish the certificate's revocation status",
			args("distribution_point_uris", uris, "failures", failures));
	}

	/**
	 * The CRL body served at {@code uri}, or null after adding why there was none to
	 * {@code failures}. The response is recorded in the environment whenever one arrived.
	 */
	private byte[] fetch(Environment env, String uri, List<String> failures) {
		ResponseEntity<byte[]> response;
		try {
			response = getCrl(env, uri);
		} catch (Exception e) {
			failures.add(uri + ": " + e.getMessage());
			return null;
		}
		env.putObject(ENV_RESPONSE, convertBinaryResponseForEnvironment("document signer certificate CRL distribution point", response));
		if (!response.getStatusCode().is2xxSuccessful()) {
			failures.add(uri + ": HTTP " + response.getStatusCode().value());
			return null;
		}
		byte[] body = response.getBody();
		if (body == null || body.length == 0) {
			failures.add(uri + ": empty body");
			return null;
		}
		return body;
	}

	/**
	 * The HTTP GET itself; overridden by the unit tests to serve canned responses. Every status
	 * is returned rather than thrown, so a 404 is recorded like any other response.
	 */
	protected ResponseEntity<byte[]> getCrl(Environment env, String uri) throws Exception {
		RestTemplate restTemplate = createRestTemplate(env);
		restTemplate.setErrorHandler(new IgnoreErrorsErrorHandler());
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, MdocIacaCrl.CONTENT_TYPE);
		return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
	}

	private String fingerprint(X509Certificate cert) {
		try {
			return HexFormat.of().formatHex(MdocUtil.sha256(cert.getEncoded()));
		} catch (CertificateEncodingException e) {
			throw error("The document signer certificate could not be re-encoded", e);
		}
	}

	/** A CRL is public: the test client's mTLS certificate is not presented to a distribution point. */
	@Override
	protected boolean useMtlsForHttpRequests() {
		return false;
	}
}
