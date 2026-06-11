package net.openid.conformance.vci10issuer.condition;

import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import com.google.gson.JsonArray;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks that no disclosure salt is reused across the SD-JWT credentials issued in a batch.
 *
 * SD-JWT (RFC 9901) §10.1 (Unlinkability): "New Key Binding keys and salts MUST be used
 * for each credential in the batch to ensure that the Verifiers cannot link the credentials
 * using these values." (Key binding key freshness is checked separately by
 * VCIEnsureBatchBindingKeysAreDistinct.)
 */
public class VCIEnsureBatchSdJwtDisclosureSaltsAreDistinct extends AbstractCondition {

	@Override
	@PreEnvironment(required = "extracted_credentials")
	public Environment evaluate(Environment env) {

		JsonArray list = env.getObject("extracted_credentials").getAsJsonArray("list");

		// salt -> index of the credential it was first seen in
		Map<String, Integer> seenSalts = new HashMap<>();
		int totalDisclosures = 0;

		for (int i = 0; i < list.size(); i++) {
			String sdJwtString = OIDFJSON.getString(list.get(i));
			SDJWT sdJwt;
			try {
				sdJwt = SDJWT.parse(sdJwtString);
			} catch (IllegalArgumentException e) {
				throw error("Parsing SD-JWT failed", e, args("credential_index", i, "credential", sdJwtString));
			}

			for (Disclosure disclosure : sdJwt.getDisclosures()) {
				totalDisclosures++;
				Integer firstIndex = seenSalts.putIfAbsent(disclosure.getSalt(), i);
				if (firstIndex != null && firstIndex != i) {
					throw error("Two credentials issued in the same batch contain a disclosure with the same salt; "
							+ "verifiers can use this to link the credentials, so new salts must be used for each "
							+ "credential in the batch",
						args("first_credential_index", firstIndex,
							"second_credential_index", i,
							"claim_name", disclosure.getClaimName(),
							"salt", disclosure.getSalt()));
				}
			}
		}

		if (totalDisclosures == 0) {
			logSuccess("The credentials in the batch contain no disclosures, so there are no salts that could link them");
		} else {
			logSuccess("No disclosure salt is shared between credentials in the batch",
				args("credential_count", list.size(), "disclosure_count", totalDisclosures));
		}

		return env;
	}
}
