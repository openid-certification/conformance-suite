package net.openid.conformance.vci10wallet.condition;

import jakarta.ws.rs.core.UriBuilder;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class VCICreateCredentialOfferUri extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String credentialIssuerUrl = env.getString("credential_issuer_metadata", "credential_issuer");

		String credentialOfferId = RandomStringUtils.secure().nextAlphanumeric(64);

		UriBuilder uriBuilder = UriBuilder.fromUri(credentialIssuerUrl);
		uriBuilder.path("/credential_offer/" + credentialOfferId);

		String credentialOfferUri = uriBuilder.toString();
		env.putString("vci","credential_offer_id", credentialOfferId);
		env.putString("vci","credential_offer_uri", credentialOfferUri);

		logSuccess("Generated credential offer URI", args("credential_offer_uri", credentialOfferUri, "credential_offer_id", credentialOfferId));

		return env;
	}
}
