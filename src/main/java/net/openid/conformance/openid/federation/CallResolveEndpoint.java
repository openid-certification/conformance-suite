package net.openid.conformance.openid.federation;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
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

public class CallResolveEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "federation_resolve_endpoint", "expected_sub" })
	public Environment evaluate(Environment env) {

		String resolveEndpointUrl = env.getString("federation_resolve_endpoint");
		String jwtString;
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			ResponseEntity<String> response = restTemplate.exchange(resolveEndpointUrl, HttpMethod.GET, null, String.class);
			JsonObject responseInfo = convertResponseForEnvironment("Resolve response", response);
			env.putObject("resolve_endpoint_response", responseInfo);
			jwtString = response.getBody();
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Unable to invoke the resolve endpoint " + resolveEndpointUrl;
			if (e.getCause() != null) {
				msg += " - " + e.getCause().getMessage();
			}
			throw error(msg, e);
		}

		if (!Strings.isNullOrEmpty(jwtString)) {
			try {
				SignedJWT jwt = SignedJWT.parse(jwtString);
				JsonObject resolveResponseBody = JsonParser.parseString(jwt.getJWTClaimsSet().toString()).getAsJsonObject();
				JsonObject resolveResponseHeader = JsonParser.parseString(jwt.getHeader().toString()).getAsJsonObject();
				logSuccess("Successfully parsed resolve response", resolveResponseBody);
				env.putString("federation_response", jwtString);
				env.putObject("federation_response_body", resolveResponseBody);
				env.putObject("federation_response_header", resolveResponseHeader);
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
