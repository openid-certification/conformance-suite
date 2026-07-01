package net.openid.conformance.condition.as;

/**
 * Generates a minimal FAPI2-compliant server JWKS: a single PS256 RSA signing key with an
 * explicit {@code alg} field, plus one RSA encryption key.
 *
 * <p>Using a single key with an explicit {@code alg} means:
 * <ul>
 *   <li>{@link ExtractServerSigningAlg} works (requires exactly one key with an explicit alg)</li>
 *   <li>{@link net.openid.conformance.condition.client.AugmentRealJwksWithDecoys} can generate
 *       PS256/ES256/EdDSA decoy keys around it</li>
 * </ul>
 */
public class FAPI2GenerateServerJWKs extends OIDCCGenerateServerJWKs {

	@Override
	protected void setupParameters() {
		numberOfRSASigningKeysWithNoAlg = 0;
		numberOfECCurveP256SigningKeysWithNoAlg = 0;
		numberOfECCurveSECP256KSigningKeysWithNoAlg = 0;
		numberOfOKPSigningKeysWithNoAlg = 0;
		numberOfPSSigningKeys = 1;
		numberOfRSAEncKeys = 1;
		numberOfECEncKeys = 0;
	}
}
