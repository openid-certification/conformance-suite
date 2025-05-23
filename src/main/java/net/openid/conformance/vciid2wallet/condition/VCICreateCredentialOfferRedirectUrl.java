package net.openid.conformance.vciid2wallet.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.variant.VCICredentialOfferParameterVariant;
import org.springframework.web.util.UriComponentsBuilder;

public class VCICreateCredentialOfferRedirectUrl extends AbstractCondition {

	private final VCICredentialOfferParameterVariant vciCredentialOfferParameterVariantType;

	public VCICreateCredentialOfferRedirectUrl(VCICredentialOfferParameterVariant vciCredentialOfferParameterVariantType) {
		this.vciCredentialOfferParameterVariantType = vciCredentialOfferParameterVariantType;
	}

	@Override
	public Environment evaluate(Environment env) {

		String credentialOfferEndpointUrl = env.getString("config", "vci.credential_offer_endpoint");
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(credentialOfferEndpointUrl);
		switch (vciCredentialOfferParameterVariantType) {
			case BY_VALUE -> {
				JsonElement credentialOfferObject = env.getElementFromObject("vci", "credential_offer");
				builder.queryParam("credential_offer", credentialOfferObject);
			}
			case BY_REFERENCE -> {
				String credentialOfferUri = env.getString("vci", "credential_offer_uri");
				builder.queryParam("credential_offer_uri", credentialOfferUri);
			}
		}

		String url = builder.toUriString();
		env.putString("vci","credential_offer_redirect_url", url);

		logSuccess("Created credential offer redirect url", args("credential_offer_redirect_url", url, "credential_offer_type", vciCredentialOfferParameterVariantType));

		return env;
	}
}
