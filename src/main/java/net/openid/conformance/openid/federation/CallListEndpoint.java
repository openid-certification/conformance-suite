package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
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

public class CallListEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "federation_list_endpoint")
	@PostEnvironment(required = { "federation_list_endpoint_response" } )
	public Environment evaluate(Environment env) {

		String listEndpoint = env.getString("federation_list_endpoint");
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			ResponseEntity<String> response = restTemplate.exchange(listEndpoint, HttpMethod.GET, null, String.class);
			JsonObject responseInfo = convertResponseForEnvironment("federation_list_endpoint_response", response);
			env.putObject("federation_list_endpoint_response", responseInfo);
			logSuccess("Successfully got a response from federation_list_endpoint", args("federation_list_endpoint_response", responseInfo));
			return env;
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Unable to fetch subordinate listing from " + listEndpoint;
			if (e.getCause() != null) {
				msg += " - " + e.getCause().getMessage();
			}
			throw error(msg, e);
		}

	}

}
