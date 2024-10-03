package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AustraliaConnectIdEnsureIdTokenContainsTrustFramework_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AustraliaConnectIdEnsureIdTokenContainsTrustFramework cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AustraliaConnectIdEnsureIdTokenContainsTrustFramework();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_validTrustFramework() {
		JsonObject idToken = JsonParser.parseString(
		"""
		{
		  "claims": {
		    "verified_claims": {
		      "verification": {
		        "trust_framework": "au_connectid"
		      }
		    }
		  }
		}
		""").getAsJsonObject();

		env.putObject("id_token", idToken);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noTrustFramework() {
		assertThrows(ConditionError.class, () -> {
			JsonObject idToken = JsonParser.parseString(
			"""
			{
			  "claims": {
			    "verified_claims": {
			      "verification": {
			      }
			    }
			  }
			}
			""").getAsJsonObject();

			env.putObject("id_token", idToken);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_invalidTrustFramework() {
		assertThrows(ConditionError.class, () -> {
			JsonObject idToken = JsonParser.parseString(
			"""
			{
			  "claims": {
			    "verified_claims": {
			      "verification": {
			        "trust_framework": "invalid"
			      }
			    }
			  }
			}
			""").getAsJsonObject();

			env.putObject("id_token", idToken);
			cond.execute(env);
		});
	}
}
