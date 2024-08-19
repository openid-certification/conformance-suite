package net.openid.conformance.openid.federation;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashSet;
import java.util.Set;

public class ValidateEntityStatementMetadataPolicy extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "entity_statement_body" } )
	public Environment evaluate(Environment env) {

		JsonElement metadataPolicyClaim = env.getElementFromObject("entity_statement_body", "metadata_policy");
		if (metadataPolicyClaim == null) {
			logSuccess("Entity statement does not contain the metadata claim");
			return env;
		}

		JsonObject metadataPolicy = metadataPolicyClaim.getAsJsonObject();

		Set<String> validTopLevelKeys = ImmutableSet.of(
			"federation_entity",
			"openid_relying_party",
			"openid_provider",
			"oauth_authorization_server",
			"oauth_client",
			"oauth_resource"
		);

		/*
		validTopLevelKeys.stream().forEach(key -> {
			if (metadataPolicy.has(key)) {
				env.putObject(key, metadataPolicy.getAsJsonObject(key));
			}
		});
		*/

		Set<String> entityTypes = metadataPolicy.keySet();
		Set<String> difference = new HashSet<>(entityTypes);
		difference.removeAll(validTopLevelKeys);
		if (!difference.isEmpty()) {
			throw error("The metadata policy claim contains invalid entity types", args("expected", validTopLevelKeys, "actual", entityTypes));
		}

		for (String entityType : entityTypes) {
			JsonObject parameters = metadataPolicy.getAsJsonObject(entityType);
			for (String parameterName : parameters.keySet()) {
				JsonObject parameter = parameters.getAsJsonObject(parameterName);
				for (String operatorName : parameter.keySet()) {
					JsonElement operator = parameter.get(operatorName);
					// operator is default, add, one_of etc, validate the values
				}
			}
		}


		logSuccess("Entity statement contains a valid metadata policy claim", args("metadata", metadataPolicy));
		return env;
	}

}
