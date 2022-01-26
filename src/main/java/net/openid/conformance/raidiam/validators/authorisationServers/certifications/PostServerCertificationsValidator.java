package net.openid.conformance.raidiam.validators.authorisationServers.certifications;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.StringField;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/authorisationservers/{AuthorisationServerId}/certifications
 */
@ApiName("Raidiam Directory POST Authorisation Server Certifications")
public class PostServerCertificationsValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertServerCertifications(body);
		return environment;
	}

	protected void assertServerCertifications(JsonElement body) {
		assertField(body,
			new StringField
				.Builder("DateOfCertification")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("CertificationId")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("AuthorisationServerId")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(body, CommonFields.getStatus());

		assertField(body,
			new StringField
				.Builder("CertificationType")
				.setOptional()
				.build());

		assertField(body,
			new IntField
				.Builder("CertificationVersion")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("CertificationPayloadURI")
				.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());
	}
}

