package net.openid.conformance.util.validation;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonSchemaValidation_UnitTest {

	private static final String EKYC_SCHEMA_PREFIX = "https://bitbucket.org/openid/ekyc-ida/raw/master/schema/";
	private static final String EKYC_RESOURCE_PREFIX = "resource:json-schemas/ekyc-ida/";

	/**
	 * The response schema's _claim_names support delegates to
	 * verified_claims_request.json#/$defs/verified_claims_def through a cross-document $ref, and
	 * the request schema uses "unevaluatedProperties": false on evidence. The wrong-branch
	 * document_details on vouch evidence below only violates that strictness, exercising the
	 * cross-document path of the strictness stripping.
	 */
	private static final String AGGREGATED_CLAIMS_WITH_WRONG_BRANCH_FIELD = """
		{
		  "_claim_names": {"verified_claims": {"src1": {
		    "verification": {
		      "trust_framework": null,
		      "evidence": [{"type": {"value": "vouch"}, "document_details": "wrong-branch"}]
		    },
		    "claims": {"given_name": null}
		  }}},
		  "_claim_sources": {"src1": {"endpoint": "https://example.com/claims", "access_token": "tok"}}
		}
		""";

	private JsonSchemaValidation createEkycResponseSchemaValidation() {
		JsonSchemaValidation validation = new JsonSchemaValidation("json-schemas/ekyc-ida/verified_claims.json");
		validation.setSchemaBuilderCustomizer(builder -> builder.schemaIdResolvers(schemaIdResolvers ->
			schemaIdResolvers.mapPrefix(EKYC_SCHEMA_PREFIX, EKYC_RESOURCE_PREFIX)));
		return validation;
	}

	@Test
	public void testValidate_strictSchemaRejectsUnknownPropertyBehindCrossDocumentRef() throws IOException {
		JsonSchemaValidation validation = createEkycResponseSchemaValidation();

		JsonSchemaValidationResult result = validation.validate(AGGREGATED_CLAIMS_WITH_WRONG_BRANCH_FIELD);

		assertFalse(result.isValid());
	}

	@Test
	public void testValidate_ignoreUnknownPropertyStrictnessAppliesBehindCrossDocumentRefs() throws IOException {
		JsonSchemaValidation validation = createEkycResponseSchemaValidation();
		validation.setIgnoreUnknownPropertyStrictness(true);

		JsonSchemaValidationResult result = validation.validate(AGGREGATED_CLAIMS_WITH_WRONG_BRANCH_FIELD);

		assertTrue(result.isValid(), () -> "expected no errors but got: " + result.getValidationMessages());
	}

	@Test
	public void testValidate_ignoreUnknownPropertyStrictnessKeepsStructuralErrorsBehindCrossDocumentRefs() throws IOException {
		JsonSchemaValidation validation = createEkycResponseSchemaValidation();
		validation.setIgnoreUnknownPropertyStrictness(true);

		// trust_framework must be null or an object in request syntax; a number is a genuine
		// structural error inside the cross-document-ref'd request schema
		String structurallyInvalid = """
			{
			  "_claim_names": {"verified_claims": {"src1": {
			    "verification": {"trust_framework": 42},
			    "claims": {"given_name": null}
			  }}},
			  "_claim_sources": {"src1": {"endpoint": "https://example.com/claims", "access_token": "tok"}}
			}
			""";

		JsonSchemaValidationResult result = validation.validate(structurallyInvalid);

		assertFalse(result.isValid());
	}
}
