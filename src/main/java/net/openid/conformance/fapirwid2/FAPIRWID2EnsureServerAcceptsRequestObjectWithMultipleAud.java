package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddMultipleAudToRequestObject;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-request-object-with-multiple-aud-succeeds",
	displayName = "FAPI-RW-ID2: ensure request object with multiple aud succeeds",
	summary = "This test pass aud value as an array containing good and bad values then server must accept it.",
	profile = "FAPI-RW-ID2",
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
public class FAPIRWID2EnsureServerAcceptsRequestObjectWithMultipleAud extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		return super.makeCreateAuthorizationRequestObjectSteps()
			.replace(AddAudToRequestObject.class,
					condition(AddMultipleAudToRequestObject.class).requirement("RFC7519-4.1.3"));
	}
}
