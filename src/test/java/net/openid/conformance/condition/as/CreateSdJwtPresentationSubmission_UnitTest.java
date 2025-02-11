package net.openid.conformance.condition.as;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateSdJwtPresentationSubmission_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateSdJwtPresentationSubmission cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CreateSdJwtPresentationSubmission();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		String pd = """
			{
			    "id": "4db74328-9e94-49bb-97b7-bbfcb2d11a06",
			    "name": "PID - Name and age verification (vc+sd-jwt)",
			    "purpose": "We need to verify your name and age",
			    "input_descriptors": [
			      {
			        "id": "fdf340c6-05f9-4125-b0fc-86a08d8ad051",
			        "format": {
			          "vc+sd-jwt": {
			            "sd-jwt_alg_values": [
			              "ES256"
			            ],
			            "kb-jwt_alg_values": [
			              "ES256"
			            ]
			          }
			        },
			        "constraints": {
			          "limit_disclosure": "required",
			          "fields": [
			            {
			              "path": [
			                "$.given_name"
			              ]
			            },
			            {
			              "path": [
			                "$.family_name"
			              ]
			            },
			            {
			              "path": [
			                "$.age_equal_or_over.21"
			              ]
			            },
			            {
			              "path": [
			                "$.vct"
			              ],
			              "filter": {
			                "type": "string",
			                "enum": [
			                  "https://example.bmi.bund.de/credential/pid/1.0",
			                  "urn:eu.europa.ec.eudi:pid:1"
			                ]
			              }
			            },
			            {
			              "path": [
			                "$.iss"
			              ],
			              "filter": {
			                "type": "string",
			                "enum": [
			                  "https://demo.pid-issuer.bundesdruckerei.de/c",
			                  "https://demo.pid-issuer.bundesdruckerei.de/c1",
			                  "https://demo.pid-issuer.bundesdruckerei.de/b1"
			                ]
			              }
			            }
			          ]
			        }
			      }
			    ]
			  }""";
		env.putObjectFromJsonString("authorization_request_object", "claims.presentation_definition", pd);

		cond.execute(env);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println(gson.toJson(env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY)));
	}

}
