package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Within a batch issuance, checks that the credentials' time claims are not a precise reflection of
 * the (single) issuance instant.
 *
 * <p>All credentials in a batch are issued in the same instant, so the cross-request "did the
 * timestamp advance by the inter-issuance gap" check ({@link VCIEnsureCredentialTimeClaimsNotLinkable})
 * does not apply. Instead, per RFC 9901 §10.1, every time claim — the issuance-time claims (SD-JWT
 * {@code iat}/{@code nbf}, mdoc MSO {@code signed}/{@code validFrom}) and the expiry claims (SD-JWT
 * {@code exp}, mdoc MSO {@code validUntil}, which are derived from the issuance time) — is classified
 * across the batch:
 *
 * <ul>
 *   <li>rounded -&gt; every value sits on a coarse (hour or day) boundary -&gt; pass;</li>
 *   <li>randomized -&gt; the values are spread wider than the near-now window, providing cover
 *       -&gt; pass;</li>
 *   <li>precise -&gt; the values are a tight, high-entropy (non-rounded) cluster -&gt; FAIL: a
 *       precise timestamp shared across the batch lets verifiers correlate the credentials.</li>
 * </ul>
 *
 * <p>For issuance claims a tight non-rounded cluster is only treated as precise when it sits at the
 * issuance instant (the response {@code Date}); a tight cluster far from now is a fixed/constant
 * value shared by all of the issuer's credentials (herd-private, not linkable). Expiry claims are
 * future-dated ({@code now + ttl}) so there is no "now" to anchor to — a tight non-rounded cluster
 * there directly carries the precise issuance time the expiry was derived from, so it is precise
 * regardless of distance from now (this is what catches an issuer that rounds {@code iat} but sets
 * {@code exp = precise_now + ttl}).
 *
 * <p>Values on (or one second before) an hour boundary are treated as rounded even when near "now" —
 * a genuinely precise timestamp lands there with probability ~2/3600, whereas flagging them would
 * make an hour-rounding issuer fail only when issuance happens to straddle the boundary (an
 * intermittent verdict). The one-second-before case covers the common <em>inclusive end of period</em>
 * convention for expiry claims, where {@code exp} is set to the last second of the validity window
 * (e.g. {@code 23:59:59}, equivalent for linkability to {@code 00:00:00} of the next day). Sub-hour
 * rounding (e.g. to the minute) is still treated as precise, as is a precise batch whose
 * per-credential timestamps differ by only a few seconds (a tight cluster, not cover).
 */
public class VCIEnsureBatchTimeClaimsNotLinkable extends AbstractVCICredentialTimeClaimsCheck {

	private static final long HOUR_SECONDS = 3600L;
	private static final long NEAR_NOW_SECONDS = 120L;
	private static final long NEAR_NOW_FALLBACK_SECONDS = 300L;

	private static final List<String> SD_JWT_ISSUANCE_CLAIMS = List.of("iat", "nbf");
	private static final List<String> SD_JWT_EXPIRY_CLAIMS = List.of("exp");
	private static final List<String> MDOC_ISSUANCE_CLAIMS = List.of("signed", "validFrom");
	private static final List<String> MDOC_EXPIRY_CLAIMS = List.of("validUntil");

	private enum Assessment { NOT_PRESENT, OK, PRECISE }

	@Override
	@PreEnvironment(required = "linkability_captures")
	public Environment evaluate(Environment env) {
		JsonArray captures = capturesList(env);
		if (captures == null || captures.size() < 2) {
			log("Fewer than two credentials were captured for the batch; cannot assess time-claim linkability",
				args("captured_count", captures == null ? 0 : captures.size()));
			return env;
		}

		String format = OIDFJSON.getStringOrNull(captures.get(0).getAsJsonObject().get("format"));
		boolean mdoc = "mso_mdoc".equals(format);
		List<String> issuanceClaims = mdoc ? MDOC_ISSUANCE_CLAIMS : SD_JWT_ISSUANCE_CLAIMS;
		List<String> expiryClaims = mdoc ? MDOC_EXPIRY_CLAIMS : SD_JWT_EXPIRY_CLAIMS;

		List<Map<String, Long>> perCredential = new ArrayList<>();
		long now = 0;
		boolean fallback = false;
		for (int i = 0; i < captures.size(); i++) {
			JsonObject capture = captures.get(i).getAsJsonObject();
			perCredential.add(timestamps(capture, format));
			now = Math.max(now, OIDFJSON.getLong(capture.get("response_time")));
			fallback = fallback || getBoolean(capture, "response_time_is_fallback");
		}
		long nearNow = fallback ? NEAR_NOW_FALLBACK_SECONDS : NEAR_NOW_SECONDS;

		Map<String, Object> details = new LinkedHashMap<>();
		details.put("format", format);
		details.put("now", now);

		List<String> preciseClaims = new ArrayList<>();
		boolean anyAssessed = false;
		for (String name : issuanceClaims) {
			Assessment a = assess(name, perCredential, now, nearNow, details, true);
			anyAssessed |= (a != Assessment.NOT_PRESENT);
			if (a == Assessment.PRECISE) {
				preciseClaims.add(name);
			}
		}
		for (String name : expiryClaims) {
			Assessment a = assess(name, perCredential, now, nearNow, details, false);
			anyAssessed |= (a != Assessment.NOT_PRESENT);
			if (a == Assessment.PRECISE) {
				preciseClaims.add(name);
			}
		}

		if (!anyAssessed) {
			log("No time claim is present across the batch credentials; cannot assess time-claim "
					+ "linkability", args("format", format));
			return env;
		}

		if (!preciseClaims.isEmpty()) {
			details.put("precise_time_claims", preciseClaims);
			details.put("near_now_seconds", nearNow);
			throw error("The batch credentials share a precise, high-entropy time claim " + preciseClaims
					+ " (a tight cluster of non-rounded values, at the issuance instant for issuance claims), "
					+ "which lets verifiers correlate the credentials. Per RFC 9901 §10.1, the time claims "
					+ "(including exp / mdoc validUntil) must be randomized or rounded.",
				details);
		}

		logSuccess("The batch credentials' time claims are randomized or rounded, not a precise "
				+ "shared timestamp", details);
		return env;
	}

	/**
	 * Classifies one time claim across the batch. {@code issuanceClaim} anchors the precise verdict
	 * to the issuance instant ({@code now}); expiry claims are future-dated and so are judged on the
	 * tight-cluster + not-rounded signal alone.
	 */
	private Assessment assess(String name, List<Map<String, Long>> perCredential, long now, long nearNow,
							  Map<String, Object> details, boolean issuanceClaim) {
		List<Long> values = new ArrayList<>();
		for (Map<String, Long> ts : perCredential) {
			Long v = ts.get(name);
			if (v != null) {
				values.add(v);
			}
		}
		if (values.size() < 2) {
			return Assessment.NOT_PRESENT; // claim not present across the batch
		}
		Set<Long> distinct = new LinkedHashSet<>(values);
		details.put(name + "_distinct_values", distinct.toString());

		// Rounded to the hour or coarser (day boundaries are also hour boundaries) -> not distinctive.
		if (distinct.stream().allMatch(this::isHourAligned)) {
			return Assessment.OK;
		}
		long min = Collections.min(distinct);
		long max = Collections.max(distinct);
		// Spread wider than the near-now window -> genuinely randomized (provides cover).
		if (max - min > nearNow) {
			return Assessment.OK;
		}
		// A tight cluster of non-rounded values. For issuance claims this is only precise when the
		// cluster sits at the issuance instant; otherwise it is a fixed/constant value (herd-private).
		// Expiry claims have no "now" anchor, so the tight non-rounded cluster is itself the signal.
		if (!issuanceClaim || Math.abs(now - min) <= nearNow || Math.abs(now - max) <= nearNow) {
			return Assessment.PRECISE;
		}
		return Assessment.OK;
	}

	/**
	 * Treats a value as rounded to an hour (or coarser) boundary. Accepts both the exact boundary
	 * ({@code 00:00:00}, the result of rounding an issuance time down) and the inclusive end of a
	 * period ({@code 23:59:59}, one second earlier — the common convention for expiry claims, where
	 * {@code exp} is the last second of the validity window). The two are equivalent for linkability.
	 */
	private boolean isHourAligned(long v) {
		return v % HOUR_SECONDS == 0 || (v + 1) % HOUR_SECONDS == 0;
	}
}
