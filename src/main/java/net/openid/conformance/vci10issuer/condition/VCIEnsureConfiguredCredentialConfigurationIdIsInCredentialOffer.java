package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCIEnsureConfiguredCredentialConfigurationIdIsInCredentialOffer extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci", strings = "vci_credential_configuration_id")
	public Environment evaluate(Environment env) {

		String configuredId = env.getString("vci_credential_configuration_id");
		JsonElement offeredIdsEl = env.getElementFromObject("vci", "credential_offer.credential_configuration_ids");
		if (offeredIdsEl == null || !offeredIdsEl.isJsonArray()) {
			throw error("credential_configuration_ids in the credential offer is missing or not an array",
				args("credential_offer", env.getElementFromObject("vci", "credential_offer")));
		}
		JsonArray offeredIds = offeredIdsEl.getAsJsonArray();

		for (JsonElement offeredId : offeredIds) {
			if (offeredId.isJsonPrimitive() && configuredId.equals(OIDFJSON.getString(offeredId))) {
				logSuccess("The 'Credential Configuration ID' in the test configuration is one of the credential_configuration_ids in the credential offer",
					args("credential_configuration_id", configuredId, "credential_configuration_ids", offeredIds));
				return env;
			}
		}

		throw error("The 'Credential Configuration ID' in the test configuration is not one of the credential_configuration_ids in the credential offer. "
				+ "Either change it in the test configuration to one of the offered ids, or have the issuer offer that credential.",
			args("credential_configuration_id", configuredId, "credential_configuration_ids", offeredIds));
	}
}
