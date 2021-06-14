package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddTlsClientAuthSubjectDnToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request", "certificate_subject" })
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		String subjectDn = env.getString("certificate_subject", "subjectdn");
		if (Strings.isNullOrEmpty(subjectDn)) {
			throw error("'subjectdn' not found in TLS certificate");
		}

		dynamicRegistrationRequest.addProperty("tls_client_auth_subject_dn", subjectDn);

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added tls_client_auth_subject_dn to dynamic registration request",
			args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}

}
