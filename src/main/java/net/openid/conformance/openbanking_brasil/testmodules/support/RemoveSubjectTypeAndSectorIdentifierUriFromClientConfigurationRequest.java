package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class RemoveSubjectTypeAndSectorIdentifierUriFromClientConfigurationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "registration_client_endpoint_request_body")
	public Environment evaluate(Environment env) {

		JsonObject request = env.getObject("registration_client_endpoint_request_body");
		request.remove("subject_type");
		request.remove("sector_identifier_uri");

		log("Removed subject_type and sector_identifier_uri fields from the request_body", Map.of("request_body", request));

		return env;
	}
}
