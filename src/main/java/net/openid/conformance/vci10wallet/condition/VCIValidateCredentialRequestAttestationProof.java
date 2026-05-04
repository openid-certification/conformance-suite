package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

import java.text.ParseException;

public class VCIValidateCredentialRequestAttestationProof extends AbstractVCIValidateCredentialRequestProof {

	@Override
	protected void validateProof(Environment env, String proofType, String expectedAudience, String credentialConfigurationId, JsonObject credentialConfiguration, JsonObject keyAttestationRequired) {
		String attestationJwt = env.getString("proof_attestation", "value");
		try {
			env.putObject("vci", "key_attestation_jwt", JWTUtil.jwtStringToJsonObjectForEnvironment(attestationJwt));
		} catch (ParseException e) {
			String errorDescription = "Key attestation validation of proof failed: could not parse JWT";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, e, args("attestation", attestationJwt));
		}
		logSuccess("Parsed key attestation JWT for proof type: " + proofType, args("proof_type", proofType));
	}
}
