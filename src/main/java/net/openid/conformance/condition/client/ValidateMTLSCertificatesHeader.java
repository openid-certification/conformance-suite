package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateMTLSCertificatesHeader extends AbstractValidateMTLSCertificatesHeader {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		// mutual_tls_authentication
		String certString = env.getString("config", "mtls.cert");
		String keyString = env.getString("config", "mtls.key");
		String caString = env.getString("config", "mtls.ca");

		validateMTLSCertificatesHeader(certString, keyString, caString);

		return env;
	}
}
