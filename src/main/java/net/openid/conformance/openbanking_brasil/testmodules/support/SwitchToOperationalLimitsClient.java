package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.PEMFormatter;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SwitchToOperationalLimitsClient extends AbstractCondition {

	private static final String HARDCODED_ALIAS = "OBB_OL";

	@Override
	@PreEnvironment(required = {"client", "config", "mutual_tls_authentication"}, strings = "redirect_uri")
	@PostEnvironment(required = {"original_client", "original_mutual_tls_authentication"}, strings = "original_redirect_uri")
	public Environment evaluate(Environment env) {
		JsonObject originalClient = env.getObject("client").deepCopy();
		JsonObject originalMtls = env.getObject("mutual_tls_authentication").deepCopy();
		env.putObject("original_client", originalClient);
		env.putObject("original_mutual_tls_authentication", originalMtls);

		//Client
		String jwksJson;
		String certString;
		String keyString;
		String caString;

		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			jwksJson = IOUtils.resourceToString("operationalLimitsTestsHardcodedClient/jwks.json", StandardCharsets.UTF_8, contextClassLoader);
			certString = IOUtils.resourceToString("operationalLimitsTestsHardcodedClient/cert.pem", StandardCharsets.UTF_8, contextClassLoader);
			keyString = IOUtils.resourceToString("operationalLimitsTestsHardcodedClient/key.pem", StandardCharsets.UTF_8, contextClassLoader);
			caString = IOUtils.resourceToString("operationalLimitsTestsHardcodedClient/ca.pem", StandardCharsets.UTF_8, contextClassLoader);


		} catch (IOException e) {
			throw error("Could not load client credentials", e);
		}

		JsonObject operationalLimitsClient = new JsonObject();
		Gson gson = JsonUtils.createBigDecimalAwareGson();
		JsonObject jwks = gson.fromJson(jwksJson, JsonObject.class);
		operationalLimitsClient.add("jwks", jwks);
		operationalLimitsClient.add("org_jwks", jwks);
		operationalLimitsClient.addProperty("client_id", env.getString("config", "client.client_id_operational_limits"));
		operationalLimitsClient.add("scope", originalClient.get("scope"));

		env.putObject("client", operationalLimitsClient);

		//MTLS

		try {
			certString = PEMFormatter.stripPEM(certString);
			keyString = PEMFormatter.stripPEM(keyString);
			caString = PEMFormatter.stripPEM(caString);

		} catch (IllegalArgumentException e) {
			throw error("Couldn't decode Operational Limits certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		}

		JsonObject operationalLimitsMtls = new JsonObject();

		operationalLimitsMtls.addProperty("cert", certString);
		operationalLimitsMtls.addProperty("key", keyString);
		operationalLimitsMtls.addProperty("ca", caString);


		env.putObject("mutual_tls_authentication", operationalLimitsMtls);

		// Redirect URI

		String originalRedirectUri = env.getString("redirect_uri");
		String alias = env.getString("config", "alias");
		env.putString("original_redirect_uri", originalRedirectUri);
		String operationalLimitsRedirectUri = originalRedirectUri.replaceAll(alias, HARDCODED_ALIAS);
		env.putString("redirect_uri", operationalLimitsRedirectUri);


		logSuccess("Switched to hardcoded Operational Limits Client",
			args("Current Client", operationalLimitsClient, "Current MTLS", operationalLimitsMtls, "Current Redirect URI", operationalLimitsRedirectUri,
				"Original client", originalClient, "Original MTLS", originalMtls, "Original Redirect URI", originalRedirectUri));

		return env;
	}
}
