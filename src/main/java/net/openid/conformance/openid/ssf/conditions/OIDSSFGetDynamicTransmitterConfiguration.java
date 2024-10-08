package net.openid.conformance.openid.ssf.conditions;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
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

public class OIDSSFGetDynamicTransmitterConfiguration extends AbstractCondition {

	public static final String WELL_KNOWN_SSF_CONFIGURATION_PATH = ".well-known/ssf-configuration";

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = {"transmitter_metadata_endpoint_response", "transmitter_metadata"})
	public Environment evaluate(Environment env) {

		String metadataEndpointUrl = buildMetadataEndpointUrl(env);

		String transmitterMetadataJson = fetchTransmitterMetadata(env, metadataEndpointUrl);

		if (Strings.isNullOrEmpty(transmitterMetadataJson)) {
			throw error("empty ssf configuration configuration");
		}

		try {
			JsonObject transmitterMetadata = JsonParser.parseString(transmitterMetadataJson).getAsJsonObject();
			logSuccess("Successfully parsed transmitter metadata", transmitterMetadata);
			env.putObject("transmitter_metadata", transmitterMetadata);
			return env;
		} catch (JsonSyntaxException e) {
			throw error(e, args("json", transmitterMetadataJson));
		}
	}

	String fetchTransmitterMetadata(Environment env, String metadataEndpointUrl) {
		// fetch the value
		String transmitterMetadataJson;
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			ResponseEntity<String> response = restTemplate.exchange(metadataEndpointUrl, HttpMethod.GET, null, String.class);
			JsonObject responseInfo = convertResponseForEnvironment("ssf-configuration", response);

			env.putObject("transmitter_metadata_endpoint_response", responseInfo);

			transmitterMetadataJson = response.getBody();
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Unable to fetch server configuration from " + metadataEndpointUrl;
			if (e.getCause() != null) {
				msg += " - " + e.getCause().getMessage();
			}
			throw error(msg, e);
		}
		return transmitterMetadataJson;
	}

	String buildMetadataEndpointUrl(Environment env) {

		String rawMetadataEndpointUrl = extractMetadataEndpointUrl(env);

		if (Strings.isNullOrEmpty(rawMetadataEndpointUrl)) {
			throw error("Couldn't find or construct a metadata endpoint URL");
		}

		String metadataSuffix = env.getString("config", "ssf.transmitter.metadata_suffix");

		String effectiveMetadataEndpointUrl = getEffectiveMetadataEndpointUrl(rawMetadataEndpointUrl, metadataSuffix);

		log("Derived effective metadata endpoint", args("effective_metadata_endpoint_url", effectiveMetadataEndpointUrl, "metadata_endpoint_url", rawMetadataEndpointUrl, "metadata_suffix", metadataSuffix));

		return effectiveMetadataEndpointUrl;
	}

	String getEffectiveMetadataEndpointUrl(String metadataEndpointUrl, String metadataSuffix) {

		if (metadataSuffix == null) {
			return metadataEndpointUrl;
		}

		String trimmedMetadataSuffix = metadataSuffix.trim();
		if (trimmedMetadataSuffix.isBlank()) {
			return metadataEndpointUrl;
		}

		if (!trimmedMetadataSuffix.startsWith("/")) {
			trimmedMetadataSuffix = "/" + trimmedMetadataSuffix;
		}

		return metadataEndpointUrl + trimmedMetadataSuffix;
	}

	String extractMetadataEndpointUrl(Environment env) {

		String iss = env.getString("config", "ssf.transmitter.issuer");
		if (!iss.endsWith("/")) {
			iss += "/";
		}
		if (Strings.isNullOrEmpty(iss)) {
			throw error("Couldn't find ssf.transmitter.issuer field for discovery purposes");
		}

		return iss + WELL_KNOWN_SSF_CONFIGURATION_PATH;
	}

}
