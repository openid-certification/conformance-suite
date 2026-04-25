package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedParametersInCredentialIssuerMetadata_UnitTest extends AbstractVciUnitTest {

	private CheckForUnexpectedParametersInCredentialIssuerMetadata cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForUnexpectedParametersInCredentialIssuerMetadata();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_noWarningWhenNoUnknownProperties() {
		String json = """
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_endpoint": "https://credential-issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "UniversityDegreeCredential": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/UniversityDegreeCredential"
			    }
			  }
			}
			""";
		putCredentialIssuerMetadata(json);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noWarningWhenCredentialMetadataUsesKnownProperties() {
		String json = """
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_endpoint": "https://credential-issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "UniversityDegreeCredential": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/UniversityDegreeCredential",
			      "credential_metadata": {
			        "display": [
			          {
			            "name": "Identity Credential",
			            "locale": "en-US",
			            "logo": {
			              "uri": "https://example.com/logo.png",
			              "alt_text": "Credential logo"
			            },
			            "description": "University degree credential",
			            "background_color": "#FFFFFF",
			            "background_image": {
			              "uri": "https://example.com/background.png"
			            },
			            "text_color": "#000000"
			          }
			        ],
			        "claims": [
			          {
			            "path": [
			              "given_name"
			            ],
			            "mandatory": true,
			            "value_type": "string",
			            "display": [
			              {
			                "name": "Given Name",
			                "locale": "en-US"
			              }
			            ]
			          }
			        ]
			      }
			    }
			  }
			}
			""";
		putCredentialIssuerMetadata(json);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_unknownTopLevelProperty() {
		String json = """
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_endpoint": "https://credential-issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "UniversityDegreeCredential": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/UniversityDegreeCredential"
			    }
			  },
			  "unexpected_field": "boom"
			}
			""";
		putCredentialIssuerMetadata(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data, "$.unexpected_field");
	}

	@Test
	public void testEvaluate_unknownPropertyInCredentialConfiguration() {
		String json = """
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_endpoint": "https://credential-issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "UniversityDegreeCredential": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/UniversityDegreeCredential",
			      "unexpected_configuration": true
			    }
			  }
			}
			""";
		putCredentialIssuerMetadata(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data, "$.credential_configurations_supported.UniversityDegreeCredential.unexpected_configuration");
	}

	@Test
	public void testEvaluate_unknownPropertyInCredentialMetadataDisplay() {
		String json = """
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_endpoint": "https://credential-issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "UniversityDegreeCredential": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/UniversityDegreeCredential",
			      "credential_metadata": {
			        "display": [
			          {
			            "name": "Identity Credential",
			            "unexpected_display": true
			          }
			        ]
			      }
			    }
			  }
			}
			""";
		putCredentialIssuerMetadata(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data, "$.credential_configurations_supported.UniversityDegreeCredential.credential_metadata.display[0].unexpected_display");
	}

	@Test
	public void testEvaluate_unknownPropertyInCredentialMetadataLogo() {
		String json = """
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_endpoint": "https://credential-issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "UniversityDegreeCredential": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/UniversityDegreeCredential",
			      "credential_metadata": {
			        "display": [
			          {
			            "name": "Identity Credential",
			            "logo": {
			              "uri": "https://example.com/logo.png",
			              "unexpected_logo": true
			            }
			          }
			        ]
			      }
			    }
			  }
			}
			""";
		putCredentialIssuerMetadata(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data, "$.credential_configurations_supported.UniversityDegreeCredential.credential_metadata.display[0].logo.unexpected_logo");
	}

	@Test
	public void testEvaluate_unknownPropertyInCredentialMetadataBackgroundImage() {
		String json = """
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_endpoint": "https://credential-issuer.example.com/credential",
			  "credential_configurations_supported": {
			    "UniversityDegreeCredential": {
			      "format": "dc+sd-jwt",
			      "vct": "https://example.com/UniversityDegreeCredential",
			      "credential_metadata": {
			        "display": [
			          {
			            "name": "Identity Credential",
			            "background_image": {
			              "uri": "https://example.com/background.png",
			              "unexpected_background_image": true
			            }
			          }
			        ]
			      }
			    }
			  }
			}
			""";
		putCredentialIssuerMetadata(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data, "$.credential_configurations_supported.UniversityDegreeCredential.credential_metadata.display[0].background_image.unexpected_background_image");
	}

	@Test
	public void testEvaluate_structuralErrorsDoNotWarn() {
		// Missing required credential_endpoint is structural, not unknown property
		String json = """
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_configurations_supported": {
			    "UniversityDegreeCredential": {
			      "format": "dc+sd-jwt"
			    }
			  }
			}
			""";
		putCredentialIssuerMetadata(json);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	private void putCredentialIssuerMetadata(String json) {
		JsonObject metadata = JsonParser.parseString(json).getAsJsonObject();
		JsonObject vci = new JsonObject();
		vci.add("credential_issuer_metadata", metadata);
		env.putObject("vci", vci);
	}
}
