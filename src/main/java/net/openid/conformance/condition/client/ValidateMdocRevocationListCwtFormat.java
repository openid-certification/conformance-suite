package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.StatusListCwt;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tstr;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that the fetched MSO revocation list is a well formed Status List Token in CWT
 * format, as ISO/IEC 18013-5 12.3.6.3 requires of both revocation mechanisms:
 * <ul>
 *   <li>a COSE_Sign1 object, tagged with the COSE_Sign1 tag (18) as
 *       draft-ietf-oauth-status-list section 5.2 requires;</li>
 *   <li>signed with ES256, ES384, ES512 or EdDSA;</li>
 *   <li>carrying the x5chain in the <em>protected</em> header;</li>
 *   <li>declaring the mechanism's media type in the protected header;</li>
 *   <li>with a CWT claims set containing sub, iat and the (mandatory, per 12.3.6.3) exp claim.</li>
 * </ul>
 *
 * <p>For the status list mechanism the claims set must also carry a status_list claim whose
 * {@code bits} is 1 (12.3.6.5).
 *
 * <p>For the identifier list mechanism, 12.3.6.4 says "type claim" but the CWT claims set has no
 * registered type claim; the Token Status List specification the envelope is inherited from
 * carries it as the COSE typ header parameter (protected header label 16), which is where it is
 * looked for here. 12.3.6.4 then deviates from the Token Status List specification: the type
 * shall be {@code application/identifierlist+cwt}, the StatusList claim shall <em>not</em> be
 * present, and the IdentifierList structure shall be present at CWT claim key 65530, following
 * the CDDL {@code IdentifierList = { "identifiers" : { * Identifier => IdentifierInfo }, ?
 * "aggregation_uri" : Aggregation_uri, * tstr => RFU }} with {@code Identifier = bstr}.
 */
public class ValidateMdocRevocationListCwtFormat extends AbstractRevocationListCwtCondition {

	@Override
	@PreEnvironment(strings = { ENV_TOKEN, ENV_URI, ENV_MECHANISM })
	public Environment evaluate(Environment env) {
		ParsedRevocationListCwt parsed = parseRevocationListCwt(env);
		String uri = env.getString(ENV_URI);
		Mechanism mechanism = getMechanism(env);

		List<String> violations = new ArrayList<>();

		CwtEnvelope envelope = checkSharedEnvelope(parsed, violations);
		checkType(parsed.coseSign1().getProtectedHeaders(), mechanism, violations);
		checkSharedClaims(parsed.claims(), uri, mechanism, violations);

		Integer identifierCount = null;
		switch (mechanism) {
			case STATUS_LIST -> checkStatusListClaim(parsed.claims(), violations);
			case IDENTIFIER_LIST -> identifierCount = checkIdentifierListClaims(parsed.claims(), violations);
		}

		if (!violations.isEmpty()) {
			throw error("The MSO revocation list is not a valid " + describe(mechanism) + ": "
					+ String.join("; ", violations),
				args("violations", violations, "uri", uri));
		}

		logSuccess("The MSO revocation list is a valid " + describe(mechanism),
			args("algorithm", envelope.algorithm(),
				"uri", uri,
				"identifiers", identifierCount,
				"chain_length", envelope.chainLength()));
		return env;
	}

	private static String describe(Mechanism mechanism) {
		return switch (mechanism) {
			case STATUS_LIST -> "Status List Token in CWT format";
			case IDENTIFIER_LIST -> "identifier list in CWT format";
		};
	}

	private void checkStatusListClaim(DataItem claims, List<String> violations) {
		DataItem statusList = getClaim(claims, StatusListCwt.CLAIM_STATUS_LIST);
		if (statusList == null) {
			violations.add("the CWT claims set does not contain the status_list claim (key 65533)");
			return;
		}

		DataItem bits = statusList.getOrNull("bits");
		if (bits == null) {
			violations.add("the status_list claim does not contain the required 'bits' element");
		} else {
			try {
				if (bits.getAsNumber() != 1) {
					violations.add("the status_list claim's 'bits' element is " + bits.getAsNumber()
						+ "; ISO/IEC 18013-5 12.3.6.5 requires it to be 1");
				}
			} catch (Exception e) {
				violations.add("the status_list claim's 'bits' element is not a number");
			}
		}

		DataItem lst = statusList.getOrNull("lst");
		if (lst == null) {
			violations.add("the status_list claim does not contain the required 'lst' element");
		} else if (!(lst instanceof Bstr)) {
			violations.add("the status_list claim's 'lst' element is not a CBOR byte string;"
				+ " draft-ietf-oauth-status-list section 5.2 requires the compressed byte array");
		}

		checkAggregationUri(statusList, "status_list", violations);
	}

	/** @return the number of entries in the identifiers map, or 0 when it could not be read */
	private int checkIdentifierListClaims(DataItem claims, List<String> violations) {
		if (getClaim(claims, StatusListCwt.CLAIM_STATUS_LIST) != null) {
			violations.add("the CWT claims set contains the StatusList claim (key 65533);"
				+ " ISO/IEC 18013-5 12.3.6.4 says it shall not be present in an identifier list");
		}

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

		checkAggregationUri(identifierList, "IdentifierList", violations);

		return identifiersMap.getItems().size();
	}

	private void checkAggregationUri(DataItem claim, String claimName, List<String> violations) {
		DataItem aggregationUri = claim.getOrNull("aggregation_uri");
		if (aggregationUri != null && !(aggregationUri instanceof Tstr)) {
			violations.add("the " + claimName + " claim's optional 'aggregation_uri' element is not a"
				+ " CBOR text string");
		}
	}
}
