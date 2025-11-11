package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class VCIValidateCredentialRequestDiVpProof extends AbstractVCIValidateCredentialRequestProof {

	@Override
	protected void validateProof(Environment env, String proofType, String expectedAudience, String credentialConfigurationId, JsonObject credentialConfiguration, JsonObject keyAttestationRequired) {
		throw error("di_vp proof not implemented yet");
	}
}
