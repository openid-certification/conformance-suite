package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.MajorType;
import org.multipaz.cbor.Tstr;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseLabel;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.X509CertChain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates that the fetched MSO revocation list is a well formed identifier list in CWT format.
 *
 * <p>ISO/IEC 18013-5 12.3.6.3 defines the envelope, shared with the status list mechanism:
 * <ul>
 *   <li>a COSE_Sign1 object, tagged with the COSE_Sign1 tag (18) as
 *       draft-ietf-oauth-status-list section 5.2 requires;</li>
 *   <li>signed with ES256, ES384, ES512 or EdDSA;</li>
 *   <li>carrying the x5chain in the <em>protected</em> header;</li>
 *   <li>with a CWT claims set containing sub, iat and the (mandatory, per 12.3.6.3) exp claim.</li>
 * </ul>
 *
 * <p>12.3.6.4 then deviates from the Token Status List specification: the type claim shall be
 * {@code application/identifierlist+cwt}, the StatusList claim shall <em>not</em> be present, and
 * the IdentifierList structure shall be present at CWT claim key 65530, following the CDDL
 * {@code IdentifierList = { "identifiers" : { * Identifier => IdentifierInfo }, ?
 * "aggregation_uri" : Aggregation_uri, * tstr => RFU }} with {@code Identifier = bstr}.
 */
public class ValidateIdentifierListTokenCwtFormat extends AbstractIdentifierListCwtCondition {

	// COSE algorithm identifiers permitted by ISO/IEC 18013-5 12.3.6.3
	private static final Map<Long, String> ALLOWED_ALGORITHMS = Map.of(
		-7L, "ES256",
		-35L, "ES384",
		-36L, "ES512",
		-8L, "EdDSA"
	);

	@Override
	@PreEnvironment(strings = { ENV_IDENTIFIER_LIST_TOKEN, ENV_IDENTIFIER_LIST_URI })
	public Environment evaluate(Environment env) {
		ParsedStatusListCwt parsed = parseStatusListCwt(env);
		CoseSign1 coseSign1 = parsed.coseSign1();

		List<String> violations = new ArrayList<>();

		if (!parsed.tagged()) {
			violations.add("the token is not tagged with the COSE_Sign1 tag (18);"
				+ " draft-ietf-oauth-status-list section 5.2 requires the tagged form");
		}

		Map<CoseLabel, DataItem> protectedHeaders = coseSign1.getProtectedHeaders();

		String algorithm = checkAlgorithm(protectedHeaders, violations);
		checkType(protectedHeaders, violations);

		X509CertChain x5chain = getProtectedX5chain(coseSign1);
		if (x5chain == null) {
			violations.add("the x5chain (COSE header label 33) is missing from the protected header;"
				+ " ISO/IEC 18013-5 12.3.6.3 requires it to be present there");
		} else if (x5chain.getCertificates().isEmpty()) {
			violations.add("the x5chain in the protected header is empty");
		}

		int identifierCount = checkClaims(env, parsed.claims(), violations);

		if (!violations.isEmpty()) {
			throw error("The MSO revocation list is not a valid identifier list in CWT format: "
					+ String.join("; ", violations),
				args("violations", violations, "uri", env.getString(ENV_IDENTIFIER_LIST_URI)));
		}

		logSuccess("The MSO revocation list is a valid identifier list in CWT format",
			args("algorithm", algorithm,
				"uri", env.getString(ENV_IDENTIFIER_LIST_URI),
				"identifiers", identifierCount,
				"chain_length", x5chain == null ? 0 : x5chain.getCertificates().size()));
		return env;
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
	 * ISO/IEC 18013-5 12.3.6.4: "The value of the type claim shall be
	 * 'application/identifierlist+cwt'".
	 *
	 * <p>12.3.6.4 says "type claim" but the CWT claims set has no registered type claim; the
	 * Token Status List specification the envelope is inherited from carries it as the COSE typ
	 * header parameter (protected header label 16), which is where it is looked for here - the
	 * same place {@link ValidateStatusListTokenCwtFormat} looks for the status list media type.
	 */
	private void checkType(Map<CoseLabel, DataItem> protectedHeaders, List<String> violations) {
		DataItem typItem = protectedHeaders.get(new CoseNumberLabel(Cose.COSE_LABEL_TYP));
		if (typItem == null) {
			violations.add("the type (COSE header label 16) is missing from the protected header;"
				+ " draft-ietf-oauth-status-list section 5.2 requires it to be present");
			return;
		}
		if (typItem instanceof Tstr typ) {
			if (!IDENTIFIER_LIST_CWT_CONTENT_TYPE.equals(typ.getValue())) {
				violations.add("the type in the protected header is '" + typ.getValue()
					+ "' rather than '" + IDENTIFIER_LIST_CWT_CONTENT_TYPE
					+ "', which ISO/IEC 18013-5 12.3.6.4 requires for the identifier list mechanism");
			}
			return;
		}
		if (typItem.getMajorType() == MajorType.UNSIGNED_INTEGER) {
			// draft-ietf-oauth-status-list also allows the registered CoAP Content-Format ID, but
			// no such ID is registered for application/identifierlist+cwt
			violations.add("the type in the protected header is a CoAP Content-Format ID ("
				+ typItem.getAsNumber() + "); no Content-Format ID is registered for '"
				+ IDENTIFIER_LIST_CWT_CONTENT_TYPE + "'");
			return;
		}
		violations.add("the type in the protected header is neither a media type string nor a"
			+ " CoAP Content-Format ID");
	}

	/** @return the number of entries in the identifiers map, or 0 when it could not be read */
	private int checkClaims(Environment env, DataItem claims, List<String> violations) {
		String uri = env.getString(ENV_IDENTIFIER_LIST_URI);

		DataItem sub = getClaim(claims, CWT_CLAIM_SUB);
		if (sub == null) {
			violations.add("the CWT claims set does not contain the sub claim (key 2)");
		} else if (!(sub instanceof Tstr subject)) {
			violations.add("the sub claim (key 2) is not a text string");
		} else if (!subject.getValue().equals(uri)) {
			violations.add("the sub claim (key 2) is '" + subject.getValue()
				+ "' but the MSO's identifier_list element references '" + uri + "'");
		}

		if (getClaim(claims, CWT_CLAIM_IAT) == null) {
			violations.add("the CWT claims set does not contain the iat claim (key 6)");
		}

		DataItem exp = getClaim(claims, CWT_CLAIM_EXP);
		if (exp == null) {
			violations.add("the CWT claims set does not contain the exp claim (key 4);"
				+ " ISO/IEC 18013-5 12.3.6.3 says the exp claim shall be present");
		} else {
			try {
				long expiry = exp.getAsNumber();
				if (Instant.ofEpochSecond(expiry).isBefore(Instant.now())) {
					violations.add("the MSO revocation list expired at " + Instant.ofEpochSecond(expiry));
				}
			} catch (Exception e) {
				violations.add("the exp claim (key 4) is not a number");
			}
		}

		if (getClaim(claims, CWT_CLAIM_STATUS_LIST) != null) {
			violations.add("the CWT claims set contains the StatusList claim (key 65533);"
				+ " ISO/IEC 18013-5 12.3.6.4 says it shall not be present in an identifier list");
		}

		return checkIdentifierListClaim(claims, violations);
	}

	private int checkIdentifierListClaim(DataItem claims, List<String> violations) {
		DataItem identifierList = getClaim(claims, CWT_CLAIM_IDENTIFIER_LIST);
		if (identifierList == null) {
			violations.add("the CWT claims set does not contain the IdentifierList claim (key 65530);"
				+ " ISO/IEC 18013-5 12.3.6.4 requires it");
			return 0;
		}
		if (!(identifierList instanceof CborMap)) {
			violations.add("the IdentifierList claim (key 65530) is not a CBOR map");
			return 0;
		}

		DataItem identifiers = identifierList.getOrNull("identifiers");
		if (identifiers == null) {
			violations.add("the IdentifierList claim does not contain the required 'identifiers' element");
			return 0;
		}
		if (!(identifiers instanceof CborMap identifiersMap)) {
			violations.add("the IdentifierList claim's 'identifiers' element is not a CBOR map;"
				+ " the ISO/IEC 18013-5 12.3.6.4 CDDL defines it as { * Identifier => IdentifierInfo }");
			return 0;
		}

		int nonByteStringKeys = 0;
		for (DataItem key : identifiersMap.getItems().keySet()) {
			if (!(key instanceof Bstr)) {
				nonByteStringKeys++;
			}
		}
		if (nonByteStringKeys > 0) {
			violations.add(nonByteStringKeys + " of the 'identifiers' map's keys are not CBOR byte"
				+ " strings; the ISO/IEC 18013-5 12.3.6.4 CDDL defines Identifier = bstr");
		}

		DataItem aggregationUri = identifierList.getOrNull("aggregation_uri");
		if (aggregationUri != null && !(aggregationUri instanceof Tstr)) {
			violations.add("the IdentifierList claim's optional 'aggregation_uri' element is not a"
				+ " text string");
		}

		return identifiersMap.getItems().size();
	}
}
