package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Used when 'Credential Configuration ID' is not set in the test configuration: requests the
 * credential configuration the issuer offered, which is only unambiguous when it offered one.
 */
public class VCIUseCredentialConfigurationIdFromCredentialOffer extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	@PostEnvironment(strings = "vci_credential_configuration_id")
	public Environment evaluate(Environment env) {

		JsonElement offeredIdsEl = env.getElementFromObject("vci", "credential_offer.credential_configuration_ids");
		if (offeredIdsEl == null || !offeredIdsEl.isJsonArray()) {
			throw error("credential_configuration_ids in the credential offer is missing or not an array",
				args("credential_offer", env.getElementFromObject("vci", "credential_offer")));
		}
		JsonArray offeredIds = offeredIdsEl.getAsJsonArray();

		if (offeredIds.size() != 1 || !offeredIds.get(0).isJsonPrimitive()) {
			throw error("'Credential Configuration ID' is not set in the test configuration, and the credential offer does not "
					+ "contain exactly one credential_configuration_id to use instead. Set 'Credential Configuration ID' in the "
					+ "test configuration to the one to request.",
				args("credential_configuration_ids", offeredIds));
		}

		String credentialConfigurationId = OIDFJSON.getString(offeredIds.get(0));
		env.putString("vci_credential_configuration_id", credentialConfigurationId);

		logSuccess("Using the credential_configuration_id from the credential offer, as 'Credential Configuration ID' is not set in the test configuration",
			args("credential_configuration_id", credentialConfigurationId));
		return env;
	}
}
