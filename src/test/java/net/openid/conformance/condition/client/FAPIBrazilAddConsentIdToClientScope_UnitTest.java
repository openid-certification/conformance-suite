package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(MockitoExtension.class)
public class FAPIBrazilAddConsentIdToClientScope_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIBrazilAddConsentIdToClientScope condition;

	@Test
	public void addsConsentAsScopeIfProvided() {

		JsonObject clientConfig = new JsonObject();
		clientConfig.addProperty("scope", "openid");

		env.putObject("client", clientConfig);
		env.putString("consent_id", "consent_id");

		condition.execute(env);
		String scope = OIDFJSON.getString(clientConfig.get("scope"));

		assertThat(scope, equalTo("openid consent:consent_id"));

	}

	@Test
	public void errorsCorrectlyIfScopeMissing() {

		JsonObject clientConfig = new JsonObject();

		env.putObject("client", clientConfig);
		env.putString("consent_id", "consent_id");

		try {
			condition.execute(env);
		} catch(ConditionError ce) {
			assertThat(ce.getMessage(), equalTo("FAPIBrazilAddConsentIdToClientScope: scope missing in client object"));
		}

	}

	@Test
	public void errorsCorrectlyIfScopeEmpty() {

		JsonObject clientConfig = new JsonObject();
		clientConfig.addProperty("scope", "");

		env.putObject("client", clientConfig);
		env.putString("consent_id", "consent_id");

		try {
			condition.execute(env);
		} catch(ConditionError ce) {
			assertThat(ce.getMessage(), equalTo("FAPIBrazilAddConsentIdToClientScope: scope empty in client object"));
		}

	}

	@BeforeEach
	public void setUp() throws Exception {
		condition = new FAPIBrazilAddConsentIdToClientScope();

		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

}
