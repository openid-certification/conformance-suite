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
 * <p>12.3.6.3 defines one envelope for both revocation mechanisms, the status list (12.3.6.5)
 * and the identifier list (12.3.6.4), and an MSO uses at most one of them. The list is therefore
 * stored under one set of environment keys whichever mechanism referenced it, with
 * {@link #ENV_MECHANISM} recording which; only the MSO's own position in the list
 * ({@link #ENV_STATUS_LIST_IDX} or {@link #ENV_IDENTIFIER_LIST_ID}) is mechanism specific.
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

	/** Environment string holding the {@link Mechanism#msoElement} of the mechanism the MSO uses. */
	public static final String ENV_MECHANISM = "mdoc_revocation_list_mechanism";

	/** Environment object holding the revocation list HTTP response (status and headers). */
	public static final String ENV_RESPONSE = "mdoc_revocation_list_endpoint_response";

	/**
	 * The optional Certificate element of the MSO's status reference (base64 DER), the explicit
	 * trust point for the revocation list's x5chain when present (ISO/IEC 18013-5 12.3.6.2).
	 */
	public static final String ENV_REFERENCE_CERTIFICATE = "mdoc_revocation_list_reference_certificate";

	/** Environment integer holding the MSO's index into the status list (status list mechanism). */
	public static final String ENV_STATUS_LIST_IDX = "mdoc_status_list_idx";

	/**
	 * Environment string holding the MSO's own Identifier, the {@code id} element of the MSO's
	 * identifier_list structure, base64 encoded (identifier list mechanism).
	 */
	public static final String ENV_IDENTIFIER_LIST_ID = "mdoc_identifier_list_id";

	/**
	 * Environment string holding the mdoc's revocation status as read from the list, a
	 * {@link net.openid.conformance.oauth.statuslists.TokenStatusList.Status} name.
	 */
	public static final String ENV_STATUS = "mdoc_revocation_status";

	/** Media type of an identifier list in CWT format (ISO/IEC 18013-5 12.3.6.4). */
	public static final String IDENTIFIER_LIST_CWT_CONTENT_TYPE = "application/identifierlist+cwt";

	/** CWT claim key of the IdentifierList structure, ISO/IEC 18013-5 12.3.6.4. */
	protected static final long CWT_CLAIM_IDENTIFIER_LIST = 65530;

	/** The two MSO revocation mechanisms of ISO/IEC 18013-5 12.3.6 and how their lists differ on the wire. */
	public enum Mechanism {
		/**
		 * The status list mechanism (12.3.6.5). draft-ietf-oauth-status-list section 5.2 allows
		 * the type header to be the media type's registered CoAP Content-Format ID instead of
		 * the media type.
		 */
		STATUS_LIST("status_list", StatusListCwt.CONTENT_TYPE, StatusListCwt.COAP_CONTENT_FORMAT_ID),

		/** The identifier list mechanism (12.3.6.4). No CoAP Content-Format ID is registered for its media type. */
		IDENTIFIER_LIST("identifier_list", IDENTIFIER_LIST_CWT_CONTENT_TYPE, null);

		/** The name of the MSO status element that references the list, as stored in {@link #ENV_MECHANISM}. */
		public final String msoElement;

		/** The media type the list is served with and declares in its type header. */
		public final String contentType;

		/** The CoAP Content-Format ID registered for {@link #contentType}, or null when there is none. */
		public final Integer coapContentFormatId;

		Mechanism(String msoElement, String contentType, Integer coapContentFormatId) {
			this.msoElement = msoElement;
			this.contentType = contentType;
			this.coapContentFormatId = coapContentFormatId;
		}

		/** The mechanism whose {@link #msoElement} is {@code msoElement}, or null when there is none. */
		public static Mechanism fromMsoElement(String msoElement) {
			for (Mechanism mechanism : values()) {
				if (mechanism.msoElement.equals(msoElement)) {
					return mechanism;
				}
			}
			return null;
		}
	}

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

	/** The mechanism {@link FetchMdocRevocationList} recorded for the credential under validation. */
	protected Mechanism getMechanism(Environment env) {
		String msoElement = env.getString(ENV_MECHANISM);
		Mechanism mechanism = Mechanism.fromMsoElement(msoElement);
		if (mechanism == null) {
			throw error("The MSO revocation mechanism is missing from the environment",
				args("mechanism", msoElement));
		}
		return mechanism;
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
	protected byte[] fetchRevocationList(Environment env, String uri, Mechanism mechanism) {
		ResponseEntity<byte[]> response;
		try {
			response = getRevocationList(env, uri, mechanism.contentType);
		} catch (Exception e) {
			throw error("Unable to retrieve the MSO revocation list referenced by the mdoc's "
				+ mechanism.msoElement + " element", e, args("uri", uri));
		}

		env.putObject(ENV_RESPONSE,
			convertBinaryResponseForEnvironment("mdoc " + mechanism.msoElement + " token endpoint", response));

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw error("Failed to retrieve the MSO revocation list referenced by the mdoc's "
				+ mechanism.msoElement + " element",
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
	 * Checks the type (COSE header label 16) of the protected header against the mechanism's
	 * media type. draft-ietf-oauth-status-list section 5.2 allows either the media type or the
	 * media type's registered CoAP Content-Format ID, where one is registered.
	 */
	protected void checkType(Map<CoseLabel, DataItem> protectedHeaders, Mechanism mechanism,
			List<String> violations) {
		String expectedType = mechanism.contentType;
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
			if (mechanism.coapContentFormatId == null) {
				violations.add("the type in the protected header is a CoAP Content-Format ID ("
					+ typ + "); no Content-Format ID is registered for '" + expectedType + "'");
			} else if (typ != mechanism.coapContentFormatId) {
				violations.add("the type in the protected header is the CoAP Content-Format ID " + typ
					+ " rather than " + mechanism.coapContentFormatId + ", the ID registered for '"
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
	protected void checkSharedClaims(DataItem claims, String uri, Mechanism mechanism,
			List<String> violations) {
		DataItem sub = getClaim(claims, StatusListCwt.CLAIM_SUB);
		if (sub == null) {
			violations.add("the CWT claims set does not contain the sub claim (key 2)");
		} else if (!(sub instanceof Tstr subject)) {
			violations.add("the sub claim (key 2) is not a text string");
		} else if (!subject.getValue().equals(uri)) {
			violations.add("the sub claim (key 2) is '" + subject.getValue()
				+ "' but the MSO's " + mechanism.msoElement + " element references '" + uri + "'");
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
