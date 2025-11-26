package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
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
public class AustraliaConnectIdEnsureMtlsAliasesContainsRequiredEndpoints_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AustraliaConnectIdEnsureMtlsAliasesContainsRequiredEndpoints cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new AustraliaConnectIdEnsureMtlsAliasesContainsRequiredEndpoints();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {

		JsonObject server = JsonParser.parseString(
		"""
		{
			"mtls_endpoint_aliases" : {
				"token_endpoint" : "https://mtls.example.com/token",
				"pushed_authorization_request_endpoint" : "https://mtls.example.com/par",
				"userinfo_endpoint" : "https://mtls.example.com/userinfo"
			}
		}
		""").getAsJsonObject();

		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noPAREndpoint() {

		JsonObject server = JsonParser.parseString(
		"""
		{
			"mtls_endpoint_aliases" : {
				"token_endpoint" : "https://mtls.example.com/token",
				"userinfo_endpoint" : "https://mtls.example.com/userinfo"
			}
		}
		""").getAsJsonObject();

		assertThrows(ConditionError.class, () -> {
			env.putObject("server", server);
			cond.execute(env);
		});

	}

	@Test
	public void testEvaluate_notPresentMtlsEndpointAliases() {

		JsonObject server = JsonParser.parseString(
		"""
		{
		}
		""").getAsJsonObject();

		assertThrows(ConditionError.class, () -> {
			env.putObject("server", server);
			cond.execute(env);
		});
	}
}
