package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class SignedPaymentConsentSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(InsertProceedWithTestString.class);
		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
		callAndStopOnFailure(CreateIdempotencyKey.class);
		callAndStopOnFailure(AddIdempotencyKeyHeader.class);
		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);
		callAndStopOnFailure(FAPIBrazilExtractClientMTLSCertificateSubject.class);
		call(exec().mapKey("request_object_claims", "consent_endpoint_request"));
		callAndStopOnFailure(AddAudAsPaymentConsentUriToRequestObject.class, "BrazilOB-6.1");
		callAndStopOnFailure(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");
		callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");
		callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");
		call(exec().unmapKey("request_object_claims"));
		callAndStopOnFailure(FAPIBrazilSignPaymentConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class, Condition.ConditionResult.FAILURE);
		call(exec().mapKey("endpoint_response", "consent_endpoint_response_full"));
		call(exec().mapKey("endpoint_response_jwt", "consent_endpoint_response_jwt"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE);

		call(condition(EnsureContentTypeApplicationJwt.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("BrazilOB-6.1")
			.skipIfStringMissing("proceed_with_test"));

		call(condition(ExtractSignedJwtFromResourceResponse.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("BrazilOB-6.1")
			.skipIfStringMissing("proceed_with_test"));

		call(condition(FAPIBrazilValidateResourceResponseSigningAlg.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("BrazilOB-6.1")
			.skipIfStringMissing("proceed_with_test"));

		call(condition(FAPIBrazilValidateResourceResponseTyp.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("BrazilOB-6.1")
			.skipIfStringMissing("proceed_with_test"));

		call(condition(FAPIBrazilGetKeystoreJwksUri.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.skipIfStringMissing("proceed_with_test"));

		call(exec().mapKey("server", "org_server"));
		call(exec().mapKey("server_jwks", "org_server_jwks"));

		call(condition(FetchServerKeys.class)
			.skipIfStringMissing("proceed_with_test"));

		call(exec().unmapKey("server"));
		call(exec().unmapKey("server_jwks"));

		call(condition(ValidateResourceResponseSignature.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("BrazilOB-6.1")
			.skipIfStringMissing("proceed_with_test"));

		call(condition(ValidateResourceResponseJwtClaims.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("BrazilOB-6.1")
			.skipIfStringMissing("proceed_with_test"));

		call(exec().unmapKey("endpoint_response"));
		call(exec().unmapKey("endpoint_response_jwt"));
	}
}
