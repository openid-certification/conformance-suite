package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIUseCredentialIdentifierInCredentialRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci_credential_request_object", strings = "vci_credential_identifier")
	@PostEnvironment(required = "vci_credential_request_object")
	public Environment evaluate(Environment env) {
		JsonObject credentialRequestObject = env.getObject("vci_credential_request_object");
		String credentialIdentifier = env.getString("vci_credential_identifier");

		credentialRequestObject.remove("credential_configuration_id");
		credentialRequestObject.remove("credential_identifier");
		credentialRequestObject.remove("credential_identifiers");
		credentialRequestObject.addProperty("credential_identifier", credentialIdentifier);

		logSuccess("Updated credential request to use credential_identifier",
			args("credential_identifier", credentialIdentifier,
				"credential_request_object", credentialRequestObject));

		return env;
	}
}
