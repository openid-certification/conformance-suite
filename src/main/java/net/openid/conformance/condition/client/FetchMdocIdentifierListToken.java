package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.mdoc.mso.MobileSecurityObject;
import org.multipaz.revocation.RevocationStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Fetches the MSO revocation list referenced by the identifier_list element in a received mdoc's
 * Mobile Security Object (ISO/IEC 18013-5 12.3.6.2) and stores the raw response for the
 * downstream validation conditions.
 *
 * <p>Stores {@code mdoc_identifier_list_token} (the base64 encoded token bytes),
 * {@code mdoc_identifier_list_id} (the MSO's own Identifier, base64 encoded),
 * {@code mdoc_identifier_list_uri} and {@code mdoc_identifier_list_token_endpoint_response}. Any
 * state left over from a previously validated credential is cleared first, so the downstream
 * conditions skip cleanly when this credential carries no identifier_list reference (the Status
 * element is optional, and when present it may use the status list mechanism instead).
 */
public class FetchMdocIdentifierListToken extends AbstractIdentifierListCwtCondition {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		clearIdentifierListState(env);

		String mdocCborBase64 = env.getString("mdoc_credential_cbor");

		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(mdocCborBase64);
		} catch (IllegalArgumentException e) {
			throw error("Failed to decode mdoc_credential_cbor from base64", e);
		}

		MobileSecurityObject mso;
		try {
			mso = MdocUtil.parseMso(bytes);
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		}

		RevocationStatus revocationStatus = mso.getRevocationStatus();
		if (revocationStatus == null) {
			log("The MSO does not contain a status element, so there is no MSO revocation list to check");
			return env;
		}
		if (!(revocationStatus instanceof RevocationStatus.IdentifierList identifierList)) {
			// the status list mechanism is handled by FetchMdocStatusListToken instead; the
			// mechanism itself is validated by ValidateMdocMsoRevocationMechanism
			log("The MSO's status element does not use the identifier list mechanism,"
				+ " skipping the identifier list check");
			return env;
		}

		String uri = identifierList.getUri();
		kotlinx.io.bytestring.ByteString id = identifierList.getId();
		byte[] identifier = id.toByteArray(0, id.getSize());

		ResponseEntity<byte[]> response;
		try {
			response = fetchIdentifierListToken(env, uri);
		} catch (Exception e) {
			throw error("Unable to retrieve the MSO revocation list referenced by the mdoc's"
				+ " identifier_list element", e, args("uri", uri));
		}

		env.putObject(ENV_IDENTIFIER_LIST_RESPONSE,
			convertBinaryResponseForEnvironment("mdoc identifier list token endpoint", response));

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw error("Failed to retrieve the MSO revocation list referenced by the mdoc's"
				+ " identifier_list element",
				args("uri", uri, "status", response.getStatusCode().value()));
		}

		byte[] body = response.getBody();
		if (body == null || body.length == 0) {
			throw error("The MSO revocation list endpoint returned an empty body", args("uri", uri));
		}

		env.putString(ENV_IDENTIFIER_LIST_TOKEN, Base64.getEncoder().encodeToString(body));
		env.putString(ENV_IDENTIFIER_LIST_URI, uri);
		env.putString(ENV_IDENTIFIER_LIST_ID, Base64.getEncoder().encodeToString(identifier));

		logSuccess("Fetched the MSO revocation list referenced by the mdoc's identifier_list element",
			args("uri", uri,
				"id", Base64.getUrlEncoder().withoutPadding().encodeToString(identifier),
				"length", body.length));
		return env;
	}

	private void clearIdentifierListState(Environment env) {
		env.removeObject(ENV_IDENTIFIER_LIST_RESPONSE);
		env.removeNativeValue(ENV_IDENTIFIER_LIST_TOKEN);
		env.removeNativeValue(ENV_IDENTIFIER_LIST_URI);
		env.removeNativeValue(ENV_IDENTIFIER_LIST_ID);
	}

	protected ResponseEntity<byte[]> fetchIdentifierListToken(Environment env, String uri) throws Exception {
		RestTemplate restTemplate = createRestTemplate(env);
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, IDENTIFIER_LIST_CWT_CONTENT_TYPE);
		return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
	}
}
