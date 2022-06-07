package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddTlsClientAuthSubjectDnToClientConfigurationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "registration_client_endpoint_request_body", "certificate_subject" })
	public Environment evaluate(Environment env) {

		JsonObject request = env.getObject("registration_client_endpoint_request_body");

		String subjectDn = env.getString("certificate_subject", "subjectdn");
		if (Strings.isNullOrEmpty(subjectDn)) {
			throw error("'subjectdn' not found in TLS certificate");
		}

		request.addProperty("tls_client_auth_subject_dn", subjectDn);

		log("Added tls_client_auth_subject_dn to client configuration request",
			args("registration_client_endpoint_request_body", request));

		return env;
	}

}
