package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Bstr;
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
 * Validates that the fetched MSO revocation list is a well formed Status List Token in CWT
 * format, as ISO/IEC 18013-5 12.3.6.3 requires:
 * <ul>
 *   <li>a COSE_Sign1 object, tagged with the COSE_Sign1 tag (18) as
 *       draft-ietf-oauth-status-list section 5.2 requires;</li>
 *   <li>signed with ES256, ES384, ES512 or EdDSA;</li>
 *   <li>carrying the x5chain in the <em>protected</em> header;</li>
 *   <li>declaring the {@code application/statuslist+cwt} type in the protected header;</li>
 *   <li>with a CWT claims set containing sub, iat, the (mandatory, per 12.3.6.3) exp claim and
 *       a status_list claim whose {@code bits} is 1 (12.3.6.5).</li>
 * </ul>
 */
public class ValidateStatusListTokenCwtFormat extends AbstractStatusListCwtCondition {

	// COSE algorithm identifiers permitted by ISO/IEC 18013-5 12.3.6.3
	private static final Map<Long, String> ALLOWED_ALGORITHMS = Map.of(
		-7L, "ES256",
		-35L, "ES384",
		-36L, "ES512",
		-8L, "EdDSA"
	);

	@Override
	@PreEnvironment(strings = { ENV_STATUS_LIST_TOKEN, ENV_STATUS_LIST_URI })
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

		DataItem claims = parsed.claims();
		checkClaims(env, claims, violations);

		if (!violations.isEmpty()) {
			throw error("The MSO revocation list is not a valid Status List Token in CWT format: "
					+ String.join("; ", violations),
				args("violations", violations, "uri", env.getString(ENV_STATUS_LIST_URI)));
		}

		logSuccess("The MSO revocation list is a valid Status List Token in CWT format",
			args("algorithm", algorithm,
				"uri", env.getString(ENV_STATUS_LIST_URI),
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

	private void checkType(Map<CoseLabel, DataItem> protectedHeaders, List<String> violations) {
		DataItem typItem = protectedHeaders.get(new CoseNumberLabel(Cose.COSE_LABEL_TYP));
		if (typItem == null) {
			violations.add("the type (COSE header label 16) is missing from the protected header;"
				+ " draft-ietf-oauth-status-list section 5.2 requires it to be present");
			return;
		}
		if (typItem instanceof Tstr typ) {
			if (!STATUS_LIST_CWT_CONTENT_TYPE.equals(typ.getValue())) {
				violations.add("the type in the protected header is '" + typ.getValue()
					+ "' rather than '" + STATUS_LIST_CWT_CONTENT_TYPE + "'");
			}
			return;
		}
		if (typItem.getMajorType() == MajorType.UNSIGNED_INTEGER) {
			// draft-ietf-oauth-status-list also allows the registered CoAP Content-Format ID
			log("The status list token's type header is a CoAP Content-Format ID rather than a media type",
				args("typ", typItem.getAsNumber()));
			return;
		}
		violations.add("the type in the protected header is neither a media type string nor a"
			+ " CoAP Content-Format ID");
	}

	private void checkClaims(Environment env, DataItem claims, List<String> violations) {
		String uri = env.getString(ENV_STATUS_LIST_URI);

		DataItem sub = getClaim(claims, CWT_CLAIM_SUB);
		if (sub == null) {
			violations.add("the CWT claims set does not contain the sub claim (key 2)");
		} else if (!(sub instanceof Tstr subject)) {
			violations.add("the sub claim (key 2) is not a text string");
		} else if (!subject.getValue().equals(uri)) {
			violations.add("the sub claim (key 2) is '" + subject.getValue()
				+ "' but the MSO's status_list element references '" + uri + "'");
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

		DataItem statusList = getClaim(claims, CWT_CLAIM_STATUS_LIST);
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

		DataItem aggregationUri = statusList.getOrNull("aggregation_uri");
		if (aggregationUri != null && !(aggregationUri instanceof Tstr)) {
			violations.add("the status_list claim's optional 'aggregation_uri' element is not a"
				+ " CBOR text string");
		}
	}
}
