package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureUnsignedPAREndpointRequestDoesNotContainRequestParameter_UnitTest {
	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureUnsignedPAREndpointRequestDoesNotContainRequestParameter cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureUnsignedPAREndpointRequestDoesNotContainRequestParameter();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_plainParameters() {

		JsonObject parRequest = JsonParser.parseString(
		"""
		{
			"body_form_params" : {
				"client_id" : "client",
				"response_type" : "code",
				"redirect_uri" : "https://client.example.com/cb",
				"code_challenge" : "abcd1234",
				"code_challenge_method" : "S256"
			}
		}
		""").getAsJsonObject();

		env.putObject("par_endpoint_http_request", parRequest);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noParameters() {

		env.putObject("par_endpoint_http_request", new JsonObject());
		cond.execute(env);
	}

	@Test
	public void testEvaluate_requestObject() {

		JsonObject parRequest = JsonParser.parseString(
		"""
		{
			"body_form_params" : {
				"client_id" : "client",
				"request" : "abcd1234"
			}
		}
		""").getAsJsonObject();

		env.putObject("par_endpoint_http_request", parRequest);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_requestObjectAlongsidePlainParameters() {

		JsonObject parRequest = JsonParser.parseString(
		"""
		{
			"body_form_params" : {
				"client_id" : "client",
				"response_type" : "code",
				"redirect_uri" : "https://client.example.com/cb",
				"request" : "abcd1234"
			}
		}
		""").getAsJsonObject();

		env.putObject("par_endpoint_http_request", parRequest);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
