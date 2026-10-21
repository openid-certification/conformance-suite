package net.openid.conformance.condition.as;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

/**
 * Creates an SD-JWT+KB credential whose vct is a type the verifier did not request. The type is
 * unrelated to any real credential type, so it neither matches nor inherits from a requested
 * vct_values entry. The claims, issuer signature and Key Binding JWT are otherwise valid.
 */
public class CreateSdJwtKbCredentialWithWrongVct extends CreateSdJwtKbCredential {

	public static final String WRONG_VCT = "urn:conformance-suite:unrequested:credential:1";

	@Override
	protected String createSdJwt(Environment env, JWK publicJWK, ECKey privateKey, String credentialType,
								 Map<String, Object> credentialClaims) {
		log("Using a vct that the verifier did not request",
			args("original_vct", credentialType, "wrong_vct", WRONG_VCT));
		return super.createSdJwt(env, publicJWK, privateKey, WRONG_VCT, credentialClaims);
	}
}
