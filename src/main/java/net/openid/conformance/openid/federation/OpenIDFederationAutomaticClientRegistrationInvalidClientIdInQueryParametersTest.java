package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
		testName = "openid-federation-automatic-client-registration-invalid-client-id-in-query-parameters",
		displayName = "openid-federation-automatic-client-registration-invalid-client-id-in-query-parameters",
		summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
			"deliberately not using its entity identifier as the client id in the query parameters",
		profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationInvalidClientIdInQueryParametersTest extends OpenIDFederationAutomaticClientRegistrationTest {

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.BY_VALUE;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.GET;
	}

	@Override
	protected void postProcessQueryParameters() {
		callAndContinueOnFailure(AddInvalidClientIdToQueryParameters.class, Condition.ConditionResult.FAILURE);
	}
}
