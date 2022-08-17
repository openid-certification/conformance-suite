package net.openid.conformance.raidiam.validators.users;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: ****
 * Api endpoint: /users/{UserEmailId}
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory  Get Users")
public class GetUsersResponseValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public GetUsersResponseValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertInnerFields(body);
		return environment;
	}

	private void assertInnerFields(JsonElement body) {
		assertField(body,
			new BooleanField
				.Builder("SuperUser")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("SuperUser")
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("BasicInformation")
				.setValidator(this::assertBasicInfo)
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("OrgAccessDetails")
				.setValidator(this::assertOrgAccessDetails)
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("DirectoryTermsAndConditionsDetails")
				.setValidator(this::assertTermsAndConditionsDetails)
				.setOptional()
				.build());
	}



	private void assertTermsAndConditionsDetails(JsonObject termsAndConditionsDetails) {
		assertField(termsAndConditionsDetails,
			new BooleanField
				.Builder("RequiresSigning")
				.setOptional()
				.build());

		assertField(termsAndConditionsDetails,
			new BooleanField
				.Builder("Updated")
				.setOptional()
				.build());

		assertField(termsAndConditionsDetails,
			new ObjectField
				.Builder("TermsAndConditionsItem")
				.setValidator(parts::assertTermsAndConditionsItem)
				.setOptional()
				.build());
	}

	private void assertOrgAccessDetails(JsonObject orgAccessDetails) {
		for (String key : orgAccessDetails.keySet()) {
			assertField(orgAccessDetails,
				new ObjectField
					.Builder(key)
					.setValidator(data -> {
						assertField(data,
							new StringField
								.Builder("OrgRegistrationNumber")
								.setOptional()
								.build());

						assertField(data,
							new BooleanField
								.Builder("OrgAdmin")
								.setOptional()
								.build());

						parts.assertDomainRoleDetails(data);
					})
					.setOptional()
					.build());
		}
	}

	private void assertBasicInfo(JsonObject basicInfo) {
		assertField(basicInfo,
			new StringField
				.Builder("UserEmail")
				.setOptional()
				.build());

		assertField(basicInfo, CommonFields.getStatus());
	}
}
