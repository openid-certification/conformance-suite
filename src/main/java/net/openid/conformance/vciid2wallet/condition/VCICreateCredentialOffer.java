package net.openid.conformance.vciid2wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.variant.VCIGrantType;

import java.util.List;

public class VCICreateCredentialOffer extends AbstractCondition {

	private final VCIGrantType vciGrantType;

	public VCICreateCredentialOffer(VCIGrantType vciGrantType) {
		this.vciGrantType = vciGrantType;
	}

	@Override
	public Environment evaluate(Environment env) {

		String credentialIssuer = env.getString("base_url");
		String issuerState = env.getString("vci", "issuer_state");

		JsonArray credentialConfigurationIds = OIDFJSON.convertListToJsonArray(List.of("eu.europa.ec.eudi.pid.1"));

		JsonObject grantsObject = new JsonObject();

		JsonObject grantObject = new JsonObject();
		switch(vciGrantType) {
			case AUTHORIZATION_CODE -> {

				if (issuerState != null) {
					grantObject.addProperty("issuer_state", issuerState);
				}

				// TODO handle authorization_Server Optional

				grantsObject.add("authorization_code", grantObject);
			}
			case PRE_AUTHORIZATION_CODE -> {

				// TODO handle pre-authorized-code REQUIRED
				// TODO handle tx_code OPTIONAL {input_mode: optional, length: optional, description: optional}
				// TODO handle authorization_Server Optional

				grantsObject.add("urn:ietf:params:oauth:grant-type:pre-authorized_code", grantObject);
			}
		}

		JsonObject credentialOffer = new JsonObject();
		credentialOffer.addProperty("credential_issuer", credentialIssuer);
		credentialOffer.add("credential_configuration_ids", credentialConfigurationIds);
		credentialOffer.add("grants", grantsObject);

		env.putObject("vci", "credential_offer", credentialOffer);

		logSuccess("Generated credential offer", args("issuer_state", issuerState, "credential_offer", credentialOffer, "grant_type", vciGrantType));

		return env;
	}
}
