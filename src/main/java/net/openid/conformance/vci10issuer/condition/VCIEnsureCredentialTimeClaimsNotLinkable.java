package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.CoseSign1;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compares the issuance time of two credentials of the same dataset, obtained a few seconds apart
 * (e.g. one per client in the multiple-clients flow), and fails if it advanced by ~the real
 * inter-issuance gap (measured via the issuers' HTTP {@code Date} response headers). That is the
 * signature of an issuer that embeds the precise issuance time, which RFC 9901 §10.1 forbids: time
 * information MUST be randomized or rounded so that multiple credentials of the same type cannot be
 * correlated by their timestamps. §10.1 notes the issue "applies to all salted hash-based
 * approaches, including mDL/mDoc", so both formats are checked:
 *
 * <ul>
 *   <li>SD-JWT VC: the {@code iat} claim.</li>
 *   <li>mdoc: the MSO {@code validityInfo.signed} timestamp.</li>
 * </ul>
 *
 * <p>A compliant issuer passes: rounding yields an identical issuance time across the two requests
 * (delta 0), and randomizing yields an issuance-time difference uncorrelated with the real gap.
 *
 * <p>Reads the whole credentials from the {@code linkability_captures} list and compares the first
 * and last. Skips when fewer than two were captured, the list is absent (e.g. an unsupported
 * format), the issuance time cannot be read, or the two carry different datasets (RFC 9901 §10.1
 * scopes the requirement to "a batch of credentials based on the same claims").
 */
public class VCIEnsureCredentialTimeClaimsNotLinkable extends AbstractCondition {

	private static final long TOLERANCE_SECONDS = 2;
	private static final long FALLBACK_TOLERANCE_SECONDS = 5;

	/** Volatile / per-credential SD-JWT fields excluded from the same-dataset comparison. */
	private static final List<String> SD_JWT_VOLATILE_CLAIMS = List.of(
		"iat", "exp", "nbf", "cnf", "jti", "status", "_sd", "_sd_alg");

	@Override
	public Environment evaluate(Environment env) {
		JsonElement listEl = env.getElementFromObject("linkability_captures", "list");
		JsonArray all = (listEl != null && listEl.isJsonArray()) ? listEl.getAsJsonArray() : null;
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

		// --- Issuance-time comparison -------------------------------------------------------------
		Long t1 = issuanceEpochSeconds(first, format);
		Long t2 = issuanceEpochSeconds(second, format);
		if (t1 == null || t2 == null) {
			log("Issuance time not available in one or both credentials; cannot assess time-claim linkability",
				args("format", format, "issuance_first", t1, "issuance_second", t2));
			return env;
		}

		long respTime1 = OIDFJSON.getLong(first.get("response_time"));
		long respTime2 = OIDFJSON.getLong(second.get("response_time"));
		boolean fallback = getBoolean(first, "response_time_is_fallback")
			|| getBoolean(second, "response_time_is_fallback");
		long tolerance = fallback ? FALLBACK_TOLERANCE_SECONDS : TOLERANCE_SECONDS;

		long deltaIssuance = t2 - t1;
		long deltaResp = respTime2 - respTime1;

		if (deltaIssuance > 0 && Math.abs(deltaIssuance - deltaResp) <= tolerance) {
			throw error("The credential's issuance time advanced by ~the real inter-issuance gap between "
					+ "two same-dataset credentials, indicating the issuer embeds the precise issuance time. "
					+ "Per RFC 9901 §10.1, time information (the SD-JWT iat claim, or the mdoc MSO "
					+ "validityInfo signed timestamp) MUST be randomized or rounded to prevent linkability.",
				args("format", format, "issuance_first", t1, "issuance_second", t2,
					"delta_issuance_seconds", deltaIssuance, "delta_response_time_seconds", deltaResp,
					"tolerance_seconds", tolerance));
		}

		logSuccess("Credential issuance time does not appear to track the precise issuance time "
				+ "(it is randomized or rounded)",
			args("format", format, "delta_issuance_seconds", deltaIssuance,
				"delta_response_time_seconds", deltaResp, "tolerance_seconds", tolerance));
		return env;
	}

	/** Issuance-time analog in epoch seconds: SD-JWT {@code iat}, or mdoc MSO {@code signed}. */
	private Long issuanceEpochSeconds(JsonObject entry, String format) {
		if ("mso_mdoc".equals(format)) {
			MobileSecurityObject mso = parseMso(entry);
			if (mso == null) {
				return null;
			}
			try {
				// kotlin.time.Instant -> ISO-8601 string -> epoch seconds
				return Instant.parse(mso.getSignedAt().toString()).getEpochSecond();
			} catch (RuntimeException e) {
				return null;
			}
		}
		JsonElement iat = path(entry, "sdjwt", "credential", "claims", "iat");
		return (iat != null && !iat.isJsonNull()) ? OIDFJSON.getLong(iat) : null;
	}

	/** Dataset claim signature for the same-dataset guard. Returns null to skip the guard. */
	private JsonObject datasetClaims(JsonObject entry, String format) {
		if ("mso_mdoc".equals(format)) {
			return mdocElementValues(entry);
		}
		JsonElement decoded = path(entry, "sdjwt", "decoded");
		if (decoded == null || !decoded.isJsonObject()) {
			return null;
		}
		JsonObject copy = decoded.getAsJsonObject().deepCopy();
		for (String key : SD_JWT_VOLATILE_CLAIMS) {
			copy.remove(key);
		}
		return copy;
	}

	private MobileSecurityObject parseMso(JsonObject entry) {
		byte[] bytes = mdocBytes(entry);
		if (bytes == null) {
			return null;
		}
		try {
			DataItem issuerSigned = Cbor.INSTANCE.decode(bytes);
			DataItem issuerAuth = issuerSigned.getOrNull("issuerAuth");
			if (issuerAuth == null) {
				return null;
			}
			CoseSign1 coseSign1 = issuerAuth.getAsCoseSign1();
			DataItem payloadItem = Cbor.INSTANCE.decode(coseSign1.getPayload());
			DataItem msoDataItem = payloadItem.getAsTaggedEncodedCbor();
			return MobileSecurityObject.Companion.fromDataItem(msoDataItem);
		} catch (RuntimeException e) {
			return null;
		}
	}

	/**
	 * Decodes the IssuerSigned namespaces to {@code {namespace.elementId: <cbor diagnostic of value>}},
	 * ignoring the per-credential random salts and digest IDs, so two credentials of the same dataset
	 * compare equal.
	 */
	private JsonObject mdocElementValues(JsonObject entry) {
		byte[] bytes = mdocBytes(entry);
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

	private byte[] mdocBytes(JsonObject entry) {
		String cbor = OIDFJSON.getStringOrNull(entry.get("mdoc_credential_cbor"));
		if (cbor == null) {
			return null;
		}
		try {
			return Base64.getDecoder().decode(cbor);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private boolean getBoolean(JsonObject o, String key) {
		JsonElement el = o.get(key);
		return el != null && !el.isJsonNull() && OIDFJSON.getBoolean(el);
	}

	private static JsonElement path(JsonObject obj, String... keys) {
		JsonElement cur = obj;
		for (String key : keys) {
			if (cur == null || !cur.isJsonObject()) {
				return null;
			}
			cur = cur.getAsJsonObject().get(key);
		}
		return cur;
	}
}
