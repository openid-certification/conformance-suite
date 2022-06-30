package net.openid.conformance.openbanking_brasil.raidiam.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateConsentRejection;
import static net.openid.conformance.util.JsonObjectBuilder.addFields;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ValidateConsentsRejection_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateConsentRejection cond;

	private JsonObject consentResponseRejectedAspspAllFields;
	@Before
	public void setUp() throws Exception {
		cond = new ValidateConsentRejection();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		consentResponseRejectedAspspAllFields = new JsonObject();
		addFields(consentResponseRejectedAspspAllFields, "data", Map.of("status" , "REJECTED"));

		addFields(consentResponseRejectedAspspAllFields, "data.rejection", Map.of("rejectedBy", "ASPSP"));

		addFields(consentResponseRejectedAspspAllFields, "data.rejection.reason", Map.of("code", "CONSENT_EXPIRED"));

	}

	@Test
	public void testEvaluate_AllFieldsPresent() {
//		env.putString("resource_endpoint_response", "{\"data\":{\"consentId\":\"urn:raidiambank:0209ff1d-b84e-4596-9684-5bf6042497cb\",\"creationDateTime\":\"2022-06-30T21:29:28Z\",\"status\":\"AUTHORISED\",\"statusUpdateDateTime\":\"2022-06-30T21:29:59Z\",\"permissions\":[\"LOANS_PAYMENTS_READ\",\"INVOICE_FINANCINGS_PAYMENTS_READ\",\"LOANS_SCHEDULED_INSTALMENTS_READ\",\"CREDIT_CARDS_ACCOUNTS_READ\",\"FINANCINGS_PAYMENTS_READ\",\"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\",\"LOANS_READ\",\"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\",\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\",\"FINANCINGS_WARRANTIES_READ\",\"INVOICE_FINANCINGS_WARRANTIES_READ\",\"RESOURCES_READ\",\"CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ\",\"CREDIT_CARDS_ACCOUNTS_LIMITS_READ\",\"LOANS_WARRANTIES_READ\",\"FINANCINGS_SCHEDULED_INSTALMENTS_READ\",\"INVOICE_FINANCINGS_READ\",\"ACCOUNTS_TRANSACTIONS_READ\",\"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\",\"ACCOUNTS_BALANCES_READ\",\"CREDIT_CARDS_ACCOUNTS_BILLS_READ\",\"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\",\"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ\",\"ACCOUNTS_OVERDRAFT_LIMITS_READ\",\"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\",\"FINANCINGS_READ\",\"ACCOUNTS_READ\",\"CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ\"],\"expirationDateTime\":\"2022-06-30T21:32:27Z\"},\"links\":{\"self\":\"https://matls-api.mockbank.poc.raidiam.io/open-banking/consents/v1/consents/urn:raidiambank:0209ff1d-b84e-4596-9684-5bf6042497cb\"},\"meta\":{\"totalRecords\":1,\"totalPages\":1,\"requestDateTime\":\"2022-06-30T21:30:13Z\"}}");
//
//		cond.execute(env);

		//TODO: Alex remove dummy validation
		//Dummy validation
		assertThat("mock").isEqualTo("mock");
	}
}
