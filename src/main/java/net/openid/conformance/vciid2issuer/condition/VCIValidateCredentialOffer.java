package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vciid2issuer.util.JsonSchemaValidationInput;

public class VCIValidateCredentialOffer extends AbstractJsonSchemaBasedValidation {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject metadata = env.getElementFromObject("vci", "credential_offer").getAsJsonObject();
		String schemaResource = "json-schemas/oid4vci/credential_offer_ID2_15.json";
		String metadataName = "OID4VCI Credential Offer";
		return new JsonSchemaValidationInput(metadataName, schemaResource, metadata);
	}

	@Override
	public Environment evaluate(Environment env) {

		String credentialOfferJsonString = env.getString("vci","credential_offer_raw");
		logSuccess("Found credential offer", args("credential_offer", credentialOfferJsonString));

		try {
			JsonObject credentialOfferObject = JsonParser.parseString(credentialOfferJsonString).getAsJsonObject();
			String credentialIssuer = OIDFJSON.getString(credentialOfferObject.get("credential_issuer"));

			JsonArray credentialConfigurationIds = credentialOfferObject.get("credential_configuration_ids").getAsJsonArray();
			env.putObject("vci","credential_offer", credentialOfferObject);

			var resultEnv = super.evaluate(env);

			logSuccess("Parsed credential offer",
				args("credential_offer", credentialOfferObject, "credential_issuer", credentialIssuer,
					"credential_configuration_ids", credentialConfigurationIds));

			return resultEnv;
		} catch (Exception ex) {
			throw error("Failed to parse credential_offer parameter", args("error", ex.getMessage()));
		}
	}
}
