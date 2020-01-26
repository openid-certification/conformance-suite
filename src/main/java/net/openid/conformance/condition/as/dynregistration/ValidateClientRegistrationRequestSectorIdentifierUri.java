package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.HttpUtil;
import net.openid.conformance.util.validation.RedirectURIValidationUtil;

import java.net.URI;
import java.net.URISyntaxException;
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
			responseBody = HttpUtil.getAsString(sectorIdentifierUri);
			if(responseBody == null || responseBody.isEmpty()) {
				throw error("Invalid sector_identifier_uri", args("uri", sectorIdentifierUri));
			}
			JsonElement jsonElement = new JsonParser().parse(responseBody);
			JsonArray jsonArray = jsonElement.getAsJsonArray();
			JsonArray redirectUris = getRedirectUris();
			if(jsonArray == null) {
				throw error("Invalid sector_identifier_uri");
			}
			if(redirectUris == null) {
				throw error("Invalid redirect_uris");
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
					throw error("redirect_uri not found in sector_identifier_uri response",
								args("redirect_uri", redirUri, "sector_identifier_uri_response", jsonArray));
				}
			}
			logSuccess("sector_identifier_uri response validated successfully. " +
						"All uris in redirect_uris are included in sector_identifier_uri response");
			return env;

		} catch (HttpUtil.HttpUtilException ex) {
			throw error("Failed to retrieve sector_identifier_uri", ex,
						args("uri", sectorIdentifierUri, "error", ex.getCause().getMessage()));
		} catch (IllegalStateException ex) {
			throw error("sector_identifier_uri response does not contain a json array", ex,
						args("uri", sectorIdentifierUri, "response", responseBody));
		} catch (JsonSyntaxException ex) {
			throw error("sector_identifier_uri response does not contain a valid json", ex,
				args("uri", sectorIdentifierUri, "response", responseBody));
		}
	}
}
