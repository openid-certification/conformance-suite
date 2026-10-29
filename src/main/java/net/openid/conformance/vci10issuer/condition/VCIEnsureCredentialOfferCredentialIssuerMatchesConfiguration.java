package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * The test fetched the credential issuer metadata, and runs the whole flow, against the
 * 'Credential Issuer URL' in the test configuration; a credential offer naming another
 * credential_issuer (OID4VCI 1.0 Final section 4.1.1) is an offer for a different issuer.
 * Compared as a simple string, as section 12.2.4 compares Credential Issuer identifiers.
 */
public class VCIEnsureCredentialOfferCredentialIssuerMatchesConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"vci", "config"})
	public Environment evaluate(Environment env) {
		JsonElement offerIssuerEl = env.getElementFromObject("vci", "credential_offer.credential_issuer");
		if (offerIssuerEl == null || !OIDFJSON.isString(offerIssuerEl)) {
			throw error("credential_issuer is missing or not a string in the credential offer",
				args("credential_issuer", offerIssuerEl));
		}
		String offerIssuer = OIDFJSON.getString(offerIssuerEl);
		String configuredIssuer = env.getString("config", "vci.credential_issuer_url");

		if (!offerIssuer.equals(configuredIssuer)) {
			throw error("The credential offer names a different credential issuer than the 'Credential Issuer URL' in the test configuration. "
					+ "The test fetched the metadata of, and tests, the configured credential issuer; deliver an offer from that issuer, "
					+ "or change 'Credential Issuer URL' to the issuer that produced the offer.",
				args("credential_offer_credential_issuer", offerIssuer, "configured_credential_issuer_url", configuredIssuer));
		}

		logSuccess("The credential offer's credential_issuer matches the 'Credential Issuer URL' in the test configuration",
			args("credential_issuer", offerIssuer));
		return env;
	}
}
