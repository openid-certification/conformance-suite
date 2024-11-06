package net.openid.conformance.openid.federation;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jwt.SignedJWT;
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

public class CallFederationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "federation_endpoint_url")
	@PostEnvironment(required = { "federation_http_response", "federation_response_jwt" } )
	public Environment evaluate(Environment env) {

		String entityStatementUrl = env.getString("federation_endpoint_url");
		if (Strings.isNullOrEmpty(entityStatementUrl)) {
			throw error("Couldn't find entityStatementUrl in configuration");
		}

		String jwtString;
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			ResponseEntity<String> response = restTemplate.exchange(entityStatementUrl, HttpMethod.GET, null, String.class);
			JsonObject responseInfo = convertResponseForEnvironment("Entity statement", response);
			env.putObject("federation_http_response", responseInfo);
			jwtString = response.getBody();
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Unable to fetch entity statement from " + entityStatementUrl;
			if (e.getCause() != null) {
				msg += " - " + e.getCause().getMessage();
			}
			throw error(msg, e);
		}

		if (!Strings.isNullOrEmpty(jwtString)) {
			try {
				JsonObject jwtAsJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(jwtString);
				env.putObject("federation_response_jwt", jwtAsJsonObject);
				return env;
			} catch (ParseException e) {
				throw error("Failed to parse entity statement as a signed JWT", e, args("jwt", jwtString));
			} catch (JsonSyntaxException e) {
				throw error(e, args("jwt", jwtString));
			}
		} else {
			throw error("Empty entity statement", args("federation_response", jwtString));
		}

	}

}
