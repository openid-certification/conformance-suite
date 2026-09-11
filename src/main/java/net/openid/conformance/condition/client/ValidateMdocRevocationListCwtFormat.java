package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.StatusListCwt;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.DataItem;

import java.util.ArrayList;
import java.util.List;

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
public class ValidateMdocRevocationListCwtFormat extends AbstractRevocationListCwtCondition {

	@Override
	@PreEnvironment(strings = { ENV_TOKEN, ENV_URI })
	public Environment evaluate(Environment env) {
		ParsedRevocationListCwt parsed = parseRevocationListCwt(env);
		String uri = env.getString(ENV_URI);

		List<String> violations = new ArrayList<>();

		CwtEnvelope envelope = checkSharedEnvelope(parsed, violations);
		checkType(parsed.coseSign1().getProtectedHeaders(), violations);
		checkSharedClaims(parsed.claims(), uri, violations);
		checkStatusListClaim(parsed.claims(), violations);

		if (!violations.isEmpty()) {
			throw error("The MSO revocation list is not a valid Status List Token in CWT format: "
					+ String.join("; ", violations),
				args("violations", violations, "uri", uri));
		}

		logSuccess("The MSO revocation list is a valid Status List Token in CWT format",
			args("algorithm", envelope.algorithm(),
				"uri", uri,
				"chain_length", envelope.chainLength()));
		return env;
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
	}
}
