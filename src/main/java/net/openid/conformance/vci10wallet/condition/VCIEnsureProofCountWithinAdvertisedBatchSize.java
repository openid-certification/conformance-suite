package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks the wallet did not send more proofs than the batch size the emulated issuer
 * advertises. OID4VCI 1.0 Final §12.2.4: "batch_size: REQUIRED. Integer value specifying
 * the maximum array size for the proofs parameter in a Credential Request."
 */
public class VCIEnsureProofCountWithinAdvertisedBatchSize extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"proof_jwts", "credential_issuer_metadata"})
	public Environment evaluate(Environment env) {

		JsonArray items = env.getObject("proof_jwts").getAsJsonArray("items");

		JsonElement batchSizeEl = env.getElementFromObject("credential_issuer_metadata",
			"batch_credential_issuance.batch_size");
		if (batchSizeEl == null) {
			throw error("The emulated issuer metadata does not advertise 'batch_credential_issuance'");
		}
		int batchSize = OIDFJSON.getInt(batchSizeEl);

		if (items.size() > batchSize) {
			throw error("The wallet sent more proofs in the credential request than the advertised batch_size allows",
				args("proofs_sent", items.size(), "batch_size", batchSize));
		}

		logSuccess("The number of proofs sent is within the advertised batch_size",
			args("proofs_sent", items.size(), "batch_size", batchSize));

		return env;
	}
}
