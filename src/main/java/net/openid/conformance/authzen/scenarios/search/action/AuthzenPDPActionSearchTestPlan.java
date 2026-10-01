package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "authzen-pdp-action-search-test-plan",
	displayName = "AuthZEN 1.0: PDP server test for Action Search - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Action Search API tests from https://github.com/openid/authzen/issues/433
		// Search Core
		AuthzenPDPActionSearchValidActionSearchRequestWithNonEmptyResultsTest.class,
		AuthzenPDPActionSearchActionSearchWithContextTest.class,
		AuthzenPDPActionSearchUnknownEntityIdentifierTest.class,
		// X-Request-ID handling (Section 10.1.3)
		AuthzenPDPActionSearchXRequestIdEchoedTest.class,
		// Idempotency
		AuthzenPDPActionSearchIdempotencyTest.class,
		// Transport binding negative tests (Section 10.1.1 / 10.1.2)
		AuthzenPDPActionSearchRejectGetMethodTest.class,
		AuthzenPDPActionSearchRejectPutMethodTest.class,
		AuthzenPDPActionSearchRejectTopLevelArrayTest.class,
		AuthzenPDPActionSearchRejectNonJsonContentTypeTest.class,
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
		// Forward-compat: unknown top-level fields ignored (Section 10.1.1)
		AuthzenPDPActionSearchUnknownTopLevelFieldsTest.class,
		// Extra action ignored (Section 10.1.1 forward compatibility)
		AuthzenPDPActionSearchExtraActionIgnoredTest.class,
		// Search Properties (Properties variant only)
		AuthzenPDPActionSearchActionSearchWithPropertiesTest.class,
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
