package net.openid.conformance.raidiam.validators.authorisationServers.certifications;

import com.google.gson.JsonElement;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This class corresponds to {@link PostServerCertificationsValidator}
 * Api endpoint: GET /organisations/{OrganisationId}/authorisationservers/{AuthorisationServerId}/certifications/{AuthorisationServerCertificationId}
 */
@ApiName("Raidiam Directory GET Authorisation Server Certifications By CertificationId")
public class GetServerCertificationsByCertificationIdValidator extends PostServerCertificationsValidator {

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertServerCertifications(body);
		return environment;
	}
}
