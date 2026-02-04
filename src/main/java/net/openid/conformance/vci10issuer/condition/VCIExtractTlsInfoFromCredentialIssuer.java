package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.util.TLSTestValueExtractor;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.MalformedURLException;

public class VCIExtractTlsInfoFromCredentialIssuer extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject credentialIssuerMetadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();

		String issuerUrl = OIDFJSON.getString(credentialIssuerMetadata.get("credential_issuer"));
		env.putString("vci", "credential_issuer", issuerUrl);
		try {
			env.putObject("tls", TLSTestValueExtractor.extractTlsFromUrl(issuerUrl));
			log("Extracted TLS configuration from credential issuer URL", args("tls", env.getObject("tls")));
		} catch (MalformedURLException e) {
			throw error("Failed to parse URL", e, args("url", issuerUrl));
		}

		return env;
	}
}
