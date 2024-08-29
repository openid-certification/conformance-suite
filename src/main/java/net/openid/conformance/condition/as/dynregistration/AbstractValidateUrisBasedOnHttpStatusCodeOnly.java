package net.openid.conformance.condition.as.dynregistration;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class AbstractValidateUrisBasedOnHttpStatusCodeOnly extends AbstractClientValidationCondition
{

	protected abstract Map<String, String> getUrisToTest();
	protected abstract String getMetadataName();
	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");
		Map<String, String> clientUris = getUrisToTest();
		if(clientUris==null || clientUris.isEmpty()) {
			logSuccess("Client does not contain any " + getMetadataName());
			return env;
		}
		List<String> clientUriStatusCodes = new ArrayList<>();
		try {
			RestTemplate restTemplate = createRestTemplate(env, false);
			for (String lang : clientUris.keySet()) {
				String uri = clientUris.get(lang);
				try {
					//Please note: restTemplate will follow redirects
					ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.HEAD, null, String.class);
					//rest template will throw an exception in case of 40x
					clientUriStatusCodes.add(uri + " : " + response.getStatusCode());
				} catch (RestClientException ex) {
					appendError("failure_reason", "Error checking " + getMetadataName(),
						"details", args("uri", uri, "error_message", ex.getMessage()));
				}
			}
		} catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException
			| KeyStoreException | InvalidKeySpecException | KeyManagementException e) {
			throw error("Error creating HTTP client", e);
		}

		if(!validationErrors.isEmpty()) {
			throw error(getMetadataName() + " validation failed",
						args("errors", validationErrors, "uri_status_codes", clientUriStatusCodes));
		}
		logSuccess("Client contains valid "+getMetadataName()+" value(s)",
					args("uri_status_codes", clientUriStatusCodes));
		return env;
	}
}
