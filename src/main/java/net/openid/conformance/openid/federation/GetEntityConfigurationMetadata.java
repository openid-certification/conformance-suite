package net.openid.conformance.openid.federation;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jwt.JWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;

public class GetEntityConfigurationMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "server", "entity_configuration_endpoint_response" } )
	public Environment evaluate(Environment env) {

		if (!env.containsObject("config")) {
			throw error("Couldn't find a configuration");
		}

		String entityConfigurationUrl = env.getString("config", "server.entityConfigurationUrl");
		if (!Strings.isNullOrEmpty(entityConfigurationUrl)) {

			String jwtString;
			try {
				RestTemplate restTemplate = createRestTemplate(env);
				ResponseEntity<String> response = restTemplate.exchange(entityConfigurationUrl, HttpMethod.GET, null, String.class);
				JsonObject responseInfo = convertResponseForEnvironment("discovery", response);
				env.putObject("entity_configuration_endpoint_response", responseInfo);
				jwtString = response.getBody();
			} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
				throw error("Error creating HTTP client", e);
			} catch (RestClientException e) {
				String msg = "Unable to fetch entity configuration from " + entityConfigurationUrl;
				if (e.getCause() != null) {
					msg += " - " +e.getCause().getMessage();
				}
				throw error(msg, e);
			}

			if (!Strings.isNullOrEmpty(jwtString)) {
				try {
					JWT jwt = JWTUtil.parseJWT(jwtString);
					JsonObject entityConfig = JsonParser.parseString(jwt.getJWTClaimsSet().toString()).getAsJsonObject();
					logSuccess("Successfully parsed entity configuration", entityConfig);
					env.putObject("server", entityConfig);
					return env;
				} catch (ParseException | JsonSyntaxException e) {
					throw error(e, args("json", jwtString));
				}
			} else {
				throw error("empty server configuration");
			}

		} else {
			throw error("Couldn't find or construct a discovery URL");
		}

	}

}
