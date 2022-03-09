package net.openid.conformance.ekyc.plan;

import net.openid.conformance.ekyc.test.oidccore.EKYCHappyPathTest;
import net.openid.conformance.ekyc.test.oidccore.EKYCHappyPathTestEmptyObjects;
import net.openid.conformance.ekyc.test.oidccore.EKYCHappyPathTestEssentialFalse;
import net.openid.conformance.ekyc.test.oidccore.EKYCRequestClaimUnknownToTheOP;
import net.openid.conformance.ekyc.test.oidccore.EKYCRequestClaimWithRandomValueMustBeOmitted;
import net.openid.conformance.ekyc.test.oidccore.EKYCRequestClaimWithSpecialCharsUnknownToTheOP;
import net.openid.conformance.ekyc.test.oidccore.EKYCRequestEssentialClaimUnknownToTheOP;
import net.openid.conformance.ekyc.test.oidccore.EKYCRequestVerifiedClaimsOnlyInIdToken;
import net.openid.conformance.ekyc.test.oidccore.EKYCRequestVerifiedClaimsOnlyInUserinfo;
import net.openid.conformance.ekyc.test.oidccore.EKYCTestUserinfoContainsDataNotAdvertisedInOPMetadata;
import net.openid.conformance.ekyc.test.oidccore.EKYCTestUserinfoDefaults;
import net.openid.conformance.ekyc.test.oidccore.EKYCTestWithUserProvidedRequest;
import net.openid.conformance.ekyc.test.oidccore.EKYCTooLongPurposeLeadsToInvalidRequestResponse;
import net.openid.conformance.ekyc.test.oidccore.EKYCTooShortPurposeLeadsToInvalidRequestResponse;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;


@PublishTestPlan(
	testPlanName = "ekyc-test-plan-oidccore",
	displayName = "OpenID for IDA using OpenID Connect Core",
	profile = TestPlan.ProfileNames.ekyctest,
	testModules = {
		EKYCHappyPathTest.class,
		EKYCHappyPathTestEmptyObjects.class,
		EKYCHappyPathTestEssentialFalse.class,
		EKYCRequestClaimUnknownToTheOP.class,
		EKYCRequestEssentialClaimUnknownToTheOP.class,
		EKYCRequestClaimWithSpecialCharsUnknownToTheOP.class,
		EKYCRequestClaimWithRandomValueMustBeOmitted.class,
		EKYCTooLongPurposeLeadsToInvalidRequestResponse.class,
		EKYCTooShortPurposeLeadsToInvalidRequestResponse.class,
		EKYCRequestVerifiedClaimsOnlyInIdToken.class,
		EKYCRequestVerifiedClaimsOnlyInUserinfo.class,
		EKYCTestWithUserProvidedRequest.class,
		EKYCTestUserinfoDefaults.class,
		EKYCTestUserinfoContainsDataNotAdvertisedInOPMetadata.class
		//TODO how to test max_age? how can the following in Yes.com tests can be implemented?
		// Test: Check if max_age is evaluated correctly. Note: max_age is chosen 1000 years in the future and should not limit the result. Expected result: All Claims are being delivered as listed in the request.
	}
)
public class EKYCWithOIDCCoreTestPlan implements TestPlan {
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
