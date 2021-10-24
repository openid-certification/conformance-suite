package net.openid.conformance.raidiam.validators.organisations;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * This class corresponds to {@link GetOrganisationsEssPollValidator}
 * Api url: ****
 * Api endpoint: GET /organisations/ess/poll
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Get Organisations Ess Poll")
public class GetOrganisationsEssPollValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> ENVELOPE_STATUS = Sets.newHashSet("completed", "created", "declined",
		"deleted", "delivered", "processing", "sent", "signed", "template", "voided", "expired");

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(data -> {
					assertField(data,
						new StringField
							.Builder("OrganisationId")
							.setMinLength(1)
							.setMaxLength(40)
							.build());

					assertField(data,
						new StringField
							.Builder("EssEnvelopeId")
							.build());

					assertField(data,
						new StringField
							.Builder("ExternalSigningServiceEnvelopeStatus")
							.setEnums(ENVELOPE_STATUS)
							.setOptional()
							.build());
				})
				.build());

		return environment;
	}
}
