package net.openid.conformance.vci10issuer.condition;

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
			String authorizationServerMetadataUrl = createAuthorizationServerMetadataUrl(credentialIssuer);
			log("Derived authorization server metadata endpoint URL from credential issuer.", args("credential_issuer", credentialIssuer, "authorization_server_metadata_url", authorizationServerMetadataUrl));
			JsonObject authorizationServerMetadataResponse = tryFetchAuthorizationServerMetadataFromUrl(0, env, credentialIssuer, authorizationServerMetadataUrl);

			env.putString("vci", "authorization_servers.count", "1");
			JsonObject authorizationServerMetadata = JsonParser.parseString(OIDFJSON.getString(authorizationServerMetadataResponse.get("body"))).getAsJsonObject();
			env.putObject("vci", "authorization_servers.server0.authorization_server_metadata", authorizationServerMetadata);

			logSuccess("Fetched authorization server metadata (derived from credential issuer)", args("credential_issuer", credentialIssuer, "authorization_server_metadata", authorizationServerMetadata));
			return env;
		}

		if (!authorizationServersEL.isJsonArray()) {
			throw error("Expected authorization_servers field to be an array", args("authorization_servers", authorizationServersEL));
		}

		// use given oauth server issuer uris to derive metadata
		JsonArray authorizationServerMetadataDataList = new JsonArray();
		JsonArray authorizationServerArray = authorizationServersEL.getAsJsonArray();

		log(String.format("Found explicit authorization_servers list with %d entries.", authorizationServerArray.size()), args("authorization_servers", authorizationServerArray));

		int i = 0;
		for (var element : authorizationServerArray) {
			String authorizationServerIssuer = OIDFJSON.getString(element);
			String authorizationServerMetadataUrl = createAuthorizationServerMetadataUrl(authorizationServerIssuer);
			log(String.format("Derived authorization server %d metadata endpoint URL from OAuth authorization server issuer.", i), args("authorization_server_issuer", authorizationServerIssuer, "authorization_server_metadata_url", authorizationServerMetadataUrl));
			JsonObject authorizationServerMetadataResponse = tryFetchAuthorizationServerMetadataFromUrl(i, env, authorizationServerIssuer, authorizationServerMetadataUrl);
			JsonObject authorizationServerMetadata = JsonParser.parseString(OIDFJSON.getString(authorizationServerMetadataResponse.get("body"))).getAsJsonObject();
			authorizationServerMetadataDataList.add(authorizationServerMetadata);
			env.putObject("vci", "authorization_servers.server" + i + ".authorization_server_metadata", authorizationServerMetadata);
			i++;
		}

		logSuccess("Fetched authorization server metadata from multiple servers", args("authorization_servers", authorizationServerArray, "authorization_server_metadata_list", authorizationServerMetadataDataList));

		return env;
	}

	protected JsonObject tryFetchAuthorizationServerMetadataFromUrl(int authServerIndex, Environment env, String authorizationServerIssuer, String authorizationServerMetadataEndpointUrl) {

		log("Fetching metadata from authorization server: " + authServerIndex, args("authorization_server_issuer", authorizationServerIssuer, "authorization_server_metadata_url", authorizationServerMetadataEndpointUrl));
		JsonObject authorizationServerMetadataResponse = fetchAuthorizationServerMetadata(env, authorizationServerMetadataEndpointUrl);
		log("Fetched metadata from authorization server: " + authServerIndex, args("authorization_server_issuer", authorizationServerIssuer, "authorization_server_metadata_url", authorizationServerMetadataEndpointUrl));
		return authorizationServerMetadataResponse;
	}

	protected String createAuthorizationServerMetadataUrl(String authServerIssuer) {

		URI authServerIssuerUri = URI.create(authServerIssuer);
		String authority = authServerIssuerUri.getScheme() + "://" + authServerIssuerUri.getHost();
		String path = authServerIssuerUri.getPath();
		int port = authServerIssuerUri.getPort();
		if (port != -1) {
			authority += ":" + port;
		}

		if (path == null || path.isEmpty() || "/".equals(path)) {
			// see: https://datatracker.ietf.org/doc/html/rfc8414#section-3.1
			//  GET /.well-known/oauth-authorization-server HTTP/1.1
			return authServerIssuer + "/.well-known/oauth-authorization-server";
		}

		// with path components after the issuer hostname, we need to apply a different rule
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
		} catch (Exception e) {
			String msg = "Unable to fetch authorization server metadata from " + metadataEndpointUrl;
			if (e.getCause() != null) {
				msg += " - " + e.getCause().getMessage();
			}
			throw error(msg, e);
		}
	}

}
