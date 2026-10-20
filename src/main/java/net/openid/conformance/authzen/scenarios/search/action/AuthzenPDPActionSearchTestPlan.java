package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "authzen-pdp-action-search-test-plan",
	shortName = "Action search",
	certifiable = true,
	displayName = "AuthZEN 1.0: PDP server test for Action Search",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Action Search API tests, following the certification scenario (https://openid.github.io/authzen/authorization-api-1_0-certification-scenario.html)
		// Search Core
		AuthzenPDPActionSearchValidActionSearchRequestWithNonEmptyResultsTest.class,
		AuthzenPDPActionSearchActionSearchWithContextTest.class,
		AuthzenPDPActionSearchResultsEvaluateToPermitTest.class,
		AuthzenPDPActionSearchRequestWithPageLimitTest.class,
		AuthzenPDPActionSearchChangedPageParametersTest.class,
		AuthzenPDPActionSearchChangedPageEntityTest.class,
		AuthzenPDPActionSearchUnknownEntityIdentifierTest.class,
		AuthzenPDPActionSearchUnknownEntityTypeTest.class,
		// X-Request-ID handling (Section 10.1.3)
		AuthzenPDPActionSearchXRequestIdEchoedTest.class,
		// Idempotency
		AuthzenPDPActionSearchIdempotencyTest.class,
		// Transport binding negative tests (Section 10.1.1 / 10.1.2)
		AuthzenPDPActionSearchRejectGetMethodTest.class,
		AuthzenPDPActionSearchRejectPutMethodTest.class,
		AuthzenPDPActionSearchRejectTopLevelArrayTest.class,
		AuthzenPDPActionSearchRejectNonJsonContentTypeTest.class,
		AuthzenPDPActionSearchRejectMissingContentTypeTest.class,
		AuthzenPDPActionSearchAcceptContentTypeWithCharsetTest.class,
		AuthzenPDPActionSearchRejectMalformedJsonTest.class,
		AuthzenPDPActionSearchRejectEmptyBodyTest.class,
		// Error handling: HTTP 400 negative tests (Section 10.1)
		AuthzenPDPActionSearchMissingResourceTest.class,
		AuthzenPDPActionSearchResourceMissingIdTest.class,
		AuthzenPDPActionSearchResourceMissingTypeTest.class,
		AuthzenPDPActionSearchMissingSubjectTest.class,
		AuthzenPDPActionSearchSubjectMissingIdTest.class,
		AuthzenPDPActionSearchSubjectMissingTypeTest.class,
		AuthzenPDPActionSearchSubjectIsStringTest.class,
		// Forward-compat: unknown fields ignored (Section 10.1.1)
		AuthzenPDPActionSearchUnknownTopLevelFieldsTest.class,
		AuthzenPDPActionSearchUnknownPageFieldsTest.class,
		// An action in an Action Search request, which Section 8.6.1 omits: ignored or rejected
		AuthzenPDPActionSearchExtraActionTest.class,
		// Search Properties (Properties variant only)
		AuthzenPDPActionSearchActionSearchWithPropertiesTest.class,
		// Authentication (Section 11.3)
		AuthzenPDPActionSearchMissingCredentialsReturns401Test.class,
		AuthzenPDPActionSearchInvalidCredentialsReturns401Test.class,
	}
)
public class AuthzenPDPActionSearchTestPlan implements TestPlan {
	@Override
	public List<String> certificationProfileName(VariantSelection variant) {

		String certProfile = "AuthZEN Action Search";
		String suffix = "";

		Map<String, String> v = variant.getVariant();
		String authzenSupport = v.get("authzen_support");

		if("properties".equalsIgnoreCase(authzenSupport)) {
			suffix = " w/properties";
		}

		return List.of(certProfile + suffix);
	}
}
