package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
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
	@PreEnvironment(required = {"config", "credential_issuer_metadata"}, strings = "credential_configuration_id_hint")
	public Environment evaluate(Environment env) {

		String credentialIssuer = env.getString("credential_issuer_metadata", "credential_issuer");
		if (credentialIssuer == null) {
			throw error("Couldn't find credential_issuer in credential_issuer_metadata");
		}
		String issuerState = env.getString("vci", "issuer_state");

		String credentialConfigurationIdHint = env.getString("credential_configuration_id_hint");
		log("Using credential_configuration_id " + credentialConfigurationIdHint, args("credential_configuration_id", credentialConfigurationIdHint));

		JsonArray credentialConfigurationIds = OIDFJSON.convertListToJsonArray(List.of(credentialConfigurationIdHint));

		JsonObject grantsObject = new JsonObject();

		JsonObject grantObject = new JsonObject();
		switch(vciGrantType) {
			case AUTHORIZATION_CODE -> {

				if (issuerState != null) {
					grantObject.addProperty("issuer_state", issuerState);
				}

				// TODO handle optional authorization_server hint
				// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-4.1.1-5.1.2.2

				grantsObject.add("authorization_code", grantObject);
			}
			case PRE_AUTHORIZATION_CODE -> {

				String preAuthCode = env.getString("vci","pre-authorized_code");
				JsonObject txCode = env.getElementFromObject("vci", "pre-authorized_code_tx_code").getAsJsonObject();

				grantObject.addProperty("pre-authorized_code", preAuthCode);
				grantObject.add("tx_code", txCode);

				// TODO handle optional authorization_server hint
				// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-4.1.1-5.2.2.3

				grantsObject.add("urn:ietf:params:oauth:grant-type:pre-authorized_code", grantObject);
			}
		}

		JsonObject credentialOffer = new JsonObject();
		credentialOffer.addProperty("credential_issuer", credentialIssuer);
		credentialOffer.add("credential_configuration_ids", credentialConfigurationIds);
		credentialOffer.add("grants", grantsObject);

		env.putObject("vci", "credential_offer", credentialOffer);

		log("Generated credential offer", args("issuer_state", issuerState, "credential_offer", credentialOffer, "grant_type", vciGrantType));

		return env;
	}
}
