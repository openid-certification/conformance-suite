package net.openid.conformance.vci10issuer.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

public class VCIGetDynamicCredentialIssuerMetadata extends AbstractCondition {

	public static final String WELL_KNOWN_CREDENTIAL_ISSUER_METADATA_PATH = ".well-known/openid-credential-issuer";

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = {"credential_issuer_metadata_endpoint_response", "vci"})
	public Environment evaluate(Environment env) {

		String metadataEndpointUrl = buildMetadataEndpointUrl(env);
		env.putString("vci", "credential_issuer_metadata_url", metadataEndpointUrl);

		fetchCredentialIssuerMetadata(env, metadataEndpointUrl);

		long status = env.getLong("credential_issuer_metadata_endpoint_response", "status");
		String contentType = env.getString("credential_issuer_metadata_endpoint_response", "headers.content-type");

		if ("application/jwt".equals(contentType)) {
			log("Credential issuer metadata response indicates signed metadata", args("contentType", contentType));
		} else {
			log("Credential issuer metadata response indicates unsigned metadata", args("contentType", contentType));
		}

		if (status != 200) {
			throw error("Could not fetch credential issuer metadata from " + metadataEndpointUrl, args("status", status, "contentType", contentType));
		}
		logSuccess("Fetched credential issuer metadata from " + metadataEndpointUrl, args("status", status, "contentType", contentType));
		return env;
	}

	protected void fetchCredentialIssuerMetadata(Environment env, String metadataEndpointUrl) {
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
			headers.add(HttpHeaders.ACCEPT, "application/json");
			headers.add(HttpHeaders.ACCEPT_LANGUAGE, "en, en-gb;q=0.9, de;q=0.8, fr;q=0.7, *;q=0.5");

			JsonElement additionalRequestHeaders = env.getElementFromObject("credential_issuer_metadata_endpoint_request", "headers");
			if (additionalRequestHeaders != null) {
				additionalRequestHeaders.getAsJsonObject()
					.entrySet()
					.forEach(entry -> headers.set(entry.getKey(), OIDFJSON.getString(entry.getValue())));
			}

			HttpEntity<?> requestEntity = new HttpEntity<>(headers);
			ResponseEntity<String> response = restTemplate.exchange(metadataEndpointUrl, HttpMethod.GET, requestEntity, String.class);
			JsonObject responseInfo = convertResponseForEnvironment("credential-issuer-metadata", response);

			env.putObject("credential_issuer_metadata_endpoint_response", responseInfo);
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Unable to fetch credential issuer metadata from " + metadataEndpointUrl;
			if (e.getCause() != null) {
				msg += " - " + e.getCause().getMessage();
			}
			throw error(msg, e);
		}
	}

	protected String buildMetadataEndpointUrl(Environment env) {

		String metadataEndpointUrl = extractMetadataEndpointUrl(env);

		if (Strings.isNullOrEmpty(metadataEndpointUrl)) {
			throw error("Couldn't find or construct a metadata endpoint URL");
		}

		log("Using credential issuer metadata endpoint", args("credential_issuer_metadata_endpoint_url", metadataEndpointUrl));

		return metadataEndpointUrl;
	}

	protected String extractMetadataEndpointUrl(Environment env) {

		String iss = env.getString("config", "vci.credential_issuer_url");

		if (Strings.isNullOrEmpty(iss)) {
			throw error("Couldn't find vci.credential_issuer_url field for discovery purposes");
		}

		URI serverIssuerUri = URI.create(iss);
		String serverIssuerPath = serverIssuerUri.getPath();

		return serverIssuerUri.getScheme() + "://" + serverIssuerUri.getAuthority() + "/" + WELL_KNOWN_CREDENTIAL_ISSUER_METADATA_PATH + serverIssuerPath;
	}

}
