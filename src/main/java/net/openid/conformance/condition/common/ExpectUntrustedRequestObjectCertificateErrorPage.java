package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectUntrustedRequestObjectCertificateErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "untrusted_request_object_certificate_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder(
			"The request object is signed with a certificate that does not chain to a trusted certificate authority. "
				+ "The wallet should reject the request and display an error indicating the verifier could not be authenticated.");
		env.putString("untrusted_request_object_certificate_error", placeholder);

		return env;
	}
}
