package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Captures the just-received credential, whole, for later linkability analysis, appending it to the
 * {@code linkability_captures} list together with the issuer's response time (from the HTTP
 * {@code Date} response header).
 *
 * <p>Called immediately after a credential is parsed (before any notification request overwrites the
 * response headers), so the {@code Date} header reliably belongs to the response that carried this
 * credential. The issuer's {@code Date} is its own clock at the moment it produced the response, so
 * it is directly comparable to the credential's issuance timestamp across credentials, regardless of
 * when verification runs on our side. If the header is absent/unparseable, a capture-time
 * {@code Instant.now()} is used as a (noisier) fallback and flagged.
 *
 * <p>The whole credential is stored (the parsed {@code sdjwt} object for SD-JWT VCs, or the raw
 * {@code mdoc_credential_cbor} for mdoc) so later analysis is not limited to particular fields.
 * {@link VCIEnsureCredentialTimeClaimsNotLinkable} consumes the list wherever a test obtains two or
 * more credentials of the same dataset (RFC 9901 §10.1 unlinkability).
 */
public class VCICaptureCredentialForLinkability extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject entry = new JsonObject();

		JsonObject sdjwt = env.getObject("sdjwt");
		String mdocCbor = env.getString("mdoc_credential_cbor");
		String format;
		if (sdjwt != null) {
			format = "sd_jwt_vc";
			entry.addProperty("format", format);
			entry.add("sdjwt", sdjwt.deepCopy());
		} else if (mdocCbor != null) {
			format = "mso_mdoc";
			entry.addProperty("format", format);
			entry.addProperty("mdoc_credential_cbor", mdocCbor);
		} else {
			log("No parsed SD-JWT or mdoc credential present; nothing to capture for linkability analysis");
			return env;
		}

		// Issuance-time reference: the issuer's own clock from the HTTP Date response header.
		String dateStr = env.getString("resource_endpoint_response_headers", "date");
		Long parsedDate = parseHttpDate(dateStr);
		boolean fallback = (parsedDate == null);
		long responseTime = fallback ? Instant.now().getEpochSecond() : parsedDate;
		entry.addProperty("response_time", responseTime);
		entry.addProperty("response_time_is_fallback", fallback);

		JsonObject container = env.getObject("linkability_captures");
		JsonArray list;
		if (container == null) {
			container = new JsonObject();
			list = new JsonArray();
			container.add("list", list);
		} else {
			list = container.getAsJsonArray("list");
		}
		list.add(entry);
		env.putObject("linkability_captures", container);

		logSuccess("Captured credential for linkability analysis",
			args("format", format, "response_time", responseTime, "captured_count", list.size()));

		return env;
	}

	private Long parseHttpDate(String dateStr) {
		if (dateStr == null || dateStr.isBlank()) {
			return null;
		}
		try {
			return ZonedDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond();
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
