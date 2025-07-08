package net.openid.conformance.vciid2issuer.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.TLSTestValueExtractor;
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
import java.net.MalformedURLException;
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
		env.putString("vci","credential_issuer_metadata_url", metadataEndpointUrl);

		String credentialIssuerMetadataJson = fetchCredentialIssuerMetadata(env, metadataEndpointUrl);

		if (Strings.isNullOrEmpty(credentialIssuerMetadataJson)) {
			throw error("empty vci configuration configuration");
		}

		try {
			JsonObject credentialIssuerMetadata = JsonParser.parseString(credentialIssuerMetadataJson).getAsJsonObject();
			logSuccess("Successfully parsed credential issuer metadata", credentialIssuerMetadata);
			env.putObject("vci","credential_issuer_metadata", credentialIssuerMetadata);

			String issuerUrl = OIDFJSON.getString(credentialIssuerMetadata.get("credential_issuer"));
			env.putString("vci", "credential_issuer", issuerUrl);
			try {
				env.putObject("tls", TLSTestValueExtractor.extractTlsFromUrl(issuerUrl));
			} catch (MalformedURLException e) {
				throw error("Failed to parse URL", e, args("url", issuerUrl));
			}

			return env;
		} catch (JsonSyntaxException e) {
			throw error(e, args("json", credentialIssuerMetadataJson));
		}
	}

	protected String fetchCredentialIssuerMetadata(Environment env, String metadataEndpointUrl) {
		// fetch the value
		String credentialIssuerMetadataJson;
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
			headers.add(HttpHeaders.ACCEPT_LANGUAGE, "en, en-gb;q=0.9, de;q=0.8, fr;q=0.7, *;q=0.5");
			HttpEntity<?> requestEntity = new HttpEntity<>(headers);
			ResponseEntity<String> response = restTemplate.exchange(metadataEndpointUrl, HttpMethod.GET, requestEntity, String.class);
			JsonObject responseInfo = convertResponseForEnvironment("credential-issuer-metadata", response);

			env.putObject("credential_issuer_metadata_endpoint_response", responseInfo);

			credentialIssuerMetadataJson = response.getBody();
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
		return credentialIssuerMetadataJson;
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

		String iss = env.getString("config", "server.discoveryIssuer");

		if (Strings.isNullOrEmpty(iss)) {
			throw error("Couldn't find server.discoveryIssuer field for discovery purposes");
		}

		if (!iss.endsWith("/")) {
			iss += "/";
		}

		return iss + WELL_KNOWN_CREDENTIAL_ISSUER_METADATA_PATH;
	}

}
