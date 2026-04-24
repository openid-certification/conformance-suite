package net.openid.conformance.condition.as;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AddVP1FinalDCQLVPTokenToAuthorizationEndpointResponseParams_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddVP1FinalDCQLVPTokenToAuthorizationEndpointResponseParams cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AddVP1FinalDCQLVPTokenToAuthorizationEndpointResponseParams();
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
		env.putObjectFromJsonString(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY, dcql);
		env.putString("credential", "test_credential");
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());

		cond.execute(env);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println(gson.toJson(env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY)));

		assertThat(OIDFJSON.getString(env.getElementFromObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, "vp_token.my_credential").getAsJsonArray().get(0))).isEqualTo("test_credential");


	}

	@Test
	public void testEvaluate_unambiguousRequiredCredentialSetUsesRequiredCredentialId() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "real_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["urn:eudi:pid:1"]
			      }
			    },
			    {
			      "id": "optional_fake",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["urn:conformance-suite:nonexistent:credential:1"]
			      }
			    }
			  ],
			  "credential_sets": [
			    {
			      "options": [["real_credential"]],
			      "required": true
			    },
			    {
			      "options": [["optional_fake"]],
			      "required": false
			    }
			  ]
			}
			""";
		env.putObjectFromJsonString(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY, dcql);
		env.putString("credential", "test_credential");
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());

		cond.execute(env);

		assertThat(OIDFJSON.getString(env.getElementFromObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, "vp_token.real_credential").getAsJsonArray().get(0)))
			.isEqualTo("test_credential");
	}

	@Test
	public void testEvaluate_multipleCredentialsWithoutCredentialSetsFails() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "credential_one",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["urn:eudi:pid:1"]
			      }
			    },
			    {
			      "id": "credential_two",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["urn:eudi:pid:1"]
			      }
			    }
			  ]
			}
			""";
		env.putObjectFromJsonString(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY, dcql);
		env.putString("credential", "test_credential");
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_multipleRequiredCredentialIdsFails() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "credential_one",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["urn:eudi:pid:1"]
			      }
			    },
			    {
			      "id": "credential_two",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["urn:eudi:pid:1"]
			      }
			    }
			  ],
			  "credential_sets": [
			    {
			      "options": [["credential_one"]],
			      "required": true
			    },
			    {
			      "options": [["credential_two"]],
			      "required": true
			    }
			  ]
			}
			""";
		env.putObjectFromJsonString(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY, dcql);
		env.putString("credential", "test_credential");
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
