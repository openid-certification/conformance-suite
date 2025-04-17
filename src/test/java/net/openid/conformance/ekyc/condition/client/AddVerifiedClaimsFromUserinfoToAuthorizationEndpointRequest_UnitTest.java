package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.ValidationMessage;
import com.tananaev.jsonpatch.JsonPatch;
import com.tananaev.jsonpatch.JsonPatchFactory;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static net.openid.conformance.ekyc.condition.client.AbstractValidateAgainstSchema.checkRequestSchema;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AddVerifiedClaimsFromUserinfoToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddVerifiedClaimsFromUserinfoToAuthorizationEndpointRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AddVerifiedClaimsFromUserinfoToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	protected void runTestWithStringData(String jsonRequestData, String jsonResponseData) throws IOException {
		env.putObjectFromJsonString("config", "ekyc.userinfo", jsonRequestData);
		env.putObject("authorization_endpoint_request", new JsonObject());
		cond.execute(env);
		JsonObject result = env.getObject("authorization_endpoint_request");

		Set<ValidationMessage> errors = checkRequestSchema(result.get("claims").toString());
		if (!errors.isEmpty()) {
			for (ValidationMessage error: errors) {
				System.out.println("RequestJsonSchemaError: " + error.toString());
			}
		}
		assertThat(errors.size()).isEqualTo(0);

		JsonObject expected = (JsonObject) JsonParser.parseString(jsonResponseData);

		JsonPatchFactory jpf = new JsonPatchFactory();
		JsonPatch patch = jpf.create(expected, result);

		System.out.println("patch: " + patch.toString());
		assertThat(patch.size()).isEqualTo(0);
	}

	protected void runTest(String userInfoFilename, String expectedRequestFilename) throws IOException {
		String testUserInfoJson = IOUtils.resourceToString(userInfoFilename, StandardCharsets.UTF_8, getClass().getClassLoader());
		String expectedJson = IOUtils.resourceToString(expectedRequestFilename, StandardCharsets.UTF_8, getClass().getClassLoader());

		runTestWithStringData(testUserInfoJson, expectedJson);
	}

	@Test
	public void testEvaluate_noError() throws Exception {
		String userInfoFilename = "test-user-info.json";
		String expectedRequestFilename = "verified-claims-request-based-on-userinfo.json";

		runTest(userInfoFilename, expectedRequestFilename);
	}

	@Test
	public void testEvaluate_noErrorYesTest001() throws Exception {
		String userInfoFilename = "test-user-info-yes-test001.json";
		String expectedRequestFilename = "verified-claims-request-based-on-userinfo-yes-test001.json";

		runTest(userInfoFilename, expectedRequestFilename);
	}

	@Test
	public void testAssuranceProcess() throws Exception {
		String userInfoData = "{\n" +
			"    \"sub\": \"f647f683-e46d-43bd-bc76-526d93429b86\",\n" +
			"    \"verified_claims\": {\n" +
			"        \"claims\": {\n" +
			"            \"birthdate\": \"1950-01-01\"\n" +
			"        },\n" +
			"        \"verification\": {\n" +
			"            \"trust_framework\": \"de_aml\",\n" +
			"            \"verification_process\": \"vp1\",\n" +
			"            \"assurance_process\": {\n" +
			"                \"policy\": \"policy1\",\n" +
			"                \"procedure\": \"procedure1\"\n" +
			"            }\n" +
			"        }\n" +
			"    }\n" +
			"}";

		String expecteData = "{\n" +
			"    \"claims\": {\n" +
			"        \"userinfo\": {\n" +
			"            \"verified_claims\": {\n" +
			"                \"claims\": {\n" +
			"                    \"birthdate\": null\n" +
			"                },\n" +
			"                \"verification\": {\n" +
			"                    \"trust_framework\": {\n" +
			"                        \"value\": \"de_aml\"\n" +
			"                    },\n" +
			"                    \"verification_process\": {\n" +
			"                        \"value\": \"vp1\"\n" +
			"                    },\n" +
			"                    \"assurance_process\": {\n" +
			"                        \"policy\": {\n" +
			"                            \"value\": \"policy1\"\n" +
			"                        },\n" +
			"                        \"procedure\": {\n" +
			"                            \"value\": \"procedure1\"\n" +
			"                        }\n" +
			"                    }\n" +
			"                }\n" +
			"            }\n" +
			"        }\n" +
			"    }\n" +
			"}";

		runTestWithStringData(userInfoData, expecteData);
	}


	@Test
	public void testAssuranceProcessAssuranceDetails() throws Exception {
		String userInfoData = "{\n" +
			"    \"sub\": \"f647f683-e46d-43bd-bc76-526d93429b86\",\n" +
			"    \"verified_claims\": {\n" +
			"        \"claims\": {\n" +
			"            \"birthdate\": \"1950-01-01\",\n" +
			"            \"given_name\": \"Given001\",\n" +
			"            \"family_name\": \"Family001\"\n" +
			"        },\n" +
			"        \"verification\": {\n" +
			"            \"evidence\": [\n" +
			"                {\n" +
			"                    \"method\": \"pipp\",\n" +
			"                    \"type\": \"document\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"method\": \"pipp\",\n" +
			"                    \"type\": \"electronic_record\"\n" +
			"                }\n" +
			"            ],\n" +
			"            \"trust_framework\": \"de_aml\",\n" +
			"            \"verification_process\": \"vp1\",\n" +
			"            \"assurance_process\": {\n" +
			"                \"policy\": \"policy1\",\n" +
			"                \"procedure\": \"procedure1\",\n" +
			"                \"assurance_details\": [\n" +
			"                    {\n" +
			"                        \"assurance_type\": \"assurance_type1\",\n" +
			"                        \"assurance_classification\": \"assurance_classification1\",\n" +
			"                        \"evidence_ref\": [\n" +
			"                            {\n" +
			"                                \"check_id\": \"id1234\",\n" +
			"                                \"evidence_metadata\": {\n" +
			"                                    \"evidence_classification\": \"evc1\"\n" +
			"                                }\n" +
			"                            }\n" +
			"                        ]\n" +
			"                    }\n" +
			"                ]\n" +
			"            }\n" +
			"        }\n" +
			"    }\n" +
			"}";

		String expecteData = "{\n" +
			"    \"claims\": {\n" +
			"      \"userinfo\": {\n" +
			"        \"verified_claims\": {\n" +
			"          \"claims\": {\n" +
			"            \"birthdate\": null,\n" +
			"            \"given_name\": null,\n" +
			"            \"family_name\": null\n" +
			"          },\n" +
			"          \"verification\": {\n" +
			"            \"trust_framework\": {\n" +
			"              \"value\": \"de_aml\"\n" +
			"            },\n" +
			"            \"verification_process\": {\n" +
			"              \"value\": \"vp1\"\n" +
			"            },\n" +
			"            \"assurance_process\": {\n" +
			"              \"policy\": {\n" +
			"                \"value\": \"policy1\"\n" +
			"              },\n" +
			"              \"procedure\": {\n" +
			"                \"value\": \"procedure1\"\n" +
			"              },\n" +
			"              \"assurance_details\": [\n" +
			"                {\n" +
			"                  \"assurance_type\": {\n" +
			"                    \"value\": \"assurance_type1\"\n" +
			"                  },\n" +
			"                  \"assurance_classification\": {\n" +
			"                    \"value\": \"assurance_classification1\"\n" +
			"                  },\n" +
			"                  \"evidence_ref\": [\n" +
			"                    {\n" +
			"                      \"check_id\": {\n" +
			"                        \"value\": \"id1234\"\n" +
			"                      },\n" +
			"                      \"evidence_metadata\": {\n" +
			"                        \"evidence_classification\": {\n" +
			"                          \"value\": \"evc1\"\n" +
			"                        }\n" +
			"                      }\n" +
			"                    }\n" +
			"                  ]\n" +
			"                }\n" +
			"              ]\n" +
			"            },\n" +
			"            \"evidence\": [\n" +
			"              {\n" +
			"                \"type\": {\n" +
			"                  \"value\": \"document\"\n" +
			"                }\n" +
			"              },\n" +
			"              {\n" +
			"                \"type\": {\n" +
			"                  \"value\": \"electronic_record\"\n" +
			"                }\n" +
			"              }\n" +
			"            ]\n" +
			"          }\n" +
			"        }\n" +
			"      }\n" +
			"    }\n" +
			"  }";

		runTestWithStringData(userInfoData, expecteData);
	}

}
