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
	@PostEnvironment(required = { "federation_http_response", "federation_response_body", "federation_response_header" } )
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
				SignedJWT jwt = SignedJWT.parse(jwtString);
				JsonObject entityStatementBody = JsonParser.parseString(jwt.getJWTClaimsSet().toString()).getAsJsonObject();
				JsonObject entityStatementHeader = JsonParser.parseString(jwt.getHeader().toString()).getAsJsonObject();
				logSuccess("Successfully parsed signed JWT", entityStatementBody);
				env.putString("federation_response", jwtString);
				env.putObject("federation_response_body", entityStatementBody);
				env.putObject("federation_response_header", entityStatementHeader);
				return env;
			} catch (ParseException e) {
				throw error("Failed to parse entity statement as a signed JWT", e, args("jwt", jwtString));
			} catch (JsonSyntaxException e) {
				throw error(e, args("json", jwtString));
			}
		} else {
			throw error("Empty entity statement", args("federation_response", jwtString));
		}

	}

}
