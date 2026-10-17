package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "authzen-pdp-resource-search-test-plan",
	shortName = "Resource search",
	certifiable = true,
	displayName = "AuthZEN 1.0: PDP server test for Resource Search",
	summary = "Every module that expects the PDP to reject its POST request sends an X-Request-ID header, and fails if the error response does not echo it (Section 10.1.3), even when the status is right.",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Resource Search API tests, following the certification scenario (https://openid.github.io/authzen/authorization-api-1_0-certification-scenario.html)
		// Search Core
		AuthzenPDPResourceSearchValidResourceSearchRequestWithNonEmptyResultsTest.class,
		AuthzenPDPResourceSearchResourceSearchWithContextTest.class,
		AuthzenPDPResourceSearchResultsEvaluateToPermitTest.class,
		AuthzenPDPResourceSearchUnknownEntityIdentifierTest.class,
		AuthzenPDPResourceSearchUnknownEntityTypeTest.class,
		AuthzenPDPResourceSearchResourceSearchWithResourceIdOmittedTest.class,
		AuthzenPDPResourceSearchRequestWithPageLimitTest.class,
		AuthzenPDPResourceSearchChangedPageParametersTest.class,
		// X-Request-ID handling (Section 10.1.3)
		AuthzenPDPResourceSearchXRequestIdEchoedTest.class,
		// Idempotency
		AuthzenPDPResourceSearchIdempotencyTest.class,
		// Transport binding negative tests (Section 10.1.1 / 10.1.2)
		AuthzenPDPResourceSearchRejectGetMethodTest.class,
		AuthzenPDPResourceSearchRejectPutMethodTest.class,
		AuthzenPDPResourceSearchRejectTopLevelArrayTest.class,
		AuthzenPDPResourceSearchRejectNonJsonContentTypeTest.class,
		AuthzenPDPResourceSearchRejectMissingContentTypeTest.class,
		AuthzenPDPResourceSearchAcceptContentTypeWithCharsetTest.class,
		AuthzenPDPResourceSearchRejectMalformedJsonTest.class,
		AuthzenPDPResourceSearchRejectEmptyBodyTest.class,
		// Error handling: HTTP 400 negative tests (Section 10.1)
		AuthzenPDPResourceSearchMissingSubjectTest.class,
		AuthzenPDPResourceSearchSubjectMissingIdTest.class,
		AuthzenPDPResourceSearchSubjectMissingTypeTest.class,
		AuthzenPDPResourceSearchMissingResourceTest.class,
		AuthzenPDPResourceSearchResourceMissingTypeTest.class,
		AuthzenPDPResourceSearchMissingActionTest.class,
		AuthzenPDPResourceSearchActionMissingNameTest.class,
		// Extra resource.id ignored (Section 8.5.1)
		AuthzenPDPResourceSearchExtraResourceIdIgnoredTest.class,
		// Forward-compat: unknown fields ignored (Section 10.1.1)
		AuthzenPDPResourceSearchUnknownTopLevelFieldsTest.class,
		AuthzenPDPResourceSearchUnknownPageFieldsTest.class,
		// Search Properties (Properties variant only)
		AuthzenPDPResourceSearchResourceSearchWithSubjectPropertiesTest.class,
		// Authentication (Section 11.3)
		AuthzenPDPResourceSearchMissingCredentialsReturns401Test.class,
		AuthzenPDPResourceSearchInvalidCredentialsReturns401Test.class,
	}
)
public class AuthzenPDPResourceSearchTestPlan implements TestPlan {
	@Override
	public List<String> certificationProfileName(VariantSelection variant) {

		String certProfile = "AuthZEN Resource Search";
		String suffix = "";

		Map<String, String> v = variant.getVariant();
		String authzenSupport = v.get("authzen_support");

		if("properties".equalsIgnoreCase(authzenSupport)) {
			suffix = " w/properties";
		}

		return List.of(certProfile + suffix);
	}
}
