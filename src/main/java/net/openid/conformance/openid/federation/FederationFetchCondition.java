package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

public class FederationFetchCondition extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String primaryIss = OIDFJSON.getString(env.getElementFromObject("primary_entity_statement_body", "iss"));
		String primarySub = OIDFJSON.getString(env.getElementFromObject("primary_entity_statement_body", "sub"));
		String fetchEndpoint = OIDFJSON.getString(env.getElementFromObject("entity_statement_body", "metadata.federation_entity.federation_fetch_endpoint"));
		String fetchEndpointUrl = UriComponentsBuilder.fromHttpUrl(fetchEndpoint).queryParam("sub", primaryIss).toUriString();

		try {
			RestTemplate restTemplate = createRestTemplate(env);
			ResponseEntity<String> response = restTemplate.exchange(fetchEndpointUrl, HttpMethod.GET, null, String.class);
			JsonObject responseInfo = convertResponseForEnvironment("federation_fetch_endpoint", response);
			env.putObject("federation_fetch_endpoint_response", responseInfo);
			String jwtString = response.getBody();
			String s = "";
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Unable to build trust chain for %s since an entity statement could not be retrieved from %s".formatted(primaryIss, fetchEndpointUrl);
			if (e.getCause() != null) {
				msg += " - " + e.getCause().getMessage();
			}
			throw error(msg, e);
		}

		return env;

	}

}
