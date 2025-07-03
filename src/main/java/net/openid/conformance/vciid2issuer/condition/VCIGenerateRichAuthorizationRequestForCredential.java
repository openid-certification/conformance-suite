package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIGenerateRichAuthorizationRequestForCredential extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "vci"})
	public Environment evaluate(Environment env) {

		String credentialConfigId = env.getString("config", "vci.credential_configuration_id");

		JsonObject rarObj = new JsonObject();
		rarObj.addProperty("type", "openid_credential");
		rarObj.addProperty("credential_configuration_id", credentialConfigId);

		JsonElement credentialIssuerMetadata = env.getElementFromObject("vci", "credential_issuer_metadata");
		if (credentialIssuerMetadata != null) {
			JsonArray credentialIssuerLocationsArray = new JsonArray();
			String credentialIssuer = env.getString("vci", "credential_issuer");
			credentialIssuerLocationsArray.add(credentialIssuer);
			rarObj.add("locations", credentialIssuerLocationsArray);
		}

		JsonArray rarArray = new JsonArray();

		rarArray.add(rarObj);

		for (JsonElement element : rarArray) {
			if (!element.isJsonObject() || !element.getAsJsonObject().has("type")) {
				throw error("The Authorization Request JSON on test config is not a valid RAR payload");
			}
		}
		JsonObject rar = new JsonObject();
		rar.add("payload", rarArray);
		env.putObject("rar", rar);

		log("Added rich authorization details for credential", args("credential_configuration_id", credentialConfigId, "authorization_details", rarArray));

		return env;
	}
}
