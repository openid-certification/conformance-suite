package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class FAPIBrasilCreateConsentRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

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

	@BeforeEach
	public void setUp() throws Exception {
		condition = new FAPIBrazilOpenBankingCreateConsentRequest();

		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

}
