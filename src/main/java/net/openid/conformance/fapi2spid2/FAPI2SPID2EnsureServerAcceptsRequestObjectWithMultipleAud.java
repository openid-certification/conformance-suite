package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddMultipleAudToRequestObject;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds",
	displayName = "FAPI2-Security-Profile-ID2: ensure request object with multiple aud succeeds",
	summary = "This test pass aud value as an array containing good and bad values then server must accept it.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(
		parameter = FAPI2ID2OPProfile.class,
		values = "cbuae"
)
public class FAPI2SPID2EnsureServerAcceptsRequestObjectWithMultipleAud extends AbstractFAPI2SPID2ServerTestModule {

	@Override
	protected void performPARRedirectWithRequestUri() {
		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class, "PAR-4");
		performRedirect();
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		return super.makeCreateAuthorizationRequestObjectSteps()
			.replace(AddAudToRequestObject.class,
					condition(AddMultipleAudToRequestObject.class).requirement("RFC7519-4.1.3"));
	}
}
