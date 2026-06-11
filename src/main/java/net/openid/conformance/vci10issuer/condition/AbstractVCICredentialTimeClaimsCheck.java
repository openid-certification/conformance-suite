package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonObjectUtils;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared extraction for the credential time-claim linkability checks. Reads the whole-credential
 * captures collected by {@link VCICaptureCredentialForLinkability} and exposes each credential's
 * time claims as epoch seconds: SD-JWT {@code iat}/{@code exp}/{@code nbf}, or the mdoc MSO
 * validityInfo {@code signed}/{@code validFrom}/{@code validUntil}.
 */
public abstract class AbstractVCICredentialTimeClaimsCheck extends AbstractCondition {

	/** The captured credentials, or null if none were captured. */
	protected JsonArray capturesList(Environment env) {
		JsonElement listEl = env.getElementFromObject("linkability_captures", "list");
		return (listEl != null && listEl.isJsonArray()) ? listEl.getAsJsonArray() : null;
	}

	/** A credential's time claims in epoch seconds. Absent claims are omitted. */
	protected Map<String, Long> timestamps(JsonObject capture, String format) {
		Map<String, Long> result = new LinkedHashMap<>();
		if ("mso_mdoc".equals(format)) {
			MobileSecurityObject mso = parseMso(capture);
			if (mso != null) {
				putInstant(result, "signed", mso.getSignedAt());
				putInstant(result, "validFrom", mso.getValidFrom());
				putInstant(result, "validUntil", mso.getValidUntil());
			}
			return result;
		}
		putClaim(result, "iat", JsonObjectUtils.path(capture, "sdjwt", "credential", "claims", "iat"));
		putClaim(result, "exp", JsonObjectUtils.path(capture, "sdjwt", "credential", "claims", "exp"));
		putClaim(result, "nbf", JsonObjectUtils.path(capture, "sdjwt", "credential", "claims", "nbf"));
		return result;
	}

	private void putClaim(Map<String, Long> map, String name, JsonElement el) {
		if (el != null && !el.isJsonNull()) {
			map.put(name, OIDFJSON.getLong(el));
		}
	}

	private void putInstant(Map<String, Long> map, String name, kotlin.time.Instant instant) {
		if (instant == null) {
			return;
		}
		try {
			// kotlin.time.Instant -> ISO-8601 string -> epoch seconds
			map.put(name, Instant.parse(instant.toString()).getEpochSecond());
		} catch (RuntimeException e) {
			// leave the claim out if it can't be parsed
		}
	}

	/** The capture's MSO, or null if it has none or it cannot be parsed (callers skip it then). */
	protected MobileSecurityObject parseMso(JsonObject capture) {
		byte[] bytes = mdocBytes(capture);
		if (bytes == null) {
			return null;
		}
		try {
			return MdocUtil.parseMso(bytes);
		} catch (MdocUtil.MdocParseException e) {
			return null;
		}
	}

	protected byte[] mdocBytes(JsonObject capture) {
		String cbor = OIDFJSON.getStringOrNull(capture.get("mdoc_credential_cbor"));
		if (cbor == null) {
			return null;
		}
		try {
			return Base64.getDecoder().decode(cbor);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	protected boolean getBoolean(JsonObject o, String key) {
		JsonElement el = o.get(key);
		return el != null && !el.isJsonNull() && OIDFJSON.getBoolean(el);
	}

}
