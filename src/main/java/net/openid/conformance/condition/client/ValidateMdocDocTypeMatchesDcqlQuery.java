package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Validates that the mdoc credential's docType matches the DCQL query's meta.doctype_value.
 */
public class ValidateMdocDocTypeMatchesDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"mdoc", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String docType = env.getString("mdoc", "docType");
		if (docType == null) {
			throw error("mdoc credential does not contain a docType");
		}

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonObject matchingCredential = DcqlQueryUtils.findCredentialById(dcqlQuery, credentialId);

		if (matchingCredential == null) {
			throw error("No DCQL credential entry found matching credential_id",
				args("credential_id", credentialId, "dcql_query", dcqlQuery));
		}

		String doctypeValue = DcqlQueryUtils.extractMdocDoctypeValue(matchingCredential);
		if (doctypeValue == null) {
			log("DCQL credential entry has no doctype_value, skipping docType validation");
			return env;
		}

		if (!doctypeValue.equals(docType)) {
			throw error("Credential docType does not match the requested doctype_value in the DCQL query",
				args("docType", docType, "doctype_value", doctypeValue, "credential_id", credentialId));
		}

		logSuccess("Credential docType matches DCQL query",
			args("docType", docType, "doctype_value", doctypeValue));
		return env;
	}
}
