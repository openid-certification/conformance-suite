package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.OAuthUriUtil;
import net.openid.conformance.variant.VCICredentialOfferParameterVariant;

import java.util.Map;

public class VCICreateCredentialOfferRedirectUrl extends AbstractCondition {

	private final VCICredentialOfferParameterVariant vciCredentialOfferParameterVariantType;

	public VCICreateCredentialOfferRedirectUrl(VCICredentialOfferParameterVariant vciCredentialOfferParameterVariantType) {
		this.vciCredentialOfferParameterVariantType = vciCredentialOfferParameterVariantType;
	}

	@Override
	public Environment evaluate(Environment env) {

		String credentialOfferEndpointUrl = env.getString("config", "vci.credential_offer_endpoint");
		Map<String, String> params = switch (vciCredentialOfferParameterVariantType) {
			case BY_VALUE -> {
				JsonElement credentialOfferObject = env.getElementFromObject("vci", "credential_offer");
				yield Map.of("credential_offer", credentialOfferObject.toString());
			}
			case BY_REFERENCE -> Map.of("credential_offer_uri", env.getString("vci", "credential_offer_uri"));
		};

		String url = OAuthUriUtil.appendFormUrlEncodedQuery(credentialOfferEndpointUrl, params);
		env.putString("vci","credential_offer_redirect_url", url);

		logSuccess("Created credential offer redirect url", args("credential_offer_redirect_url", url, "credential_offer_type", vciCredentialOfferParameterVariantType));

		return env;
	}
}
