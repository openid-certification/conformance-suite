package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateAbsenceOfMetadataPolicy extends AbstractValidateMetadata {

	@Override
	@PreEnvironment(required = { "federation_response_jwt" } )
	public Environment evaluate(Environment env) {

		JsonElement metadataPolicyElement = env.getElementFromObject("federation_response_jwt", "claims.metadata_policy");

		if (metadataPolicyElement != null && !metadataPolicyElement.isJsonNull()) {
			throw error("Only subordinate statements may contain metadata_policy", args("metadata_policy", metadataPolicyElement));
		}

		logSuccess("Entity statement does not contain metadata_policy");
		return env;

	}
}
