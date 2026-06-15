package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonObjectUtils;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compares the time claims of two credentials of the same dataset, obtained a few seconds apart (e.g.
 * one per client in the multiple-clients flow), and fails if any of them advanced by ~the real
 * inter-issuance gap (measured via the issuers' HTTP {@code Date} response headers). That is the
 * signature of an issuer that embeds the precise issuance time, which RFC 9901 §10.1 forbids: time
 * information MUST be randomized or rounded so that multiple credentials of the same type cannot be
 * correlated by their timestamps. §10.1 notes the issue "applies to all salted hash-based
 * approaches, including mDL/mDoc", so both formats and all of their time claims are checked:
 *
 * <ul>
 *   <li>SD-JWT VC: the {@code iat}, {@code exp} and {@code nbf} claims.</li>
 *   <li>mdoc: the MSO {@code validityInfo} {@code signed}, {@code validFrom} and {@code validUntil}.</li>
 * </ul>
 *
 * The check fails if <em>any</em> of those advances by ~the real inter-issuance gap.
 *
 * <p>A compliant issuer passes: rounding yields an identical timestamp across the two requests
 * (delta 0), and randomizing yields a timestamp difference uncorrelated with the real gap.
 *
 * <p>Reads the whole credentials from the {@code linkability_captures} list and compares the first
 * and last. Skips when fewer than two were captured, the list is absent (e.g. an unsupported
 * format), no comparable time claims exist, or the two carry different datasets (RFC 9901 §10.1
 * scopes the requirement to "a batch of credentials based on the same claims").
 */
public class VCIEnsureCredentialTimeClaimsNotLinkable extends AbstractVCICredentialTimeClaimsCheck {

	private static final long TOLERANCE_SECONDS = 2;
	private static final long FALLBACK_TOLERANCE_SECONDS = 5;

	/** Volatile / per-credential SD-JWT fields excluded from the same-dataset comparison. */
	private static final List<String> SD_JWT_VOLATILE_CLAIMS = List.of(
		"iat", "exp", "nbf", "cnf", "jti", "status", "_sd", "_sd_alg");

	@Override
	public Environment evaluate(Environment env) {
		JsonArray all = capturesList(env);
		if (all == null || all.size() < 2) {
			log("Fewer than two credentials were captured; cannot assess time-claim linkability",
				args("captured_count", all == null ? 0 : all.size()));
			return env;
		}

		JsonObject first = all.get(0).getAsJsonObject();
		JsonObject second = all.get(all.size() - 1).getAsJsonObject();
		String format = OIDFJSON.getStringOrNull(first.get("format"));

		// --- Same-dataset guard -------------------------------------------------------------------
		// RFC 9901 §10.1 frames the requirement around "a batch of credentials based on the same
		// claims". Only assess linkability when the two credentials actually carry the same dataset.
		JsonObject claims1 = datasetClaims(first, format);
		JsonObject claims2 = datasetClaims(second, format);
		if (claims1 != null && claims2 != null && !claims1.equals(claims2)) {
			log("The two credentials carry different claims; not a same-dataset issuance, so "
					+ "time-claim linkability cannot be assessed",
				args("first_claims", claims1, "second_claims", claims2));
			return env;
		}

		// --- Time-claim comparison ----------------------------------------------------------------
		// RFC 9901 §10.1 lists iat, exp and nbf; the mdoc analogues are the MSO validityInfo
		// signed / validFrom / validUntil. Each MUST be randomized or rounded, so fail if ANY of them
		// advanced by ~the real inter-issuance gap (the signature of a precise value).
		Map<String, Long> ts1 = timestamps(first, format);
		Map<String, Long> ts2 = timestamps(second, format);

		long respTime1 = OIDFJSON.getLong(first.get("response_time"));
		long respTime2 = OIDFJSON.getLong(second.get("response_time"));
		boolean fallback = getBoolean(first, "response_time_is_fallback")
			|| getBoolean(second, "response_time_is_fallback");
		long tolerance = fallback ? FALLBACK_TOLERANCE_SECONDS : TOLERANCE_SECONDS;
		long deltaResp = respTime2 - respTime1;

		Map<String, Object> details = new LinkedHashMap<>();
		details.put("format", format);
		details.put("delta_response_time_seconds", deltaResp);
		details.put("tolerance_seconds", tolerance);

		List<String> preciseClaims = new ArrayList<>();
		boolean anyComparable = false;
		for (String name : ts1.keySet()) {
			Long a = ts1.get(name);
			Long b = ts2.get(name);
			if (a == null || b == null) {
				continue;
			}
			anyComparable = true;
			long delta = b - a;
			details.put(name + "_first", a);
			details.put(name + "_second", b);
			details.put("delta_" + name + "_seconds", delta);
			if (delta > 0 && Math.abs(delta - deltaResp) <= tolerance) {
				preciseClaims.add(name);
			}
		}

		if (!anyComparable) {
			log("No comparable time information in the two credentials; cannot assess time-claim linkability",
				args("format", format));
			return env;
		}

		if (!preciseClaims.isEmpty()) {
			details.put("precise_time_claims", preciseClaims);
			throw error("Credential time information " + preciseClaims + " advanced by ~the real inter-issuance "
					+ "gap between two same-dataset credentials, indicating the issuer embeds the precise issuance "
					+ "time. Per RFC 9901 §10.1, iat/exp/nbf (and the mdoc MSO validityInfo signed/validFrom/"
					+ "validUntil) MUST be randomized or rounded to prevent linkability.",
				details);
		}

		logSuccess("Credential time information does not appear to track the precise issuance time "
				+ "(it is randomized or rounded)",
			details);
		return env;
	}

	/** Dataset claim signature for the same-dataset guard. Returns null to skip the guard. */
	private JsonObject datasetClaims(JsonObject capture, String format) {
		if ("mso_mdoc".equals(format)) {
			return mdocElementValues(capture);
		}
		JsonElement decoded = JsonObjectUtils.path(capture, "sdjwt", "decoded");
		if (decoded == null || !decoded.isJsonObject()) {
			return null;
		}
		JsonObject copy = decoded.getAsJsonObject().deepCopy();
		for (String key : SD_JWT_VOLATILE_CLAIMS) {
			copy.remove(key);
		}
		return copy;
	}

	/**
	 * Decodes the IssuerSigned namespaces to {@code {namespace.elementId: <cbor diagnostic of value>}},
	 * ignoring the per-credential random salts and digest IDs, so two credentials of the same dataset
	 * compare equal.
	 */
	private JsonObject mdocElementValues(JsonObject capture) {
		byte[] bytes = mdocBytes(capture);
		if (bytes == null) {
			return null;
		}
		try {
			DataItem issuerSigned = Cbor.INSTANCE.decode(bytes);
			DataItem nameSpaces = issuerSigned.getOrNull("nameSpaces");
			if (nameSpaces == null) {
				return null;
			}
			JsonObject result = new JsonObject();
			for (Map.Entry<DataItem, DataItem> ns : nameSpaces.getAsMap().entrySet()) {
				String namespace = ns.getKey().getAsTstr();
				for (DataItem itemBytes : ns.getValue().getAsArray()) {
					DataItem item = itemBytes.getAsTaggedEncodedCbor();
					String elementId = item.get("elementIdentifier").getAsTstr();
					String value = Cbor.INSTANCE.toDiagnostics(item.get("elementValue"),
						Set.<org.multipaz.cbor.DiagnosticOption>of());
					result.addProperty(namespace + "." + elementId, value);
				}
			}
			return result;
		} catch (RuntimeException e) {
			return null;
		}
	}
}
