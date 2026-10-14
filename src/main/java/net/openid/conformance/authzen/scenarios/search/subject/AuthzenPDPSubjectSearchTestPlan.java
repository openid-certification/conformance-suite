package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "authzen-pdp-subject-search-test-plan",
	shortName = "Subject search",
	certifiable = true,
	displayName = "AuthZEN 1.0: PDP server test for Subject Search",
	summary = "Tests a PDP's Subject Search API (Section 8.4) against the fixture in the [certification scenario](https://openid.github.io/authzen/authorization-api-1_0-certification-scenario.html): search results and the decisions they evaluate to, pagination, unknown entities, an ignored subject.id, idempotency, request validation and error responses, unknown fields, the HTTP binding (Section 10.1) and authentication (Sections 11.2 and 11.3). The modules for property-based decisions run only when 'AuthZEN Specification Support' is 'properties'. Every POST module that accepts only an error status sends an X-Request-ID header, and fails if the error response does not echo it (Section 10.1.3), even when the status is right.",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Subject Search API tests from https://github.com/openid/authzen/issues/433
		// Search Core
		AuthzenPDPSubjectSearchValidSubjectSearchRequestWithNonEmptyResultsTest.class,
		AuthzenPDPSubjectSearchSubjectSearchWithContextTest.class,
		AuthzenPDPSubjectSearchSubjectSearchWithSubjectIdOmittedTest.class,
		AuthzenPDPSubjectSearchRequestWithPageLimitTest.class,
		AuthzenPDPSubjectSearchUnknownEntityTypeTest.class,
		AuthzenPDPSubjectSearchUnknownEntityIdentifierTest.class,
		// X-Request-ID handling (Section 10.1.3)
		AuthzenPDPSubjectSearchXRequestIdEchoedTest.class,
		// Idempotency
		AuthzenPDPSubjectSearchIdempotencyTest.class,
		// Transport binding negative tests (Section 10.1.1 / 10.1.2)
		AuthzenPDPSubjectSearchRejectGetMethodTest.class,
		AuthzenPDPSubjectSearchRejectPutMethodTest.class,
		AuthzenPDPSubjectSearchRejectTopLevelArrayTest.class,
		AuthzenPDPSubjectSearchRejectNonJsonContentTypeTest.class,
		AuthzenPDPSubjectSearchAcceptContentTypeWithCharsetTest.class,
		AuthzenPDPSubjectSearchRejectMalformedJsonTest.class,
		AuthzenPDPSubjectSearchRejectEmptyBodyTest.class,
		// Error handling: HTTP 400 negative tests (Section 10.1)
		AuthzenPDPSubjectSearchMissingActionTest.class,
		AuthzenPDPSubjectSearchActionMissingNameTest.class,
		AuthzenPDPSubjectSearchActionIsStringTest.class,
		AuthzenPDPSubjectSearchMissingResourceTest.class,
		AuthzenPDPSubjectSearchResourceMissingIdTest.class,
		AuthzenPDPSubjectSearchResourceMissingTypeTest.class,
		AuthzenPDPSubjectSearchSubjectMissingTypeTest.class,
		AuthzenPDPSubjectSearchMissingSubjectTest.class,
		// Forward-compat: unknown fields ignored (Section 10.1.1)
		AuthzenPDPSubjectSearchUnknownTopLevelFieldsTest.class,
		AuthzenPDPSubjectSearchUnknownPageFieldsTest.class,
		// Extra subject.id ignored (Section 8.4.1)
		AuthzenPDPSubjectSearchExtraSubjectIdIgnoredTest.class,
		// Search Properties (Properties variant only)
		AuthzenPDPSubjectSearchSubjectSearchWithResourcePropertiesTest.class,
		// Authentication (Section 11.3)
		AuthzenPDPSubjectSearchMissingCredentialsReturns401Test.class,
		AuthzenPDPSubjectSearchInvalidCredentialsReturns401Test.class,
	}
)
public class AuthzenPDPSubjectSearchTestPlan implements TestPlan {
	@Override
	public List<String> certificationProfileName(VariantSelection variant) {

		String certProfile = "AuthZEN Subject Search";
		String suffix = "";

		Map<String, String> v = variant.getVariant();
		String authzenSupport = v.get("authzen_support");

		if("properties".equalsIgnoreCase(authzenSupport)) {
			suffix = " w/properties";
		}

		return List.of(certProfile + suffix);
	}
}
