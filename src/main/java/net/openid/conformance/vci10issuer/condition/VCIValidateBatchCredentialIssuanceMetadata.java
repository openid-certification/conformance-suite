package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates the batch_credential_issuance object in the credential issuer metadata.
 *
 * OID4VCI 1.0 Final §12.2.4: "batch_size: REQUIRED. Integer value specifying the maximum
 * array size for the proofs parameter in a Credential Request. It MUST be 2 or greater."
 *
 * On success the batch size is stored in the environment as 'vci_batch_size'.
 */
public class VCIValidateBatchCredentialIssuanceMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonElement batchEl = env.getElementFromObject("vci", "credential_issuer_metadata.batch_credential_issuance");
		if (batchEl == null || !batchEl.isJsonObject()) {
			throw error("'batch_credential_issuance' is missing from the credential issuer metadata or is not a JSON object",
				args("batch_credential_issuance", batchEl));
		}
		JsonObject batchObj = batchEl.getAsJsonObject();

		JsonElement batchSizeEl = batchObj.get("batch_size");
		if (batchSizeEl == null) {
			throw error("'batch_size' is missing from 'batch_credential_issuance' in the credential issuer metadata; it is REQUIRED",
				args("batch_credential_issuance", batchObj));
		}

		if (!batchSizeEl.isJsonPrimitive() || !batchSizeEl.getAsJsonPrimitive().isNumber()) {
			throw error("'batch_size' in 'batch_credential_issuance' must be an integer",
				args("batch_size", batchSizeEl));
		}

		Number batchSizeNumber = OIDFJSON.getNumber(batchSizeEl);
		double batchSizeDouble = batchSizeNumber.doubleValue();
		if (batchSizeDouble != Math.rint(batchSizeDouble)) {
			throw error("'batch_size' in 'batch_credential_issuance' must be an integer",
				args("batch_size", batchSizeEl));
		}

		int batchSize = batchSizeNumber.intValue();
		if (batchSize < 2) {
			throw error("'batch_size' in 'batch_credential_issuance' must be 2 or greater",
				args("batch_size", batchSize));
		}

		env.putInteger("vci_batch_size", batchSize);

		logSuccess("The credential issuer supports batch credential issuance",
			args("batch_size", batchSize));

		return env;
	}
}
