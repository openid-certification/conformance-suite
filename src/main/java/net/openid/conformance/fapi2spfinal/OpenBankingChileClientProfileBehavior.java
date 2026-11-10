package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.ExtractChileResponseSigningJwksFromConfiguration;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

/**
 * Profile behavior for Chile Open Finance client tests.
 * Requires mTLS everywhere, and publishes the key the emulated bank signs its resource
 * responses with at the CMF key endpoint (CMF-XJWS-5.3.2.1).
 */
public class OpenBankingChileClientProfileBehavior extends FAPI2ClientProfileBehavior {

	/** The CMF key endpoint, on the same (mTLS) host as the resource APIs. */
	public static final String RESPONSE_SIGNING_JWKS_PATH = "jws-signature-validation/v1/jwks";

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public ConditionSequence additionalServerConfiguration() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(ExtractChileResponseSigningJwksFromConfiguration.class, "CMF-XJWS-5.3.1");
			}
		};
	}

	@Override
	public void exposeProfileEndpoints() {
		super.exposeProfileEndpoints();
		module.exposeMtlsPath("response_signing_jwks_uri", RESPONSE_SIGNING_JWKS_PATH);
	}

	@Override
	public boolean claimsHttpMtlsPath(String path) {
		return RESPONSE_SIGNING_JWKS_PATH.equals(path);
	}

	@Override
	public Object handleProfileSpecificMtlsPath(String requestId, String path) {
		return module.jwksEndpoint(ExtractChileResponseSigningJwksFromConfiguration.RESPONSE_SIGNING_PUBLIC_JWKS);
	}
}
