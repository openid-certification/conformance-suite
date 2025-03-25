package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

public class VCIFetchOAuthorizationServerMetadata extends AbstractCondition {

	@PreEnvironment(required = {"vci"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement credentialIssuerMetadataEl = env.getElementFromObject("vci", "credential_issuer_metadata");
		JsonObject credentialIssuerMetadata = credentialIssuerMetadataEl.getAsJsonObject();

		JsonElement authorizationServersEL = credentialIssuerMetadata.get("authorization_servers");
		if (authorizationServersEL == null) {
			// derive oauth server metadata from issuer

			String credentialIssuer = OIDFJSON.getString(credentialIssuerMetadata.get("credential_issuer"));
			JsonObject authorizationServerMetadataResponse = fetchAuthorizationServerMetadataFromUrl(env, credentialIssuer);

			env.putString("vci", "authorization_servers.count", "1");
			JsonObject authorizationServerMetadata = JsonParser.parseString(OIDFJSON.getString(authorizationServerMetadataResponse.get("body"))).getAsJsonObject();
			env.putObject("vci", "authorization_servers.server0.authorization_server_metadata", authorizationServerMetadata);

			logSuccess("Fetched authorization server metadata", args("credential_issuer", credentialIssuer, "authorization_server_metadata", authorizationServerMetadata));
			return env;
		}

		if (!authorizationServersEL.isJsonArray()) {
			throw error("Expected authorization_servers field to be an array", args("authorization_servers", authorizationServersEL));
		}

		// use given oauth server issuer uris to derive metadata
		JsonArray authorizationServerMetadataDataList = new JsonArray();
		JsonArray authorizationServerArray = authorizationServersEL.getAsJsonArray();

		log("Found multiple authorization_servers", args("authorization_servers", authorizationServerArray));

		int i = 0;
		for (var element : authorizationServerArray) {
			String authorizationServerIssuer = OIDFJSON.getString(element);
			JsonObject authorizationServerMetadataResponse;
//			try {
				authorizationServerMetadataResponse = fetchAuthorizationServerMetadataFromUrl(env, authorizationServerIssuer);
//			} catch (Exception e) {
//				continue;
//			}
			JsonObject authorizationServerMetadata = JsonParser.parseString(OIDFJSON.getString(authorizationServerMetadataResponse.get("body"))).getAsJsonObject();
			authorizationServerMetadataDataList.add(authorizationServerMetadata);
			env.putObject("vci", "authorization_servers.server" + i + ".authorization_server_metadata", authorizationServerMetadata);

			i++;
		}
		env.putString("vci", "authorization_servers.count", i + "");

		logSuccess("Fetched authorization server metadata from multiple servers", args("authorization_servers", authorizationServerArray, "authorization_server_metadata_list", authorizationServerMetadataDataList));

		return env;
	}

	private JsonObject fetchAuthorizationServerMetadataFromUrl(Environment env, String authorizationServerIssuer) {
		String authorizationServerMetadataEndpointUrl = getAuthorizationServerMetadataEndpointUrl(authorizationServerIssuer);
		JsonObject authorizationServerMetadataResponse = fetchAuthorizationServerMetadata(env, authorizationServerMetadataEndpointUrl);

		log("Fetched authorization server metadata from issuer", args("authorization_server", authorizationServerIssuer, "authorization_server_metadata_url", authorizationServerMetadataEndpointUrl));
		return authorizationServerMetadataResponse;
	}

	protected String getAuthorizationServerMetadataEndpointUrl(String authServerIssuer) {

		URI authServerIssuerUri = URI.create(authServerIssuer);
		String authority = authServerIssuerUri.getScheme() + "://" + authServerIssuerUri.getHost();
		String path = authServerIssuerUri.getPath();

		if (path == null || path.isEmpty() || "/".equals(path)) {
			// see: https://datatracker.ietf.org/doc/html/rfc8414#section-3.1
			//  GET /.well-known/oauth-authorization-server HTTP/1.1
			return authServerIssuer + "/.well-known/oauth-authorization-server";
		}

		// see: https://datatracker.ietf.org/doc/html/rfc8414#section-3.1
		// GET /.well-known/oauth-authorization-server/issuer1 HTTP/1.1
		return authority + "/.well-known/oauth-authorization-server" + path;
	}

	protected JsonObject fetchAuthorizationServerMetadata(Environment env, String metadataEndpointUrl) {
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			ResponseEntity<String> response = restTemplate.exchange(metadataEndpointUrl, HttpMethod.GET, null, String.class);
			JsonObject responseInfo = convertResponseForEnvironment("oauth-authorization-server", response);
			return responseInfo;
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Unable to fetch authorization server metadata from " + metadataEndpointUrl;
			if (e.getCause() != null) {
				msg += " - " + e.getCause().getMessage();
			}
			throw error(msg, e);
		}
	}

}
