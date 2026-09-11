package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import kotlinx.io.bytestring.ByteString;
import org.multipaz.crypto.X509Cert;
import org.multipaz.revocation.RevocationStatus;

import java.util.Base64;

/**
 * Fetches the MSO revocation list referenced by the status_list element in a received mdoc's
 * Mobile Security Object (ISO/IEC 18013-5 12.3.6.2) and stores the raw response for the
 * downstream validation conditions.
 *
 * <p>Stores {@code mdoc_revocation_list_token} (the base64 encoded token bytes),
 * {@code mdoc_revocation_list_uri}, {@code mdoc_revocation_list_endpoint_response} and the
 * MSO's index into the list, {@code mdoc_status_list_idx}, plus the status reference's optional
 * Certificate element as {@code mdoc_revocation_list_reference_certificate}. Any state left over from a
 * previously validated credential is cleared first, so the downstream conditions skip cleanly
 * when this credential carries no status reference (the Status element is optional, "An MSO
 * may contain the Status structure").
 */
public class FetchMdocRevocationList extends AbstractRevocationListCwtCondition {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		env.removeObject(ENV_RESPONSE);
		env.removeNativeValue(ENV_TOKEN);
		env.removeNativeValue(ENV_URI);
		env.removeNativeValue(ENV_REFERENCE_CERTIFICATE);
		env.removeNativeValue(ENV_STATUS_LIST_IDX);
		env.removeNativeValue(ENV_STATUS);

		RevocationStatus revocationStatus = parseCredentialMso(env).getRevocationStatus();
		if (revocationStatus == null) {
			log("The MSO does not contain a status element, so there is no MSO revocation list to check");
			return env;
		}
		if (!(revocationStatus instanceof RevocationStatus.StatusList statusList)) {
			// the mechanism itself is validated by ValidateMdocMsoRevocationMechanism
			log("The MSO's status element does not use the status list mechanism, skipping the status list check");
			return env;
		}

		String uri = statusList.getUri();
		int idx = statusList.getIdx();
		X509Cert certificate = statusList.getCertificate();
		if (certificate != null) {
			// the optional Certificate element is the explicit trust point for the revocation
			// list's x5chain (12.3.6.2); recorded for ValidateMdocRevocationListCertificateChain
			env.putString(ENV_REFERENCE_CERTIFICATE, base64(certificate.getEncoded()));
		}

		byte[] body = fetchRevocationList(env, uri);

		env.putString(ENV_TOKEN, Base64.getEncoder().encodeToString(body));
		env.putString(ENV_URI, uri);
		env.putInteger(ENV_STATUS_LIST_IDX, idx);

		logSuccess("Fetched the MSO revocation list referenced by the mdoc's status_list element",
			args("uri", uri, "idx", idx, "length", body.length));
		return env;
	}

	private static String base64(ByteString bytes) {
		return Base64.getEncoder().encodeToString(bytes.toByteArray(0, bytes.getSize()));
	}
}
