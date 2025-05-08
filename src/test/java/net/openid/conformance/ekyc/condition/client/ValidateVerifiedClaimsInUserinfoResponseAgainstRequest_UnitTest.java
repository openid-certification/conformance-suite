package net.openid.conformance.ekyc.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateVerifiedClaimsInUserinfoResponseAgainstRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateVerifiedClaimsInUserinfoResponseAgainstRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateVerifiedClaimsInUserinfoResponseAgainstRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	protected void runTestWithStringData(String requestData, String responseData) throws IOException {
		env.putObjectFromJsonString("authorization_endpoint_request", "claims.userinfo.verified_claims", requestData);
		env.putObjectFromJsonString("verified_claims_response", "userinfo", responseData);
		cond.execute(env);

	}


	protected void runTest(String requestFilename, String responseFilename) throws IOException {
		String requestJson = IOUtils.resourceToString(requestFilename, StandardCharsets.UTF_8, getClass().getClassLoader());
		String responseJson = IOUtils.resourceToString(responseFilename, StandardCharsets.UTF_8, getClass().getClassLoader());
		runTestWithStringData(requestJson, responseJson);
	}

	@Test
	public void testEvaluate_noError() throws Exception {
		String requestFilename = "ValidateVerifiedClaimsInUserinfoResponseAgainstRequest/request.json";
		String responseFilename = "ValidateVerifiedClaimsInUserinfoResponseAgainstRequest/response.json";

		runTest(requestFilename, responseFilename);
	}

	@Test
	public void testEvaluateSingleAssuranceDetails_noError() throws Exception {
		String requestJson = """
		{
		  "claims": {
			"place_of_birth": null,
			"dogname": null,
			"birthdate": null,
			"address": null,
			"nationalities": null,
			"given_name": null,
			"family_name": null
		  },
		  "verification": {
			"trust_framework": {
			  "value": "de_aml"
			},
			"verification_process": {
			  "value": "7df98de2-8834-48fb-b817-ee087cad8497"
			},
			"assurance_level": {
			   "value": "low"
			},
			"assurance_process": {
				"policy": {
					"value": "somepolicy1"
				},
				"procedure": {
					"value": "someprocedure1"
				},
				"assurance_details": [
					{
						"assurance_type": {
							"value": "some_assurancetype1"
						},
						"assurance_classification": {
							"value": "some_classification1"
						}
					}
				]
			},
			"evidence": [
			  {
				"type": {
				  "value": "id_document"
				},
				"document": {
				  "type": null
				}
			  }
			]
		  }
		}""";
		String responseJson = """
		{
		  "claims": {
			"place_of_birth": {
			  "locality": "Berlin",
			  "country": "DE"
			},
			"birthdate": "1950-01-01",
			"address": {
			  "street_address": "Street1 1",
			  "country": "DE",
			  "formatted": "Street1 1\nBerlin\n10243\nDE",
			  "locality": "Berlin",
			  "region": "Berlin",
			  "postal_code": "10243"
			},
			"nationalities": [
			  "DE"
			],
			"given_name": "Given001",
			"family_name": "Family001"
		  },
		  "verification": {
			"trust_framework": "de_aml",
			"assurance_level": "low",
			"assurance_process": {
				"policy": "somepolicy1",
				"procedure": "someprocedure1",
				"assurance_details": [
					{
						"assurance_type": "some_assurancetype1",
						"assurance_classification": "some_classification1"
					}
				]
			  },
			  "evidence": [
			  {
				"document": {
				  "type": "idcard"
				},
				"type": "id_document"
			  }
			],
			"verification_process": "7df98de2-8834-48fb-b817-ee087cad8497"
		  }
		}""";

		runTestWithStringData(requestJson, responseJson);
	}

	@Test
	public void testEvaluateSingleAssuranceDetails_expectError() throws Exception {
		String requestJson = """
		{
		  "claims": {
			"place_of_birth": null,
			"dogname": null,
			"birthdate": null,
			"address": null,
			"nationalities": null,
			"given_name": null,
			"family_name": null
		  },
		  "verification": {
			"trust_framework": {
			  "value": "de_aml"
			},
			"verification_process": {
			  "value": "7df98de2-8834-48fb-b817-ee087cad8497"
			},
			"assurance_level": {
			   "value": "low"
			},
			"assurance_process": {
				"policy": {
					"value": "somepolicy1"
				},
				"procedure": {
					"value": "someprocedure1"
				},
				"assurance_details": [
					{
						"assurance_type": {
							"value": "some_assurancetype1"
						},
						"assurance_classification": {
							"value": "some_classification1"
						}
					}
				]
			},
			"evidence": [
			  {
				"type": {
				  "value": "id_document"
				},
				"document": {
				  "type": null
				}
			  }
			]
		  }
		}""";
		String responseJson = """
		{
		  "claims": {
			"place_of_birth": {
			  "locality": "Berlin",
			  "country": "DE"
			},
			"birthdate": "1950-01-01",
			"address": {
			  "street_address": "Street1 1",
			  "country": "DE",
			  "formatted": "Street1 1\nBerlin\n10243\nDE",
			  "locality": "Berlin",
			  "region": "Berlin",
			  "postal_code": "10243"
			},
			"nationalities": [
			  "DE"
			],
			"given_name": "Given001",
			"family_name": "Family001"
		  },
		  "verification": {
			"trust_framework": "de_aml",
			"assurance_level": "low",
			"assurance_process": {
				"policy": "somepolicy1",
				"procedure": "someprocedure1",
				"assurance_details": [
					{
						"assurance_type": "some_assurancetype1",
						"assurance_classification": "some_classification2"
					}
				]
			  },
			  "evidence": [
			  {
				"document": {
				  "type": "idcard"
				},
				"type": "id_document"
			  }
			],
			"verification_process": "7df98de2-8834-48fb-b817-ee087cad8497"
		  }
		}""";

		assertThrows(ConditionError.class, () -> {
			runTestWithStringData(requestJson, responseJson);
		});
	}


	@Test
	public void testEvaluateSingleAssuranceDetailsRequestMultipleResponse_noError() throws Exception {
		String requestJson = """
		{
		  "claims": {
			"place_of_birth": null,
			"dogname": null,
			"birthdate": null,
			"address": null,
			"nationalities": null,
			"given_name": null,
			"family_name": null
		  },
		  "verification": {
			"trust_framework": {
			  "value": "de_aml"
			},
			"verification_process": {
			  "value": "7df98de2-8834-48fb-b817-ee087cad8497"
			},
			"assurance_level": {
			   "value": "low"
			},
			"assurance_process": {
				"policy": {
					"value": "somepolicy1"
				},
				"procedure": {
					"value": "someprocedure1"
				},
				"assurance_details": [
					{
						"assurance_type": {
							"value": "some_assurancetype1"
						},
						"assurance_classification": {
							"value": "some_classification1"
						}
					}
				]
			},
			"evidence": [
			  {
				"type": {
				  "value": "id_document"
				},
				"document": {
				  "type": null
				}
			  }
			]
		  }
		}""";
		String responseJson = """
		{
		  "claims": {
			"place_of_birth": {
			  "locality": "Berlin",
			  "country": "DE"
			},
			"birthdate": "1950-01-01",
			"address": {
			  "street_address": "Street1 1",
			  "country": "DE",
			  "formatted": "Street1 1\nBerlin\n10243\nDE",
			  "locality": "Berlin",
			  "region": "Berlin",
			  "postal_code": "10243"
			},
			"nationalities": [
			  "DE"
			],
			"given_name": "Given001",
			"family_name": "Family001"
		  },
		  "verification": {
			"trust_framework": "de_aml",
			"assurance_level": "low",
			"assurance_process": {
				"policy": "somepolicy1",
				"procedure": "someprocedure1",
				"assurance_details": [
					{
						"assurance_type": "some_invalid_assurancetype0",
						"assurance_classification": "some_invalid_classification0"
					},
					{
						"assurance_type": "some_assurancetype1",
						"assurance_classification": "some_classification1"
					}
				]
			  },
			  "evidence": [
			  {
				"document": {
				  "type": "idcard"
				},
				"type": "id_document"
			  }
			],
			"verification_process": "7df98de2-8834-48fb-b817-ee087cad8497"
		  }
		}""";

		runTestWithStringData(requestJson, responseJson);
	}

	@Test
	public void testEvaluateMultipleAssuranceDetailsRequestMultipleResponse_noError() throws Exception {
		String requestJson = """
		{
		  "claims": {
			"place_of_birth": null,
			"dogname": null,
			"birthdate": null,
			"address": null,
			"nationalities": null,
			"given_name": null,
			"family_name": null
		  },
		  "verification": {
			"trust_framework": {
			  "value": "de_aml"
			},
			"verification_process": {
			  "value": "7df98de2-8834-48fb-b817-ee087cad8497"
			},
			"assurance_level": {
			   "value": "low"
			},
			"assurance_process": {
				"policy": {
					"value": "somepolicy1"
				},
				"procedure": {
					"value": "someprocedure1"
				},
				"assurance_details": [
					{
						"assurance_type": {
							"value": "some_assurancetype1"
						},
						"assurance_classification": {
							"value": "some_classification1"
						}
					},
					{
						"assurance_type": {
							"value": "some_assurancetype2"
						},
						"assurance_classification": {
							"value": "some_classification2"
						}
					}
				]
			},
			"evidence": [
			  {
				"type": {
				  "value": "id_document"
				},
				"document": {
				  "type": null
				}
			  }
			]
		  }
		}""";
		String responseJson = """
		{
		  "claims": {
			"place_of_birth": {
			  "locality": "Berlin",
			  "country": "DE"
			},
			"birthdate": "1950-01-01",
			"address": {
			  "street_address": "Street1 1",
			  "country": "DE",
			  "formatted": "Street1 1\nBerlin\n10243\nDE",
			  "locality": "Berlin",
			  "region": "Berlin",
			  "postal_code": "10243"
			},
			"nationalities": [
			  "DE"
			],
			"given_name": "Given001",
			"family_name": "Family001"
		  },
		  "verification": {
			"trust_framework": "de_aml",
			"assurance_level": "low",
			"assurance_process": {
				"policy": "somepolicy1",
				"procedure": "someprocedure1",
				"assurance_details": [
					{
						"assurance_type": "some_invalid_assurancetype0",
						"assurance_classification": "some_invalid_classification0"
					},
					{
						"assurance_type": "some_assurancetype1",
						"assurance_classification": "some_classification1"
					},
					{
						"assurance_type": "some_assurancetype2",
						"assurance_classification": "some_classification2"
					}
				]
			  },
			  "evidence": [
			  {
				"document": {
				  "type": "idcard"
				},
				"type": "id_document"
			  }
			],
			"verification_process": "7df98de2-8834-48fb-b817-ee087cad8497"
		  }
		}""";

		runTestWithStringData(requestJson, responseJson);
	}


	@Test
	public void testEvaluateMultipleAssuranceDetailsRequestMultipleResponse_expectError() throws Exception {
		String requestJson = """
		{
		  "claims": {
			"place_of_birth": null,
			"dogname": null,
			"birthdate": null,
			"address": null,
			"nationalities": null,
			"given_name": null,
			"family_name": null
		  },
		  "verification": {
			"trust_framework": {
			  "value": "de_aml"
			},
			"verification_process": {
			  "value": "7df98de2-8834-48fb-b817-ee087cad8497"
			},
			"assurance_level": {
			   "value": "low"
			},
			"assurance_process": {
				"policy": {
					"value": "somepolicy1"
				},
				"procedure": {
					"value": "someprocedure1"
				},
				"assurance_details": [
					{
						"assurance_type": {
							"value": "some_assurancetype1"
						},
						"assurance_classification": {
							"value": "some_classification1"
						}
					},
					{
						"assurance_type": {
							"value": "some_assurancetype2"
						},
						"assurance_classification": {
							"value": "some_classification2"
						}
					}
				]
			},
			"evidence": [
			  {
				"type": {
				  "value": "id_document"
				},
				"document": {
				  "type": null
				}
			  }
			]
		  }
		}""";
		String responseJson = """
		{
		  "claims": {
			"place_of_birth": {
			  "locality": "Berlin",
			  "country": "DE"
			},
			"birthdate": "1950-01-01",
			"address": {
			  "street_address": "Street1 1",
			  "country": "DE",
			  "formatted": "Street1 1\nBerlin\n10243\nDE",
			  "locality": "Berlin",
			  "region": "Berlin",
			  "postal_code": "10243"
			},
			"nationalities": [
			  "DE"
			],
			"given_name": "Given001",
			"family_name": "Family001"
		  },
		  "verification": {
			"trust_framework": "de_aml",
			"assurance_level": "low",
			"assurance_process": {
				"policy": "somepolicy1",
				"procedure": "someprocedure1",
				"assurance_details": [
					{
						"assurance_type": "some_invalid_assurancetype0",
						"assurance_classification": "some_invalid_classification0"
					},
					{
						"assurance_type": "some_invalid_assurancetype1",
						"assurance_classification": "some_invalid_classification1"
					},
					{
						"assurance_type": "some_invalid_assurancetype2",
						"assurance_classification": "some_invalid_classification2"
					}
				]
			  },
			  "evidence": [
			  {
				"document": {
				  "type": "idcard"
				},
				"type": "id_document"
			  }
			],
			"verification_process": "7df98de2-8834-48fb-b817-ee087cad8497"
		  }
		}""";

		assertThrows(ConditionError.class, () -> {
			runTestWithStringData(requestJson, responseJson);
		});
	}


	@Test
	public void testEvaluateEssentialAssuranceTypeMultipleResponse_noError() throws Exception {
		String requestJson = """
		{
		  "claims": {
			"place_of_birth": null,
			"dogname": null,
			"birthdate": null,
			"address": null,
			"nationalities": null,
			"given_name": null,
			"family_name": null
		  },
		  "verification": {
			"trust_framework": {
			  "value": "de_aml"
			},
			"verification_process": {
			  "value": "7df98de2-8834-48fb-b817-ee087cad8497"
			},
			"assurance_level": {
			   "value": "low"
			},
			"assurance_process": {
				"policy": {
					"value": "somepolicy1"
				},
				"procedure": {
					"value": "someprocedure1"
				},
				"assurance_details": [
					{
						"assurance_type": {
							"essential": true
						},
						"assurance_classification": {
							"essential": true
						}
					}
				]
			},
			"evidence": [
			  {
				"type": {
				  "value": "id_document"
				},
				"document": {
				  "type": null
				}
			  }
			]
		  }
		}""";
		String responseJson = """
		{
		  "claims": {
			"place_of_birth": {
			  "locality": "Berlin",
			  "country": "DE"
			},
			"birthdate": "1950-01-01",
			"address": {
			  "street_address": "Street1 1",
			  "country": "DE",
			  "formatted": "Street1 1\nBerlin\n10243\nDE",
			  "locality": "Berlin",
			  "region": "Berlin",
			  "postal_code": "10243"
			},
			"nationalities": [
			  "DE"
			],
			"given_name": "Given001",
			"family_name": "Family001"
		  },
		  "verification": {
			"trust_framework": "de_aml",
			"assurance_level": "low",
			"assurance_process": {
				"policy": "somepolicy1",
				"procedure": "someprocedure1",
				"assurance_details": [
					{
						"assurance_type1": "some_assurancetype1",
						"assurance_classification": "some_classification1"
					},
					{
						"assurance_type": "some_assurancetype1",
						"assurance_classification": "some_classification1"
					}
				]
			  },
			  "evidence": [
			  {
				"document": {
				  "type": "idcard"
				},
				"type": "id_document"
			  }
			],
			"verification_process": "7df98de2-8834-48fb-b817-ee087cad8497"
		  }
		}""";

		runTestWithStringData(requestJson, responseJson);
	}

}
