package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIUseCredentialConfigurationIdInCredentialRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci_credential_request_object", strings = "vci_credential_configuration_id")
	@PostEnvironment(required = "vci_credential_request_object")
	public Environment evaluate(Environment env) {
		JsonObject credentialRequestObject = env.getObject("vci_credential_request_object");
		String credentialConfigurationId = env.getString("vci_credential_configuration_id");

		credentialRequestObject.remove("credential_configuration_id");
		credentialRequestObject.remove("credential_identifier");
		credentialRequestObject.remove("credential_identifiers");
		credentialRequestObject.addProperty("credential_configuration_id", credentialConfigurationId);

		logSuccess("Updated credential request to use credential_configuration_id",
			args("credential_configuration_id", credentialConfigurationId,
				"credential_request_object", credentialRequestObject));

		return env;
	}
}
