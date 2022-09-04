package net.openid.conformance.condition.as.dynregistration;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * if supporting tls_client_auth client authentication mechanism as defined in RFC8705 shall only accept
 * tls_client_auth_subject_dn as an indication of the certificate subject value as defined in clause 2.1.2 RFC8705;
 */
public class FAPIBrazilEnsureTlsClientAuthSubjectDnOnly extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request"})
	public Environment evaluate(Environment env) {
		//tls_client_auth, self_signed_tls_client_auth, private_key_jwt
		String authMethod = env.getString("dynamic_registration_request", "token_endpoint_auth_method");

		if("tls_client_auth".equals(authMethod)) {
			checkElement(env, "tls_client_auth_san_dns");
			checkElement(env, "tls_client_auth_san_uri");
			checkElement(env, "tls_client_auth_san_ip");
			checkElement(env, "tls_client_auth_san_email");
			String dn = env.getString("dynamic_registration_request", "tls_client_auth_subject_dn");
			if(Strings.isNullOrEmpty(dn)) {
				throw error("Registration request does not contain tls_client_auth_subject_dn");
			}
			env.putString("registered_tls_client_auth_subject_dn", dn);
			logSuccess("Registration request does not contain any of the disallowed tls_client_auth metadata" +
				" (tls_client_auth_san_dns, tls_client_auth_san_uri, tls_client_auth_san_ip, tls_client_auth_san_email)",
				args("tls_client_auth_subject_dn", dn));
			return env;
		} else {
			log("token_endpoint_auth_method is not tls_client_auth");
			return env;
		}
	}

	protected void checkElement(Environment env, String elementName) {
		String element = env.getString("dynamic_registration_request", elementName);
		if(element!=null) {
			throw error("dynamic_registration_request contains "+elementName+" which is not allowed. " +
					"Only tls_client_auth_subject_dn is allowed.",
				args(elementName, element));
		}
	}
}
