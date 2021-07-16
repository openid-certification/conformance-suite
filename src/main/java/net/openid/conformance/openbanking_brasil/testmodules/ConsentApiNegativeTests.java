package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.openbanking_brasil.consent.CreateNewConsentValidator;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@PublishTestModule(
	testName = "consent-api-test-bad-permissions",
	displayName = "Ensures request for invalid",
	summary = "Ensures permissions allow you to call only the correct resources - When completed, please upload a screenshot of the permissions being requested by the bank",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl"
	}
)
public class ConsentApiNegativeTests extends AbstractClientCredentialsGrantFunctionalTestModule {

	// Permission groups
	String[] nonExistentPermission  = new String[] {"BAD_PERMISSION"};
	String[] creditOperationsContractData = new String[] {"LOANS_READ", "LOANS_WARRANTIES_READ", "LOANS_SCHEDULED_INSTALMENTS_READ", "LOANS_PAYMENTS_READ", "FINANCINGS_READ", "FINANCINGS_WARRANTIES_READ", "FINANCINGS_SCHEDULED_INSTALMENTS_READ", "FINANCINGS_PAYMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ", "INVOICE_FINANCINGS_READ", "INVOICE_FINANCINGS_WARRANTIES_READ", "INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ", "INVOICE_FINANCINGS_PAYMENTS_READ", "RESOURCES_READ"};
	String[] incompleteCreditOperationsContractData = new String[] {"LOANS_WARRANTIES_READ", "LOANS_SCHEDULED_INSTALMENTS_READ", "LOANS_PAYMENTS_READ", "FINANCINGS_READ", "FINANCINGS_WARRANTIES_READ", "FINANCINGS_SCHEDULED_INSTALMENTS_READ", "FINANCINGS_PAYMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ", "INVOICE_FINANCINGS_READ", "INVOICE_FINANCINGS_WARRANTIES_READ", "INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ", "INVOICE_FINANCINGS_PAYMENTS_READ", "RESOURCES_READ"};
	String[] incompleteLimits = new String[] {"ACCOUNTS_OVERDRAFT_LIMITS_READ", "RESOURCES_READ"};
	String[] incompleteCombo = arrayUtils.concatArrays(incompleteLimits, creditOperationsContractData);

	// Bad date Strings
	Instant expiryTime = Instant.now().plus(2, ChronoUnit.HOURS);
	Instant expiryTimeNoFractionalSeconds = expiryTime.truncatedTo(ChronoUnit.SECONDS);
	String rfc3339ExpiryTime = DateTimeFormatter.ISO_INSTANT.format(expiryTimeNoFractionalSeconds);

	@Override
	protected void runTests() {

		validateBadPermission(incompleteCreditOperationsContractData, "incomplete Credit Operations Contract Data permission group");
		validateBadPermission(incompleteCombo, "incomplete combination of Limits & Credit Operations Contract Data permission groups");
		validateBadPermission(nonExistentPermission, "non-existent permission group");

		validateBadExpiration(dateTimeGreaterThanAYear(), "DateTime greater than 1 year from now");
		validateBadExpiration(dateTimeInThePast(), "DateTime in the past");
		validateBadExpiration("2021-13-32T10:00:00-05:00", "bad DateTime"); // not a dateTime		
	}

	private void validateBadPermission(String[] permissions, String description) {
		String logMessage = String.format("Check for HTTP 400 response from consent api request for %s", description);
		runInBlock(logMessage, () -> {

			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			env.putString("consent_permissions", String.join(" ", permissions));
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class);
			callAndStopOnFailure(EnsureResponseCodeWas400.class);
		});
	}

	private void validateBadExpiration(String rfc3339ExpiryTime, String description) {
		String logMessage = String.format("Check for HTTP 400 response from consent api request for %s.", description);
		runInBlock(logMessage, () -> {

			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			env.putString("consent_permissions", String.join(" ", creditOperationsContractData));
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			addExpirationToRequest(rfc3339ExpiryTime);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class);
			callAndStopOnFailure(EnsureResponseCodeWas400.class);
		});	
	}

	private void addExpirationToRequest(String rfc3339ExpiryTime) {

		JsonObject consentRequest = env.getObject("consent_endpoint_request");
		JsonObject data = consentRequest.getAsJsonObject("data");

		data.addProperty("expirationDateTime", rfc3339ExpiryTime);
	}

	static String dateTimeGreaterThanAYear() {
		Instant expiryTime = Instant.now().plus(367, ChronoUnit.DAYS);
		Instant expiryTimeNoFractionalSeconds = expiryTime.truncatedTo(ChronoUnit.SECONDS);
		return DateTimeFormatter.ISO_INSTANT.format(expiryTimeNoFractionalSeconds);
	}

	static String dateTimeInThePast() {
		Instant expiryTime = Instant.now().minus(2, ChronoUnit.HOURS);
		Instant expiryTimeNoFractionalSeconds = expiryTime.truncatedTo(ChronoUnit.SECONDS);
		return DateTimeFormatter.ISO_INSTANT.format(expiryTimeNoFractionalSeconds);
	}
}
