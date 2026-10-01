package net.openid.conformance.condition.client;

/**
 * DCQL queries shared by the unit tests of the conditions that apply claim_sets semantics to a
 * presentation: single-element options in the Verifier's order of preference, as the suite's
 * built-in age-over-18 queries use.
 */
final class DcqlTestFixtures {

	private DcqlTestFixtures() {
	}

	static final String AGE_OVER_18_SD_JWT_DCQL = """
		{
		  "credentials": [
		    {
		      "id": "my_credential",
		      "format": "dc+sd-jwt",
		      "claims": [
		        {"id": "age_over_18", "path": ["age_equal_or_over", "18"]},
		        {"id": "age_in_years", "path": ["age_in_years"]},
		        {"id": "birthdate", "path": ["birthdate"]}
		      ],
		      "claim_sets": [["age_over_18"], ["age_in_years"], ["birthdate"]]
		    }
		  ]
		}
		""";

	static final String AGE_OVER_18_MDOC_DCQL = """
		{
		  "credentials": [
		    {
		      "id": "my_credential",
		      "format": "mso_mdoc",
		      "meta": {"doctype_value": "org.iso.18013.5.1.mDL"},
		      "claims": [
		        {"id": "age_over_18", "path": ["org.iso.18013.5.1", "age_over_18"]},
		        {"id": "age_in_years", "path": ["org.iso.18013.5.1", "age_in_years"]},
		        {"id": "birth_date", "path": ["org.iso.18013.5.1", "birth_date"]}
		      ],
		      "claim_sets": [["age_over_18"], ["age_in_years"], ["birth_date"]]
		    }
		  ]
		}
		""";
}
