package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class FAPIBrasilCreateConsentRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FAPIBrazilOpenBankingCreateConsentRequest condition;

	@Test
	public void expectsACPF() {

		JsonObject resourceConfig = new JsonObject();

		env.putObject("config", resourceConfig);

		try {
			condition.execute(env);
		} catch(ConditionError ce) {
			assertThat(ce.getMessage(), equalTo("FAPIBrazilOpenBankingCreateConsentRequest: A least one of CPF and CNPJ must be specified in the test configuration"));
		}

	}

	@Test
	public void infersDefaultPermissionAccountsIfNoneProvided() {

		JsonObject config = new JsonObject();
		JsonObject resourceConfig = new JsonObject();
		config.add("resource", resourceConfig);
		resourceConfig.addProperty("brazilCpf", "138830383");

		JsonObject consentConfig = new JsonObject();
		config.add("consent", consentConfig);
		consentConfig.addProperty("productType", "business");

		env.putObject("config", config);

		JsonObject client = new JsonObject();
		client.addProperty("scope", "openid accounts");
		env.putObject("client", client);

		condition.execute(env);

		JsonObject consents = env.getObject("consent_endpoint_request").getAsJsonObject("data");
		JsonArray permissions = consents.getAsJsonArray("permissions");

		assertTrue(permissions.contains(new JsonPrimitive("ACCOUNTS_READ")));

	}

	@Test
	public void infersDefaultPermissionIfNoneProvided() {

		JsonObject config = new JsonObject();
		JsonObject resourceConfig = new JsonObject();
		config.add("resource", resourceConfig);
		resourceConfig.addProperty("brazilCpf", "138830383");

		JsonObject consentConfig = new JsonObject();
		config.add("consent", consentConfig);
		consentConfig.addProperty("productType", "business");

		env.putObject("config", config);

		JsonObject client = new JsonObject();
		client.addProperty("scope", "openid flibble");
		env.putObject("client", client);

		condition.execute(env);

		JsonObject consents = env.getObject("consent_endpoint_request").getAsJsonObject("data");
		JsonArray permissions = consents.getAsJsonArray("permissions");

		assertTrue(permissions.contains(new JsonPrimitive("RESOURCES_READ")));

	}

	@Test
	public void providedPermissionIsUsed() {

		JsonObject config = new JsonObject();
		JsonObject resourceConfig = new JsonObject();
		config.add("resource", resourceConfig);
		resourceConfig.addProperty("brazilCpf", "138830383");

		String[] permissionsToSet = { "ACCOUNTS_READ_BALANCE" };
		env.putString("consent_permissions", String.join(" ", permissionsToSet));

		env.putObject("config", config);

		condition.execute(env);

		JsonObject consents = env.getObject("consent_endpoint_request").getAsJsonObject("data");
		JsonArray permissions = consents.getAsJsonArray("permissions");

		assertTrue(permissions.contains(new JsonPrimitive("ACCOUNTS_READ_BALANCE")));

	}

	@Test
	public void multipleProvidedPermissionsAreUsed() {

		JsonObject config = new JsonObject();
		JsonObject resourceConfig = new JsonObject();
		config.add("resource", resourceConfig);
		resourceConfig.addProperty("brazilCpf", "138830383");

		String[] permissionsToSet = { "ACCOUNTS_READ_BALANCE", "ACCOUNTS_TRANSACTIONS_READ" };
		env.putString("consent_permissions", String.join(" ", permissionsToSet));

		env.putObject("config", config);

		condition.execute(env);

		JsonObject consents = env.getObject("consent_endpoint_request").getAsJsonObject("data");
		JsonArray permissions = consents.getAsJsonArray("permissions");

		assertThat(permissions.size(), is(2));
		assertTrue(permissions.contains(new JsonPrimitive("ACCOUNTS_READ_BALANCE")));
		assertTrue(permissions.contains(new JsonPrimitive("ACCOUNTS_TRANSACTIONS_READ")));

	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void omitsBusinessEntityWhenCnpjIsAbsent(boolean openInsurance) {
		assertPersonalConsentRequest(createConsentRequest(null, openInsurance));
	}
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void omitsBusinessEntityWhenCnpjIsEmpty(boolean openInsurance) {
		assertPersonalConsentRequest(createConsentRequest("", openInsurance));
	}
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void includesBusinessEntityWhenCnpjIsConfigured(boolean openInsurance) {
		JsonObject request = createConsentRequest("44471172000119", openInsurance);
		assertEquals(JsonParser.parseString("""
			{
				"data": {
					"permissions": ["CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ", "RESOURCES_READ"],
					"loggedUser": {"document": {"identification": "34560863008", "rel": "CPF"}},
					"businessEntity": {"document": {"identification": "44471172000119", "rel": "CNPJ"}}
				}
			}
			"""), request);
	}

	private JsonObject createConsentRequest(String cnpj, boolean openInsurance) {
		JsonObject resource = new JsonObject();
		resource.addProperty("brazilCpf", "34560863008");
		if (cnpj != null) {
			resource.addProperty("brazilCnpj", cnpj);
		}
		JsonObject config = new JsonObject();
		config.add("resource", resource);
		env.putObject("config", config);
		env.putString("consent_permissions", "CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ RESOURCES_READ");

		AbstractCondition create = openInsurance ? new FAPIBrazilOpenInsuranceCreateConsentRequest() : condition;
		create.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		create.execute(env);

		assertEquals(JsonParser.parseString("""
			{"requested_permissions": ["CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ", "RESOURCES_READ"]}
			"""), env.getObject("brazil_consent"));
		return env.getObject("consent_endpoint_request");
	}

	private void assertPersonalConsentRequest(JsonObject request) {
		assertEquals(JsonParser.parseString("""
			{
				"data": {
					"permissions": ["CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ", "RESOURCES_READ"],
					"loggedUser": {"document": {"identification": "34560863008", "rel": "CPF"}}
				}
			}
			"""), request);
	}

	@BeforeEach
	public void setUp() throws Exception {
		condition = new FAPIBrazilOpenBankingCreateConsentRequest();

		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

}
