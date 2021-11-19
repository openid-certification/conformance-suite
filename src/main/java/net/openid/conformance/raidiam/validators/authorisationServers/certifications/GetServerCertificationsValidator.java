package net.openid.conformance.raidiam.validators.authorisationServers.certifications;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * This class corresponds to {@link PostServerCertificationsValidator}
 * Api endpoint: GET /organisations/{OrganisationId}/authorisationservers/{AuthorisationServerId}/certifications
 */
@ApiName("Raidiam Directory GET Authorisation Server Certifications")
public class GetServerCertificationsValidator extends PostServerCertificationsValidator {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertServerCertifications)
				.setOptional()
				.build());

		return environment;
	}
}
