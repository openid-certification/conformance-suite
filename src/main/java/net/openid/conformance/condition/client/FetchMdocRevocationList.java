package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import kotlinx.io.bytestring.ByteString;
import org.multipaz.crypto.X509Cert;
import org.multipaz.revocation.RevocationStatus;

import java.util.Base64;

/**
 * Fetches the MSO revocation list referenced by the status element in a received mdoc's Mobile
 * Security Object (ISO/IEC 18013-5 12.3.6.2), whichever of the two mechanisms it uses, and
 * stores the raw response for the downstream validation conditions.
 *
 * <p>Stores {@code mdoc_revocation_list_token} (the base64 encoded token bytes),
 * {@code mdoc_revocation_list_uri}, {@code mdoc_revocation_list_mechanism} and
 * {@code mdoc_revocation_list_endpoint_response}, plus the MSO's own position in the list:
 * {@code mdoc_status_list_idx} for the status list mechanism, {@code mdoc_identifier_list_id}
 * (base64 encoded) for the identifier list mechanism. Any state left over from a previously
 * validated credential is cleared first, so the downstream conditions skip cleanly when this
 * credential carries no status reference (the Status element is optional, "An MSO may contain
 * the Status structure").
 */
public class FetchMdocRevocationList extends AbstractRevocationListCwtCondition {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		env.removeObject(ENV_RESPONSE);
		env.removeNativeValue(ENV_TOKEN);
		env.removeNativeValue(ENV_URI);
		env.removeNativeValue(ENV_MECHANISM);
		env.removeNativeValue(ENV_REFERENCE_CERTIFICATE);
		env.removeNativeValue(ENV_STATUS_LIST_IDX);
		env.removeNativeValue(ENV_IDENTIFIER_LIST_ID);
		env.removeNativeValue(ENV_STATUS);

		RevocationStatus revocationStatus = parseCredentialMso(env).getRevocationStatus();
		if (revocationStatus == null) {
			log("The MSO does not contain a status element, so there is no MSO revocation list to check");
			return env;
		}

		Mechanism mechanism;
		String uri;
		X509Cert certificate;
		if (revocationStatus instanceof RevocationStatus.StatusList statusList) {
			mechanism = Mechanism.STATUS_LIST;
			uri = statusList.getUri();
			certificate = statusList.getCertificate();
		} else if (revocationStatus instanceof RevocationStatus.IdentifierList identifierList) {
			mechanism = Mechanism.IDENTIFIER_LIST;
			uri = identifierList.getUri();
			certificate = identifierList.getCertificate();
		} else {
			throw error("The MSO's status element uses neither the status list nor the identifier"
				+ " list mechanism", args("status", revocationStatus.toString()));
		}

		if (certificate != null) {
			// the optional Certificate element is the explicit trust point for the revocation
			// list's x5chain (12.3.6.2); recorded for ValidateMdocRevocationListCertificateChain
			env.putString(ENV_REFERENCE_CERTIFICATE, base64(certificate.getEncoded()));
		}

		byte[] body = fetchRevocationList(env, uri, mechanism);

		env.putString(ENV_TOKEN, Base64.getEncoder().encodeToString(body));
		env.putString(ENV_URI, uri);
		env.putString(ENV_MECHANISM, mechanism.msoElement);

		String position;
		if (revocationStatus instanceof RevocationStatus.StatusList statusList) {
			env.putInteger(ENV_STATUS_LIST_IDX, statusList.getIdx());
			position = String.valueOf(statusList.getIdx());
		} else {
			String id = base64(((RevocationStatus.IdentifierList) revocationStatus).getId());
			env.putString(ENV_IDENTIFIER_LIST_ID, id);
			position = id;
		}

		logSuccess("Fetched the MSO revocation list referenced by the mdoc's " + mechanism.msoElement + " element",
			args("uri", uri,
				mechanism == Mechanism.STATUS_LIST ? "idx" : "id", position,
				"length", body.length));
		return env;
	}

	private static String base64(ByteString bytes) {
		return Base64.getEncoder().encodeToString(bytes.toByteArray(0, bytes.getSize()));
	}
}
