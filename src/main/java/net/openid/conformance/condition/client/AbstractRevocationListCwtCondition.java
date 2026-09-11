package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.oauth.statuslists.StatusListCwt;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborDouble;
import org.multipaz.cbor.CborFloat;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.MajorType;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseLabel;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.mdoc.mso.MobileSecurityObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Shared plumbing for the conditions that consume an MSO revocation list, i.e. a Status List
 * Token in CWT format as required by ISO/IEC 18013-5 12.3.6.3.
 *
 * <p>The raw token bytes are held base64 encoded in the {@link #ENV_TOKEN} environment string
 * (written by {@link FetchMdocRevocationList}); each condition re-parses them so no non-JSON
 * object needs to live in the environment.
 */
public abstract class AbstractRevocationListCwtCondition extends AbstractCondition {

	/** Environment string holding the base64 encoded revocation list token bytes. */
	public static final String ENV_TOKEN = "mdoc_revocation_list_token";

	/** Environment string holding the URI the revocation list was fetched from. */
	public static final String ENV_URI = "mdoc_revocation_list_uri";

	/** Environment object holding the revocation list HTTP response (status and headers). */
	public static final String ENV_RESPONSE = "mdoc_revocation_list_endpoint_response";

	/**
	 * The optional Certificate element of the MSO's status reference (base64 DER), the explicit
	 * trust point for the revocation list's x5chain when present (ISO/IEC 18013-5 12.3.6.2).
	 */
	public static final String ENV_REFERENCE_CERTIFICATE = "mdoc_revocation_list_reference_certificate";

	/** Environment integer holding the MSO's index into the status list. */
	public static final String ENV_STATUS_LIST_IDX = "mdoc_status_list_idx";

	/**
	 * Environment string holding the mdoc's revocation status as read from the list, a
	 * {@link net.openid.conformance.oauth.statuslists.TokenStatusList.Status} name.
	 */
	public static final String ENV_STATUS = "mdoc_revocation_status";

	/**
	 * COSE algorithm identifiers permitted for an MSO revocation list by ISO/IEC 18013-5 12.3.6.3,
	 * mapped to their names.
	 */
	protected static final Map<Long, String> ALLOWED_ALGORITHMS = Map.of(
		-7L, "ES256",
		-35L, "ES384",
		-36L, "ES512",
		-8L, "EdDSA"
	);

	/**
	 * A parsed MSO revocation list token.
	 *
	 * @param coseSign1 the COSE_Sign1 structure
	 * @param tagged whether it was tagged with the COSE_Sign1 tag (18), which
	 *   draft-ietf-oauth-status-list section 5.2 requires
	 * @param claims the CWT claims set decoded from the COSE_Sign1 payload
	 */
	protected record ParsedRevocationListCwt(CoseSign1 coseSign1, boolean tagged, DataItem claims) {
	}

	protected byte[] getRevocationListTokenBytes(Environment env) {
		String encoded = env.getString(ENV_TOKEN);
		if (encoded == null) {
			throw error("No MSO revocation list token found in the environment");
		}
		try {
			return Base64.getDecoder().decode(encoded);
		} catch (IllegalArgumentException e) {
			throw error("Failed to base64 decode the stored MSO revocation list token", e);
		}
	}

	/**
	 * Parses the fetched MSO revocation list as a COSE_Sign1 with a CWT claims set payload. Only
	 * structural failures that make any further checking impossible throw here; the individual
	 * ISO/IEC 18013-5 12.3.6.3 requirements are reported by
	 * {@link ValidateMdocRevocationListCwtFormat}.
	 */
	protected ParsedRevocationListCwt parseRevocationListCwt(Environment env) {
		byte[] tokenBytes = getRevocationListTokenBytes(env);

		DataItem decoded;
		try {
			decoded = Cbor.INSTANCE.decode(tokenBytes);
		} catch (Exception e) {
			throw error("The MSO revocation list is not valid CBOR; ISO/IEC 18013-5 12.3.6.3 requires"
				+ " it to be a Status List Token in CWT format", e);
		}

		boolean tagged = false;
		DataItem coseItem = decoded;
		if (decoded instanceof Tagged tag) {
			if (tag.getTagNumber() != Tagged.COSE_SIGN1) {
				throw error("The MSO revocation list is tagged with CBOR tag " + tag.getTagNumber()
					+ "; draft-ietf-oauth-status-list section 5.2 requires the COSE_Sign1 tag (18)");
			}
			tagged = true;
			coseItem = tag.getTaggedItem();
		}

		CoseSign1 coseSign1;
		try {
			coseSign1 = CoseSign1.Companion.fromDataItem(coseItem);
		} catch (Exception e) {
			throw error("The MSO revocation list could not be parsed as a COSE_Sign1 structure;"
				+ " ISO/IEC 18013-5 12.3.6.3 requires the MSO revocation list to be a COSE_Sign1 object", e);
		}

		byte[] payload = coseSign1.getPayload();
		if (payload == null || payload.length == 0) {
			throw error("The MSO revocation list's COSE_Sign1 structure has no payload,"
				+ " so it carries no CWT claims set");
		}

		DataItem claims;
		try {
			claims = Cbor.INSTANCE.decode(payload);
		} catch (Exception e) {
			throw error("The MSO revocation list's CWT claims set is not valid CBOR", e);
		}
		if (!(claims instanceof CborMap)) {
			throw error("The MSO revocation list's CWT claims set is not a CBOR map");
		}

		return new ParsedRevocationListCwt(coseSign1, tagged, claims);
	}

	/**
	 * Returns the x5chain (COSE header label 33) from the protected headers, or null when it is
	 * absent. ISO/IEC 18013-5 12.3.6.3 requires it to be in the protected header.
	 */
	protected X509CertChain getProtectedX5chain(CoseSign1 coseSign1) {
		Map<CoseLabel, DataItem> protectedHeaders = coseSign1.getProtectedHeaders();
		DataItem x5chainItem = protectedHeaders.get(new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN));
		if (x5chainItem == null) {
			return null;
		}
		try {
			return x5chainItem.getAsX509CertChain();
		} catch (Exception e) {
			throw error("The MSO revocation list's x5chain protected header could not be parsed"
				+ " as an X.509 certificate chain", e);
		}
	}

	/**
	 * Returns the non-empty x5chain from the protected headers, for the checks that cannot run
	 * without it.
	 *
	 * @param purpose what the chain is needed for, completing "..., so " in the failure message
	 */
	protected X509CertChain requireProtectedX5chain(CoseSign1 coseSign1, String purpose) {
		X509CertChain x5chain = getProtectedX5chain(coseSign1);
		if (x5chain == null || x5chain.getCertificates().isEmpty()) {
			throw error("The MSO revocation list does not contain an x5chain in its protected header,"
				+ " so " + purpose);
		}
		return x5chain;
	}

	/** Parses the Mobile Security Object of the mdoc credential under validation. */
	protected MobileSecurityObject parseCredentialMso(Environment env) {
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(env.getString("mdoc_credential_cbor"));
		} catch (IllegalArgumentException e) {
			throw error("Failed to decode mdoc_credential_cbor from base64", e);
		}
		try {
			return MdocUtil.parseMso(bytes);
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		}
	}

	/**
	 * Retrieves an MSO revocation list over HTTP, records the response in
	 * {@link #ENV_RESPONSE} and returns its body.
	 */
	protected byte[] fetchRevocationList(Environment env, String uri) {
		ResponseEntity<byte[]> response;
		try {
			response = getRevocationList(env, uri, StatusListCwt.CONTENT_TYPE);
		} catch (Exception e) {
			throw error("Unable to retrieve the MSO revocation list referenced by the mdoc's"
				+ " status_list element", e, args("uri", uri));
		}

		env.putObject(ENV_RESPONSE,
			convertBinaryResponseForEnvironment("mdoc status_list token endpoint", response));

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw error("Failed to retrieve the MSO revocation list referenced by the mdoc's"
				+ " status_list element",
				args("uri", uri, "status", response.getStatusCode().value()));
		}

		byte[] body = response.getBody();
		if (body == null || body.length == 0) {
			throw error("The MSO revocation list endpoint returned an empty body", args("uri", uri));
		}
		return body;
	}

	/** The HTTP GET itself; overridden by the unit tests to serve a canned response. */
	protected ResponseEntity<byte[]> getRevocationList(Environment env, String uri,
			String acceptContentType) throws Exception {
		RestTemplate restTemplate = createRestTemplate(env);
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, acceptContentType);
		return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
	}

	/**
	 * The result of {@link #checkSharedEnvelope}.
	 *
	 * @param algorithm the signature algorithm's name, or its COSE identifier when unrecognised,
	 *   or null when the algorithm header is missing or unreadable
	 * @param x5chain the certificate chain from the protected header, or null when absent
	 */
	protected record CwtEnvelope(String algorithm, X509CertChain x5chain) {

		public int chainLength() {
			return x5chain == null ? 0 : x5chain.getCertificates().size();
		}
	}

	/**
	 * Checks the parts of the envelope ISO/IEC 18013-5 12.3.6.3 defines identically for both
	 * revocation list mechanisms: the COSE_Sign1 tag, the signature algorithm and the presence of
	 * the x5chain in the protected header. The type header and the claims set are mechanism
	 * specific and are checked by the caller.
	 */
	protected CwtEnvelope checkSharedEnvelope(ParsedRevocationListCwt parsed, List<String> violations) {
		if (!parsed.tagged()) {
			violations.add("the token is not tagged with the COSE_Sign1 tag (18);"
				+ " draft-ietf-oauth-status-list section 5.2 requires the tagged form");
		}

		String algorithm = checkAlgorithm(parsed.coseSign1().getProtectedHeaders(), violations);

		X509CertChain x5chain = getProtectedX5chain(parsed.coseSign1());
		if (x5chain == null) {
			violations.add("the x5chain (COSE header label 33) is missing from the protected header;"
				+ " ISO/IEC 18013-5 12.3.6.3 requires it to be present there");
		} else if (x5chain.getCertificates().isEmpty()) {
			violations.add("the x5chain in the protected header is empty");
		}

		return new CwtEnvelope(algorithm, x5chain);
	}

	private String checkAlgorithm(Map<CoseLabel, DataItem> protectedHeaders, List<String> violations) {
		DataItem algItem = protectedHeaders.get(new CoseNumberLabel(Cose.COSE_LABEL_ALG));
		if (algItem == null) {
			violations.add("the algorithm (COSE header label 1) is missing from the protected header");
			return null;
		}
		long alg;
		try {
			alg = algItem.getAsNumber();
		} catch (Exception e) {
			violations.add("the algorithm in the protected header is not a number");
			return null;
		}
		String name = ALLOWED_ALGORITHMS.get(alg);
		if (name == null) {
			violations.add("the signature algorithm (COSE algorithm identifier " + alg
				+ ") is not one of ES256, ES384, ES512 or EdDSA, which are the only algorithms"
				+ " ISO/IEC 18013-5 12.3.6.3 permits for an MSO revocation list");
			return String.valueOf(alg);
		}
		return name;
	}

	/**
	 * Checks the type (COSE header label 16) of the protected header against the status list
	 * media type. draft-ietf-oauth-status-list section 5.2 allows either the media type or the
	 * media type's registered CoAP Content-Format ID.
	 */
	protected void checkType(Map<CoseLabel, DataItem> protectedHeaders, List<String> violations) {
		String expectedType = StatusListCwt.CONTENT_TYPE;
		DataItem typItem = protectedHeaders.get(new CoseNumberLabel(Cose.COSE_LABEL_TYP));
		if (typItem == null) {
			violations.add("the type (COSE header label 16) is missing from the protected header;"
				+ " draft-ietf-oauth-status-list section 5.2 requires it to be present");
			return;
		}
		if (typItem instanceof Tstr typ) {
			if (!expectedType.equals(typ.getValue())) {
				violations.add("the type in the protected header is '" + typ.getValue()
					+ "' rather than the required '" + expectedType + "'");
			}
			return;
		}
		if (typItem.getMajorType() == MajorType.UNSIGNED_INTEGER) {
			long typ = typItem.getAsNumber();
			if (typ != StatusListCwt.COAP_CONTENT_FORMAT_ID) {
				violations.add("the type in the protected header is the CoAP Content-Format ID " + typ
					+ " rather than " + StatusListCwt.COAP_CONTENT_FORMAT_ID + ", the ID registered for '"
					+ expectedType + "'");
			} else {
				log("The MSO revocation list's type header is the CoAP Content-Format ID registered for the"
					+ " media type", args("typ", typ));
			}
			return;
		}
		violations.add("the type in the protected header is neither a media type string nor a"
			+ " CoAP Content-Format ID");
	}

	/**
	 * Checks the claims ISO/IEC 18013-5 12.3.6.3 requires of both mechanisms: sub matching the URI
	 * the token was fetched from, iat, and exp (which 12.3.6.3 makes mandatory) not in the past;
	 * plus, per draft-ietf-oauth-status-list section 5.2, that iat is a NumericDate and that
	 * the optional ttl is a positive unsigned integer.
	 */
	protected void checkSharedClaims(DataItem claims, String uri, List<String> violations) {
		DataItem sub = getClaim(claims, StatusListCwt.CLAIM_SUB);
		if (sub == null) {
			violations.add("the CWT claims set does not contain the sub claim (key 2)");
		} else if (!(sub instanceof Tstr subject)) {
			violations.add("the sub claim (key 2) is not a text string");
		} else if (!subject.getValue().equals(uri)) {
			violations.add("the sub claim (key 2) is '" + subject.getValue()
				+ "' but the MSO's status_list element references '" + uri + "'");
		}

		DataItem iat = getClaim(claims, StatusListCwt.CLAIM_IAT);
		if (iat == null) {
			violations.add("the CWT claims set does not contain the iat claim (key 6)");
		} else if (!isNumericDate(iat)) {
			violations.add("the iat claim (key 6) is not a NumericDate (an integer or floating point number)");
		}

		DataItem ttl = getClaim(claims, StatusListCwt.CLAIM_TTL);
		if (ttl != null) {
			if (ttl.getMajorType() != MajorType.UNSIGNED_INTEGER) {
				violations.add("the ttl claim (key 65534) is not an unsigned integer (CBOR major type 0);"
					+ " draft-ietf-oauth-status-list section 5.2 requires it to be a positive number");
			} else if (ttl.getAsNumber() <= 0) {
				violations.add("the ttl claim (key 65534) is " + ttl.getAsNumber()
					+ "; draft-ietf-oauth-status-list section 5.2 requires it to be a positive number");
			}
		}

		DataItem exp = getClaim(claims, StatusListCwt.CLAIM_EXP);
		if (exp == null) {
			violations.add("the CWT claims set does not contain the exp claim (key 4);"
				+ " ISO/IEC 18013-5 12.3.6.3 says the exp claim shall be present");
			return;
		}
		Long expiry = numericDateSeconds(exp);
		if (expiry == null) {
			violations.add("the exp claim (key 4) is not a NumericDate (an integer or floating point number)");
		} else if (Instant.ofEpochSecond(expiry).isBefore(Instant.now())) {
			violations.add("the MSO revocation list expired at " + Instant.ofEpochSecond(expiry));
		}
	}

	/** Whether the item can hold an RFC 8392 NumericDate: an integer or a floating point number. */
	private static boolean isNumericDate(DataItem item) {
		return numericDateSeconds(item) != null;
	}

	/**
	 * Reads an RFC 8392 NumericDate, an integer or floating point number of seconds since the
	 * epoch, rounding a fractional value down; null when the item is neither kind of number.
	 */
	protected static Long numericDateSeconds(DataItem item) {
		if (item.getMajorType() == MajorType.UNSIGNED_INTEGER
			|| item.getMajorType() == MajorType.NEGATIVE_INTEGER) {
			return item.getAsNumber();
		}
		if (item instanceof CborFloat) {
			return (long) Math.floor(item.getAsFloat());
		}
		if (item instanceof CborDouble) {
			return (long) Math.floor(item.getAsDouble());
		}
		return null;
	}

	/** Returns the claim with the given CWT claim key, or null when it is absent. */
	protected DataItem getClaim(DataItem claims, long key) {
		try {
			return claims.getOrNull(key);
		} catch (Exception e) {
			return null;
		}
	}
}
