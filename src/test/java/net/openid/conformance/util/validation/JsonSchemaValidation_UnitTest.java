package net.openid.conformance.util.validation;

import com.networknt.schema.Error;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
	public void testValidate_unknownPropertyErrorsAttributeOnlyTheFailedBranchsUnknowns() throws IOException {
		JsonSchemaValidation validation = createEkycResponseSchemaValidation();

		// The embedded-attachment oneOf branch fails only on unknown_member; the external
		// branch's additionalProperties rejections of content_type/content are sibling-branch
		// noise and must not be attributed as unknown properties.
		JsonSchemaValidationResult result = validation.validate("""
			{
			  "verified_claims": {
			  "claims": {"given_name": "Paula"},
			  "verification": {
			    "trust_framework": "de_aml",
			    "evidence": [{
			      "type": "document",
			      "attachments": [{
			        "content_type": "image/png",
			        "content": "aGVsbG8=",
			        "unknown_member": "x"
			      }]
			    }]
			  }
			  }
			}
			""");

		assertFalse(result.isValid());
		assertTrue(result.structuralErrors().isValid(),
			() -> "expected no structural errors but got: " + result.structuralErrors().getValidationMessages());
		assertEquals(List.of("unknown_member"),
			result.unknownPropertyErrors().getValidationMessages().stream().map(Error::getProperty).toList());
	}

	@Test
	public void testValidate_structuralOneOfFailureAttributesNoUnknownProperties() throws IOException {
		JsonSchemaValidation validation = createEkycResponseSchemaValidation();

		// content_type with parameters fails the embedded branch's pattern - a genuine
		// structural error, so nothing may be classified as an unknown property even though
		// the external branch rejects content_type/content as additionalProperties.
		JsonSchemaValidationResult result = validation.validate("""
			{
			  "verified_claims": {
			  "claims": {"given_name": "Paula"},
			  "verification": {
			    "trust_framework": "de_aml",
			    "evidence": [{
			      "type": "document",
			      "attachments": [{
			        "content_type": "text/plain; charset=utf-8",
			        "content": "aGVsbG8="
			      }]
			    }]
			  }
			  }
			}
			""");

		assertFalse(result.structuralErrors().isValid());
		assertTrue(result.unknownPropertyErrors().isValid(),
			() -> "expected no unknown-property errors but got: " + result.unknownPropertyErrors().getValidationMessages());
	}

	@Test
	public void testValidate_directUnevaluatedPropertyIsAttributedAsUnknown() throws IOException {
		JsonSchemaValidation validation = createEkycResponseSchemaValidation();

		// A wrong-branch field at evidence level is an unevaluated property; the enclosing
		// verified_claims anyOf branch fails solely because of it, so it is attributed as
		// an unknown property rather than treated as a structural anyOf failure.
		JsonSchemaValidationResult result = validation.validate("""
			{
			  "verified_claims": {
			  "claims": {"given_name": "Paula"},
			  "verification": {
			    "trust_framework": "de_aml",
			    "evidence": [{
			      "type": "vouch",
			      "document_details": "ignored-for-vouch"
			    }]
			  }
			  }
			}
			""");

		assertTrue(result.structuralErrors().isValid(),
			() -> "expected no structural errors but got: " + result.structuralErrors().getValidationMessages());
		assertEquals(List.of("document_details"),
			result.unknownPropertyErrors().getValidationMessages().stream().map(Error::getProperty).toList());
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

	@Test
	public void testValidate_ignoreUnknownPropertyStrictnessFailsLoudlyOnUnresolvableClasspathRef() {
		// A stripped-resource: IRI that doesn't resolve must not fall through to the library's
		// default loaders (which would report the original, unrewritten IRI - or worse, fetch
		// the unstripped document from the network).
		JsonSchemaValidation validation = new JsonSchemaValidation("json-schemas/ekyc-ida/verified_claims.json");
		validation.setSchemaBuilderCustomizer(builder -> builder.schemaIdResolvers(schemaIdResolvers ->
			schemaIdResolvers.mapPrefix(EKYC_SCHEMA_PREFIX, "resource:json-schemas/does-not-exist/")));
		validation.setIgnoreUnknownPropertyStrictness(true);

		IllegalStateException e = assertThrows(IllegalStateException.class,
			() -> validation.validate(AGGREGATED_CLAIMS_WITH_WRONG_BRANCH_FIELD));
		assertTrue(e.getMessage().contains("json-schemas/does-not-exist/verified_claims_request.json"),
			() -> "unexpected message: " + e.getMessage());
	}
}
