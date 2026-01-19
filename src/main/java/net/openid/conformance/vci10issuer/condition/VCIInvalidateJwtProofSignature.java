package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.testmodule.Environment;

/**
 * Invalidates the signature on the JWT proof.
 * This is used for negative testing to ensure the issuer properly rejects
 * JWT proofs with invalid signatures.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-7.2.1">OID4VCI Section 7.2.1 - JWT Proof Type</a>
 */
public class VCIInvalidateJwtProofSignature extends AbstractInvalidateJwsSignature {

	@Override
	@PreEnvironment(required = {"vci", "credential_request_proofs"})
	@PostEnvironment(required = {"vci", "credential_request_proofs"})
	public Environment evaluate(Environment env) {
		// Invalidate the signature in vci.proof.jwt
		String jwt = env.getString("vci", "proof.jwt");
		if (jwt == null) {
			throw error("JWT proof not found in environment at vci.proof.jwt");
		}

		String invalidJwt = invalidateSignatureString("vci.proof.jwt", jwt);
		env.putString("vci", "proof.jwt", invalidJwt);

		// Also update the credential_request_proofs object since that's what gets sent to the server
		JsonObject proofsObject = new JsonObject();
		JsonArray jwtArray = new JsonArray();
		jwtArray.add(invalidJwt);
		proofsObject.add("jwt", jwtArray);
		env.putObject("credential_request_proofs", proofsObject);

		logSuccess("Invalidated JWT proof signature",
			args("original_jwt", jwt, "invalid_jwt", invalidJwt));

		return env;
	}
}
