package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.DcqlQueryUtils;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DiagnosticOption;
import org.multipaz.testapp.TestAppUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreateMdocCredential extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "session_transcript")
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {
		byte[] sessionTranscript = Base64.getDecoder().decode(env.getString("session_transcript"));

		TestAppUtils testAppUtils = TestAppUtils.INSTANCE;
		testAppUtils.initialise();

		String requestedDocType = null;
		Map<String, Set<String>> requestedClaims = null;
		JsonObject dcqlQuery = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY);
		if (dcqlQuery != null) {
			JsonObject matchingCredential = findFirstMdocCredentialEntry(dcqlQuery);
			if (matchingCredential != null) {
				requestedDocType = DcqlQueryUtils.extractMdocDoctypeValue(matchingCredential);
				// Always pass a (possibly empty) map when DCQL is present so the wallet
				// only discloses requested elements. If claims are omitted, nothing is
				// disclosed, matching the SD-JWT data minimization behaviour.
				requestedClaims = extractMdocRequestedClaims(matchingCredential);
			}
		}

		byte[] mdoc = testAppUtils.generateDeviceResponse(sessionTranscript, requestedDocType, requestedClaims);
		env.putString("credential", Base64URL.encode(mdoc).toString());

		String diagnostics = Cbor.INSTANCE.toDiagnostics(mdoc,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));
		log("Created mdoc presentation",
			args("mdoc_b64", Base64URL.encode(mdoc).toString(),
				"cbor_diagnostic", diagnostics,
				"requested_docType", requestedDocType,
				"requested_claims", flattenRequestedClaims(requestedClaims)));

		return env;
	}

	private static JsonObject findFirstMdocCredentialEntry(JsonObject dcqlQuery) {
		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");
		if (credentials == null) {
			return null;
		}
		for (JsonElement credEl : credentials) {
			JsonObject cred = credEl.getAsJsonObject();
			if (cred.has("format") && "mso_mdoc".equals(OIDFJSON.getString(cred.get("format")))) {
				return cred;
			}
		}
		return null;
	}

	// MongoDB rejects map keys containing dots; flatten to [namespace, elementName] pairs for logging.
	private static List<List<String>> flattenRequestedClaims(Map<String, Set<String>> claims) {
		if (claims == null) {
			return null;
		}
		List<List<String>> result = new ArrayList<>();
		for (Map.Entry<String, Set<String>> e : claims.entrySet()) {
			for (String elem : e.getValue()) {
				result.add(List.of(e.getKey(), elem));
			}
		}
		return result;
	}

	// TODO: does not honour DCQL claim_sets semantics; flattens all listed claims.
	private Map<String, Set<String>> extractMdocRequestedClaims(JsonObject credential) {
		Map<String, Set<String>> result = new LinkedHashMap<>();
		JsonArray claims = credential.getAsJsonArray("claims");
		if (claims == null) {
			return result;
		}
		for (JsonElement claimEl : claims) {
			JsonArray path = claimEl.getAsJsonObject().getAsJsonArray("path");
			if (path == null || path.size() != 2) {
				log("Ignoring DCQL claim with non-2-element path (mdoc requires [namespace, elementIdentifier])",
					args("path", path));
				continue;
			}
			String namespace = OIDFJSON.getString(path.get(0));
			String elementName = OIDFJSON.getString(path.get(1));
			result.computeIfAbsent(namespace, k -> new LinkedHashSet<>()).add(elementName);
		}
		return result;
	}

}
