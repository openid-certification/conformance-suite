package net.openid.conformance.ekyc.plan;

import net.openid.conformance.ekycid3.test.oidccore.EKYCID3HappyPathTest;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3HappyPathTestEmptyObjects;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3HappyPathTestEssentialFalse;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3RequestClaimUnknownToTheOP;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3RequestClaimWithRandomValueMustBeOmitted;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3RequestClaimWithSpecialCharsUnknownToTheOP;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3RequestEssentialClaimUnknownToTheOP;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3RequestVerifiedClaimsOnlyInIdToken;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3RequestVerifiedClaimsOnlyInUserinfo;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3TestUserinfoContainsDataNotAdvertisedInOPMetadata;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3TestUserinfoDefaults;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3TestWithUserProvidedRequest;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3TooLongPurposeLeadsToInvalidRequestResponse;
import net.openid.conformance.ekycid3.test.oidccore.EKYCID3TooShortPurposeLeadsToInvalidRequestResponse;import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;


@PublishTestPlan(
	testPlanName = "ekycid3-test-plan-oidccore",
	displayName = "OpenID for IDA-ID3 using OpenID Connect Core",
	profile = TestPlan.ProfileNames.ekyctest,
	testModules = {
		EKYCID3HappyPathTest.class,
		EKYCID3HappyPathTestEmptyObjects.class,
		EKYCID3HappyPathTestEssentialFalse.class,
		EKYCID3RequestClaimUnknownToTheOP.class,
		EKYCID3RequestEssentialClaimUnknownToTheOP.class,
		EKYCID3RequestClaimWithSpecialCharsUnknownToTheOP.class,
		EKYCID3RequestClaimWithRandomValueMustBeOmitted.class,
		EKYCID3TooLongPurposeLeadsToInvalidRequestResponse.class,
		EKYCID3TooShortPurposeLeadsToInvalidRequestResponse.class,
		EKYCID3RequestVerifiedClaimsOnlyInIdToken.class,
		EKYCID3RequestVerifiedClaimsOnlyInUserinfo.class,
		EKYCID3TestWithUserProvidedRequest.class,
		EKYCID3TestUserinfoDefaults.class,
		EKYCID3TestUserinfoContainsDataNotAdvertisedInOPMetadata.class
		//TODO how to test max_age? how can the following in Yes.com tests can be implemented?
		// Test: Check if max_age is evaluated correctly. Note: max_age is chosen 1000 years in the future and should not limit the result. Expected result: All Claims are being delivered as listed in the request.
	}
)
public class EKYCID3WithOIDCCoreTestPlan implements TestPlan {
}
/*
IA-9
purpose: OPTIONAL. String describing the purpose for obtaining certain user data from the OP.
The purpose MUST NOT be shorter than 3 characters and MUST NOT be longer than 300 characters.
If these rules are violated, the authentication request MUST fail and the OP returns an error invalid_request to the RP.
- Test: Send long purpose
- Test: Send short purpose
Note: In order to prevent injection attacks, the OP MUST escape the text appropriately before it will be shown in a user
interface. The OP MUST expect special characters in the URL decoded purpose text provided by the RP.
The OP MUST ensure that any special characters in the purpose text cannot be used to inject code into
the web interface of the OP (e.g., cross-site scripting, defacing). Proper escaping MUST be applied by the OP.
The OP SHALL NOT remove characters from the purpose text to this end.
- Test: Send html special chars and require screenshot


 */
