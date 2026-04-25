package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.SerializeVCICredentialRequestObject;
import net.openid.conformance.vci10issuer.condition.VCIInjectUnknownCredentialIdentifier;
import net.openid.conformance.vci10issuer.condition.VCIUseCredentialIdentifierInCredentialRequest;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-unknown-credential-identifier",
	displayName = "OID4VCI 1.0: Issuer fail on unknown credential identifier",
	summary = "This test case checks for the proper error handling during the standard credential issuance flow using an emulated wallet when an unknown credential identifier is used.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnUnknownCredentialIdentifier extends AbstractVCIIssuerTestModule {

	@Override
	protected ConditionSequence makeCreateCredentialRequestSteps() {
		return super.makeCreateCredentialRequestSteps()
			.replace(SerializeVCICredentialRequestObject.class, new AbstractConditionSequence() {
				@Override
				public void evaluate() {
					callAndContinueOnFailure(VCIInjectUnknownCredentialIdentifier.class,
						Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
					callAndStopOnFailure(VCIUseCredentialIdentifierInCredentialRequest.class, "OID4VCI-1FINAL-8.2");
					callAndStopOnFailure(SerializeVCICredentialRequestObject.class, "OID4VCI-1FINAL-8.2");
				}
			});
	}

	@Override
	protected void verifyCredentialIssuerCredentialResponse() {
		verifyCredentialIssuerCredentialErrorResponse();

		callAndStopOnFailure(new VCIValidateCredentialErrorResponse(VciErrorCode.UNKNOWN_CREDENTIAL_IDENTIFIER), "OID4VCI-1FINAL-8.3.1");
	}
}
