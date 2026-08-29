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
 * Fetches the MSO revocation list referenced by the status_list element in a received mdoc's
 * Mobile Security Object (ISO/IEC 18013-5 12.3.6.2) and stores the raw response for the
 * downstream validation conditions.
 *
 * <p>Stores {@code mdoc_status_list_token} (the base64 encoded token bytes),
 * {@code mdoc_status_list_idx}, {@code mdoc_status_list_uri} and
 * {@code mdoc_status_list_token_endpoint_response}. Any state left over from a previously
 * validated credential is cleared first, so the downstream conditions skip cleanly when this
 * credential carries no status reference (the Status element is optional, "An MSO may contain
 * the Status structure").
 */
public class FetchMdocStatusListToken extends AbstractStatusListCwtCondition {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		clearStatusListState(env);

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
		if (!(revocationStatus instanceof RevocationStatus.StatusList statusList)) {
			// the identifier list mechanism is not (yet) checked here; the mechanism itself is
			// validated by ValidateMdocMsoRevocationMechanism
			log("The MSO's status element does not use the status list mechanism, skipping the status list check");
			return env;
		}

		String uri = statusList.getUri();
		int idx = statusList.getIdx();

		ResponseEntity<byte[]> response;
		try {
			response = fetchStatusListToken(env, uri);
		} catch (Exception e) {
			throw error("Unable to retrieve the MSO revocation list referenced by the mdoc's status_list element",
				e, args("uri", uri));
		}

		env.putObject(ENV_STATUS_LIST_RESPONSE,
			convertBinaryResponseForEnvironment("mdoc status list token endpoint", response));

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw error("Failed to retrieve the MSO revocation list referenced by the mdoc's status_list element",
				args("uri", uri, "status", response.getStatusCode().value()));
		}

		byte[] body = response.getBody();
		if (body == null || body.length == 0) {
			throw error("The MSO revocation list endpoint returned an empty body", args("uri", uri));
		}

		env.putString(ENV_STATUS_LIST_TOKEN, Base64.getEncoder().encodeToString(body));
		env.putString(ENV_STATUS_LIST_URI, uri);
		env.putInteger(ENV_STATUS_LIST_IDX, idx);

		logSuccess("Fetched the MSO revocation list referenced by the mdoc's status_list element",
			args("uri", uri, "idx", idx, "length", body.length));
		return env;
	}

	private void clearStatusListState(Environment env) {
		env.removeObject(ENV_STATUS_LIST_RESPONSE);
		env.removeNativeValue(ENV_STATUS_LIST_TOKEN);
		env.removeNativeValue(ENV_STATUS_LIST_URI);
		env.removeNativeValue(ENV_STATUS_LIST_IDX);
	}

	protected ResponseEntity<byte[]> fetchStatusListToken(Environment env, String uri) throws Exception {
		RestTemplate restTemplate = createRestTemplate(env);
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, STATUS_LIST_CWT_CONTENT_TYPE);
		return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
	}
}
