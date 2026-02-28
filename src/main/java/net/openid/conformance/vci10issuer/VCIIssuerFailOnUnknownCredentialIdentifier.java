package net.openid.conformance.vci10issuer;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.VCIInjectUnknownCredentialIdentifier;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-unknown-credential-identifier",
	displayName = "OID4VCI 1.0: Issuer fail on unknown credential identifier",
	summary = "This test case checks for the proper error handling during the standard credential issuance flow using an emulated wallet when an unknown credential identifier is used.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnUnknownCredentialIdentifier extends VCIIssuerHappyFlow {

	@Override
	protected String serializeCredentialRequestObject(JsonObject credentialRequestObject) {
		callAndContinueOnFailure(VCIInjectUnknownCredentialIdentifier.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		credentialRequestObject.remove("credential_configuration_id");
		credentialRequestObject.remove("credential_identifier");
		credentialRequestObject.remove("credential_identifiers");

		credentialRequestObject.addProperty("credential_identifier", env.getString("vci_credential_identifier"));
		return super.serializeCredentialRequestObject(credentialRequestObject);
	}

	@Override
	protected void verifyCredentialIssuerCredentialResponse() {
		verifyCredentialIssuerCredentialErrorResponse();

		callAndStopOnFailure(new VCIValidateCredentialErrorResponse(VciErrorCode.UNKNOWN_CREDENTIAL_IDENTIFIER), "OID4VCI-1FINAL-8.3.1");
	}
}
