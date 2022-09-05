package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractFAPIBrazilExtractCertificateSubject;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate extends AbstractFAPIBrazilExtractCertificateSubject
{

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(required = "client_certificate_subject")
	public Environment evaluate(Environment env) {
		String certString = env.getString("incoming_request", "headers.x-ssl-cert");
		certString = certString.replaceAll("-----BEGIN CERTIFICATE-----", "");
		certString = certString.replaceAll("-----END CERTIFICATE-----", "");
		certString = certString.replaceAll("\\s", "");

		if (Strings.isNullOrEmpty(certString)) {
			throw error("Couldn't find client certificate in headers. (See last incoming request details)");
		}
		JsonObject certificateSubject = extractSubject(certString, "Client certificate must be a BRCAC profile certificate where the subjectdn contains a UID");

		env.putObject("client_certificate_subject", certificateSubject);

		logSuccess("Extracted subject from the mtls client certificate", certificateSubject);

		return env;
	}

}
