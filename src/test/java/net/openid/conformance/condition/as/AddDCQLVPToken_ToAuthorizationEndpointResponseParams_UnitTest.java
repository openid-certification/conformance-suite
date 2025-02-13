package net.openid.conformance.condition.as;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AddDCQLVPToken_ToAuthorizationEndpointResponseParams_UnitTest {

	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddDCQLVPTokenToAuthorizationEndpointResponseParams cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AddDCQLVPTokenToAuthorizationEndpointResponseParams();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		// example taken from https://openid.github.io/OpenID4VP/openid-4-verifiable-presentations-wg-draft.html#name-examples-for-dcql-queries
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "meta": {
			        "doctype_value": "org.iso.7367.1.mVRC"
			      },
			      "claims": [
			        {"path": ["org.iso.7367.1", "vehicle_holder"]},
			        {"path": ["org.iso.18013.5.1", "first_name"]}
			      ]
			    }
			  ]
			}
			""";
		env.putObjectFromJsonString("authorization_request_object", "claims.dcql_query", dcql);
		env.putString("credential", "test_credential");
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());

		cond.execute(env);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println(gson.toJson(env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY)));

		assertThat(env.getString(CreateAuthorizationEndpointResponseParams.ENV_KEY, "vp_token.my_credential")).isEqualTo("test_credential");


	}

}
