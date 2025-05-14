package net.openid.conformance.vciid2wallet.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class VCICreateCredentialOfferRedirectUrl extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String credentialOfferEndpointUrl = env.getString("config", "vci.credential_offer_endpoint");
		JsonElement credentialOfferObject = env.getElementFromObject("vci", "credential_offer");

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(credentialOfferEndpointUrl);
		builder.queryParam("credential_offer", credentialOfferObject);

		String url = builder.toUriString();
		env.putString("vci","credential_offer_redirect_url", url);

		logSuccess("Created credential offer redirect url", args("credential_offer_redirect_url", url));

		return env;
	}
}
