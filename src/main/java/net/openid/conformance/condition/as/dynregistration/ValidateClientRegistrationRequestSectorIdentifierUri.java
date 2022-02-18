package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashSet;
import java.util.Set;

/**
 * sector_identifier_uri
 * OPTIONAL. URL using the https scheme to be used in calculating Pseudonymous Identifiers by the OP.
 * The URL references a file with a single JSON array of redirect_uri values. Please see Section 5.
 * Providers that use pairwise sub (subject) values SHOULD utilize the sector_identifier_uri value
 * provided in the Subject Identifier calculation for pairwise identifiers.
 *
 * This class should only be used for registration requests:
 * https://openid.net/specs/openid-connect-registration-1_0.html#SectorIdentifierValidation
 * The values registered in redirect_uris MUST be included in the elements of the array,
 * or registration MUST fail. This MUST be validated at registration time;
 * there is no requirement for the OP to retain the contents of this JSON file or
 * to retrieve or revalidate its contents in the future.
 */
public class ValidateClientRegistrationRequestSectorIdentifierUri extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "dynamic_registration_request"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("dynamic_registration_request");

		String sectorIdentifierUri = getSectorIdentifierUri();
		if(sectorIdentifierUri==null) {
			logSuccess("A sector_identifier_uri was not provided");
			return env;
		}
		if(!sectorIdentifierUri.toLowerCase().startsWith("https://")) {
			throw error("sector_identifier_uri MUST be a URL using the https scheme",
						args("uri", sectorIdentifierUri));
		}
		String responseBody = "";
		try {
			RestTemplate restTemplate = createRestTemplate(env, false);
			responseBody = restTemplate.getForObject(sectorIdentifierUri, String.class);
			if(responseBody == null || responseBody.isEmpty()) {
				throw error("Invalid sector_identifier_uri. When fetching sector_identifier_uri " +
							"the server returned an empty body", args("uri", sectorIdentifierUri));
			}
			JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
			JsonArray redirectUris = null;
			try {
				redirectUris = getRedirectUris();
				if(redirectUris == null) {
					throw error("redirect_uris is empty");
				}
			} catch(IllegalStateException ex) {
				throw error("redirect_uris is not encoded as a json array");
			}

			Set<String> urisFromSectorIdentifierUri = new HashSet<>();
			Set<String> urisFromRedirectUris = new HashSet<>();
			for(JsonElement element : jsonArray) {
				urisFromSectorIdentifierUri.add(OIDFJSON.getString(element));
			}
			for(JsonElement element : redirectUris) {
				urisFromRedirectUris.add(OIDFJSON.getString(element));
			}
			//The values registered in redirect_uris MUST be included in the elements of the array,
			//or registration MUST fail. This MUST be validated at registration time;
			//there is no requirement for the OP to retain the contents of this JSON file or
			//to retrieve or revalidate its contents in the future.
			for(String redirUri : urisFromRedirectUris) {
				if(!urisFromSectorIdentifierUri.contains(redirUri)) {
					throw error("A redirect_uri provided in registration request is not found in " +
									"sector_identifier_uri response",
								args("redirect_uri", redirUri, "sector_identifier_uri_response", jsonArray));
				}
			}
			logSuccess("sector_identifier_uri response validated successfully. " +
						"All uris in redirect_uris are included in sector_identifier_uri response");
			return env;

		} catch (RestClientException ex) {
			throw error("Failed to retrieve sector_identifier_uri", ex,
						args("uri", sectorIdentifierUri, "error", ex.getMessage()));
		} catch (IllegalStateException ex) {
			throw error("sector_identifier_uri response does not contain a json array", ex,
						args("uri", sectorIdentifierUri, "response", responseBody));
		} catch (JsonSyntaxException ex) {
			throw error("sector_identifier_uri response does not contain a valid json", ex,
				args("uri", sectorIdentifierUri, "response", responseBody));
		} catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException |
				KeyStoreException | InvalidKeySpecException | KeyManagementException e) {
			throw error("Error creating http client", e);
		}

}
}
