package net.openid.conformance.raidiam;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;

public class OrgApiValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject response = bodyFrom(environment);

		assertJsonField(response, "$.OrgDetails.OrganisationId", "e1ebedfc-7600-46af-a30a-180a1e6f49c1");
		assertJsonField(response, "$.OrgDetails.Status", "Active");
		assertJsonField(response, "$.OrgDetails.OrganisationName", "CECME DO GRUPO PAO ACUCAR/BSB");
		assertJsonField(response, "$.OrgDetails.CreatedOn", "2020-12-30T13:43:08.900Z");
		assertJsonField(response, "$.OrgDetails.LegalEntityName", "COOPERATIVA DE ECONOMIA E CRÃDITO MÃTUO DOS EMPREGADOS DO GRUPO PAO DE ACUCAR - BRASILIA - LTDA.");
		assertJsonField(response, "$.OrgDetails.CountryOfRegistration", "BR");
		assertJsonField(response, "$.OrgDetails.CompanyRegister", "Cadastro Nacional da Pessoa JurÃ­dica");
		assertJsonField(response, "$.OrgDetails.RegistrationNumber", "00543108");
		assertJsonField(response, "$.OrgDetails.RegistrationId", "00543108");
		assertJsonField(response, "$.OrgDetails.RegisteredName", "COOPERATIVA DE ECONOMIA E CRÃDITO MÃTUO DOS EMPREGADOS DO GRUPO PAO DE ACUCAR - BRASILIA - LTDA.");
		assertJsonField(response, "$.OrgDetails.AddressLine1", "ADDRESS TBC");
		assertJsonField(response, "$.OrgDetails.AddressLine2", "ADDRESS TBC");
		assertJsonField(response, "$.OrgDetails.City", "BrasÃ­lia");
		assertJsonField(response, "$.OrgDetails.Postcode", "ADDRESS TBC");
		assertJsonField(response, "$.OrgDetails.Country", "BR");
		assertJsonField(response, "$.OrgDetails.RequiresParticipantTermsAndConditionsSigning", true);

		logSuccess("Org response looks good");
		return environment;
	}

}

