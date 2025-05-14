package net.openid.conformance.vciid2wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

public class VCICreateCredentialOffer extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String credentialIssuer = env.getString("base_url");
		String issuerState = RandomStringUtils.secure().nextAlphanumeric(64);
		JsonArray credentialConfigurationIds = OIDFJSON.convertListToJsonArray(List.of("eu.europa.ec.eudi.pid.1"));

		JsonObject grantObject = new JsonObject();
		grantObject.addProperty("issuer_state", issuerState);

		JsonObject grantsObject = new JsonObject();
		grantsObject.add("authorization_code", grantObject);

		JsonObject credentialOffer = new JsonObject();
		credentialOffer.addProperty("credential_issuer", credentialIssuer);
		credentialOffer.add("credential_configuration_ids", credentialConfigurationIds);
		credentialOffer.add("grants", grantsObject);

		env.putObject("vci", "credential_offer", credentialOffer);

		logSuccess("Generated credential offer", args("issuer_state", issuerState, "credential_offer", credentialOffer));

		return env;
	}
}
