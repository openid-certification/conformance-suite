package net.openid.conformance.openinsurance.testmodule.v1.consents;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "opin-consent-api-test-permission-groups",
	displayName = "Validate that consent API accepts the consent groups",
	summary = "Validates that consent API accepts the consent groups\n" +
		"\u2022 Creates a series of consent requests with valid permissions group and expect for each of them a 201 to be returned by the server\n" +
		"\u2022 Validates consent API request for 'Personal Registration Data' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Personal Additional Information' permission group(s)\n" +
			"\u2022 Validates consent API request for 'Personal Qualification' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Business Registration Data' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Business Additional Information' permission group(s)\n" +
			"\u2022 Validates consent API request for 'Business Qualification' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Capitalization Titles' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Pension Risk' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Damages and People Patrimonial' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Damages and People Aeronautical' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Damages and People Nautical' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Damages and People Nuclear' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Damages and People Oil' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Damages and People Responsability' permission group(s)\n"+
		"\u2022 Validates consent API request for 'Damages and People Transport' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Damages and People Financial Risks' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Damages and People Rural' permission group(s)\n" +
		"\u2022 Validates consent API request for 'Damages and People Auto' permission group(s)\n" +
			"\u2022 Validates consent API request for 'Damages and People Housing' permission group(s)\n" +
			"\u2022 Validates consent API request for 'Damages and People Acceptance and Branches Abroad' permission group(s)\n" +
			"\u2022 Validates consent API request for 'Damages and People Person' permission group(s)\n" +
		"\u2022 Expect a 201 - Created to be returned by the server, or 422 - Unprocessable Entity in case the server does not support the permission group mentioned\n" +
			"\u2022 For 201 responses, ensure that the permissions are not widened\n",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
public class OpinConsentsApiPermissionGroupsTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	private OpinConsentPermissionsBuilder permissionsBuilder;
	private boolean passed = false;

	@Override
	protected void runTests() {
		permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);
		passed = false;


		String productType = env.getString("config", "consent.productType");
		if (!Strings.isNullOrEmpty(productType) && productType.equals("business")) {
			permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.CUSTOMERS_BUSINESS).build();
			validatePermissions(PermissionsGroup.CUSTOMERS_BUSINESS);
		}
		if (!Strings.isNullOrEmpty(productType) && productType.equals("personal")) {
			permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.CUSTOMERS_PERSONAL).build();
			validatePermissions(PermissionsGroup.CUSTOMERS_PERSONAL);
		}


		for (PermissionsGroup permissionsGroup : PermissionsGroup.values()) {
			if (permissionsGroup.equals(PermissionsGroup.CUSTOMERS_BUSINESS) ||
					permissionsGroup.equals(PermissionsGroup.CUSTOMERS_BUSINESS) ||
					permissionsGroup.equals(PermissionsGroup.RESOURCES) ||
					permissionsGroup.equals(PermissionsGroup.ALL)) {
				continue;
			}
			permissionsBuilder.resetPermissions().addPermissionsGroup(permissionsGroup).build();
			validatePermissions(permissionsGroup);
		}

		//If all validates returned a 422
		if (!passed) {
			throw new TestFailureException(getId(), "All resources returned a 422 when at least one set of permissions should have passed");
		}
	}


	private void validatePermissions(PermissionsGroup permissionsGroup) {
		String logMessage = String.format("Validate consent api request for '%s' permission group(s)", permissionsGroup.name());
		runInBlock(logMessage, () -> {

			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndStopOnFailure(IgnoreResponseError.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.SUCCESS);

			if (!env.getString("resource_endpoint_response").equals("{}")) {
				passed = true;
				callAndStopOnFailure(ValidateRequestedPermissionsAreNotWidened.class, Condition.ConditionResult.FAILURE);
			} else {
				callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
				callAndContinueOnFailure(EnsureResponseCodeWas422.class, Condition.ConditionResult.FAILURE);
			}

		});
	}
}
