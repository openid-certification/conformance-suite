package net.openid.conformance.raidiam.validators;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

public class CommonParts {

	private final AbstractJsonAssertingCondition validator;
	private static final Set<String> TYPES = Sets.newHashSet("Directory", "Participant");
	private static final Set<String> CONTACT_ROLE = Sets.newHashSet("PTC", "STC", "PBC", "SBC", "PSDC", "SSDC", "PDRC", "SDRC", "PPC", "SPC", "PCPC", "SCPC");
	private static final Set<String> SYSTEM = Sets.newHashSet("Directory", "Service Desk", "Dispute Resolution", "Portal", "Centralized Platform");
	private static final Set<String> DIRECTION = Sets.newHashSet("ASC", "DESC");
	private static final Set<String> STATUS = Sets.newHashSet("Active", "Pending", "Withdrawn");
	private static final Set<String> DOMAIN_STATUS = Sets.newHashSet("Active", "Inactive");
	public static final Set<String> ENVELOPE_STATUS = Sets.newHashSet("completed", "created", "declined",
		"deleted", "delivered", "processing", "sent", "signed", "template", "voided", "expired");
	private static final Set<String> CONTRACT_TYPES = Sets.newHashSet("Business", "Technical",
		"Billing", "Incident", "Security");
	private static final Set<String> WEBHOOK_STATUS = Sets.newHashSet("Confirmed", "Pending", "Deactivated");
	private static final Set<String> MODE = Sets.newHashSet("Live", "Test");

	public CommonParts(AbstractJsonAssertingCondition validator) {
		this.validator = validator;
	}

	public void assertTermsAndConditionsItem(JsonObject termsAndConditionsItem) {
		validator.assertField(termsAndConditionsItem,
			new IntField
				.Builder("TnCId")
				.setOptional()
				.build());

		validator.assertField(termsAndConditionsItem,
			new StringField
				.Builder("Name")
				.setMinLength(1)
				.build());

		validator.assertField(termsAndConditionsItem,
			new StringField
				.Builder("Type")
				.setEnums(TYPES)
				.build());

		validator.assertField(termsAndConditionsItem,
			new StringField
				.Builder("Content")
				.setMinLength(1)
				.build());

		validator.assertField(termsAndConditionsItem, CommonFields.getStatus());

		validator.assertField(termsAndConditionsItem,
			new ObjectField
				.Builder("ExternalSigningService")
				.setValidator(this::assertExternalSigningService)
				.setOptional()
				.build());
	}

	private void assertExternalSigningService(JsonObject externalSigningService) {
		validator.assertField(externalSigningService,
			new StringField
				.Builder("ExternalSigningServiceName")
				.setEnums(Sets.newHashSet("DocuSign"))
				.setOptional()
				.build());

		validator.assertField(externalSigningService,
			new ObjectField
				.Builder("ExternalSigningServiceSignerTemplateConfig")
				.setValidator(this::assertExternalSigningServiceSignerTemplateConfig)
				.setOptional()
				.build());

		validator.assertField(externalSigningService,
			new StringField
				.Builder("ExternalSigningServiceSubject")
				.setOptional()
				.setMinLength(1)
				.build());

		validator.assertField(externalSigningService,
			new StringField
				.Builder("ExternalSigningServiceEmailSubject")
				.setMinLength(1)
				.setMaxLength(100)
				.build());
	}

	private void assertExternalSigningServiceSignerTemplateConfig(JsonObject body) {
		//1
		validator.assertField(body,
			new StringField
				.Builder("Signer1TemplateId")
				.setOptional()
				.setMinLength(1)
				.build());

		validator.assertField(body,
			new IntField
				.Builder("Signer1Version")
				.setOptional()
				.build());
		//2
		validator.assertField(body,
			new StringField
				.Builder("Signer2TemplateId")
				.setOptional()
				.setMinLength(1)
				.build());

		validator.assertField(body,
			new IntField
				.Builder("Signer2Version")
				.setOptional()
				.build());

		//3
		validator.assertField(body,
			new StringField
				.Builder("Signer3TemplateId")
				.setOptional()
				.setMinLength(1)
				.build());

		validator.assertField(body,
			new IntField
				.Builder("Signer3Version")
				.setOptional()
				.build());

		//4
		validator.assertField(body,
			new StringField
				.Builder("Signer4TemplateId")
				.setOptional()
				.setMinLength(1)
				.build());

		validator.assertField(body,
			new IntField
				.Builder("Signer4Version")
				.setOptional()
				.build());

		//5
		validator.assertField(body,
			new StringField
				.Builder("Signer5TemplateId")
				.setOptional()
				.setMinLength(1)
				.build());

		validator.assertField(body,
			new IntField
				.Builder("Signer5Version")
				.setOptional()
				.build());

		//6
		validator.assertField(body,
			new StringField
				.Builder("Signer6TemplateId")
				.setOptional()
				.setMinLength(1)
				.build());

		validator.assertField(body,
			new IntField
				.Builder("Signer6Version")
				.setOptional()
				.build());
	}

	public void assertDomainRoleDetails(JsonObject orgAccessDetails) {
		validator.assertField(orgAccessDetails,
			new ObjectArrayField
				.Builder("DomainRoleDetails")
				.setValidator(role -> {
					validator.assertField(role,
						new StringField
							.Builder("AuthorisationDomainName")
							.setOptional()
							.build());

					validator.assertField(role,
						new StringField
							.Builder("AuthorisationDomainRoleName")
							.setOptional()
							.build());
						assertBasicRoleField(role);
				})
				.setOptional()
				.build());
	}

	public void assertAuthority(JsonObject body) {
		validator.assertField(body,
			new StringField
				.Builder("AuthorityId")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(body,
			new StringField
				.Builder("AuthorityName")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(body,
			new StringField
				.Builder("AuthorityCode")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(body,
			new StringField
				.Builder("AuthorityUri")
				.setOptional()
				.build());

		validator.assertField(body,
			new StringField
				.Builder("AuthorityCountry")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(body, CommonFields.getStatus());
	}

	public void organisationDomainUsersContent(JsonObject content) {
		validator.assertField(content,
			new StringField
				.Builder("AuthorisationDomainUserId")
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("Email")
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("AuthorisationDomain")
				.setOptional()
				.build());
		validator.assertField(content,
			new StringField
				.Builder("AuthorisationDomainRole")
				.setOptional()
				.build());

		assertBasicRoleField(content);
	}

	public void organisationContent(JsonObject content) {
		validator.assertField(content,
			new StringField
				.Builder("OrganisationId")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("Status")
				.setEnums(STATUS)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("OrganisationName")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("CreatedOn")
				.setMaxLength(30)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("LegalEntityName")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("CountryOfRegistration")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("CompanyRegister")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringArrayField
				.Builder("Tags")
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("Size")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("RegistrationNumber")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("RegistrationId")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("RegisteredName")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("AddressLine1")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("AddressLine2")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("City")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("Postcode")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("Country")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("ParentOrganisationReference")
				.setMaxLength(65535)
				.setOptional()
				.build());

		validator.assertField(content,
			new BooleanField
				.Builder("RequiresParticipantTermsAndConditionsSigning")
				.setOptional()
				.build());
	}

	private void assertBasicRoleField(JsonObject domainRoleDetails) {
		validator.assertField(domainRoleDetails,
			new StringField
				.Builder("Status")
				.setEnums(CommonFields.STATUS)
				.setOptional()
				.build());

		validator.assertField(domainRoleDetails,
			new StringField
				.Builder("ContactRole")
				.setEnums(CONTACT_ROLE)
				.setOptional()
				.build());

		validator.assertField(domainRoleDetails,
			new StringField
				.Builder("System")
				.setEnums(SYSTEM)
				.setOptional()
				.build());
	}

	public void assertDefaultResponseFields(JsonObject body) {
		validator.assertField(body,
			new IntField
				.Builder("totalPages")
				.setOptional()
				.build());

		validator.assertField(body,
			new IntField
				.Builder("totalSize")
				.setOptional()
				.build());

		validator.assertField(body,
			new ObjectField
				.Builder("pageable")
				.setValidator(this::assertPageable)
				.setOptional()
				.build());

		validator.assertField(body,
			new IntField
				.Builder("numberOfElements")
				.setOptional()
				.build());

		validator.assertField(body,
			new IntField
				.Builder("size")
				.setOptional()
				.build());

		validator.assertField(body,
			new IntField
				.Builder("offset")
				.setOptional()
				.build());

		validator.assertField(body,
			new BooleanField
				.Builder("empty")
				.setOptional()
				.build());

		validator.assertField(body,
			new IntField
				.Builder("pageNumber")
				.setOptional()
				.build());
	}
	private void assertPageable(JsonObject page) {
					validator.assertField(page,
						new IntField
							.Builder("number")
							.setOptional()
							.build());

					validator.assertField(page,
						new ObjectField
							.Builder("sort")
							.setValidator(this::assertSort)
							.setOptional()
							.build());

					validator.assertField(page,
						new IntField
							.Builder("size")
							.setOptional()
							.build());

					validator.assertField(page,
						new IntField
							.Builder("offset")
							.setOptional()
							.build());

					validator.assertField(page,
						new BooleanField
							.Builder("sorted")
							.setOptional()
							.build());
	}

	private void assertSort(JsonObject sort) {
		validator.assertField(sort,
			new BooleanField
				.Builder("sorted")
				.setOptional()
				.build());

		validator.assertField(sort,
			new ObjectArrayField
				.Builder("orderBy")
				.setValidator(this::assertOrderBy)
				.setOptional()
				.build());
	}

	private void assertOrderBy(JsonObject orderBy) {
		validator.assertField(orderBy,
			new StringField
				.Builder("property")
				.setOptional()
				.build());

		validator.assertField(orderBy,
			new StringField
				.Builder("direction")
				.setEnums(DIRECTION)
				.setOptional()
				.build());

		validator.assertField(orderBy,
			new BooleanField
				.Builder("ignoreCase")
				.setOptional()
				.build());

		validator.assertField(orderBy,
			new BooleanField
				.Builder("ascending")
				.setOptional()
				.build());
	}

	public void assertOrgTermsAndConditionsDetail(JsonObject data) {
		validator.assertField(data,
			new StringField
				.Builder("InitiatedBy")
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("Role")
				.setOptional()
				.build());

		validator.assertField(data,
			new ObjectField
				.Builder("TermsAndConditionsDetail")
				.setValidator(this::assertTermsAndConditionsDetail)
				.setOptional()
				.build());
	}

	public void assertTermsAndConditionsDetail(JsonObject data) {
		validator.assertField(data,
			new ObjectField
				.Builder("TermsAndConditionsItem")
				.setValidator(this::assertTermsAndConditionsItem)
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("InititatedDate")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("ExternalSigningServiceEnvelopeId")
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("ExternalSigningServiceEnvelopeStatus")
				.setEnums(ENVELOPE_STATUS)
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("ExternalSigningServiceEnvelopePasscode")
				.setOptional()
				.build());
	}

	public void assertOrgDomainClaims(JsonObject data) {
		validator.assertField(data,
			new StringField
				.Builder("OrganisationAuthorityDomainClaimId")
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("AuthorisationDomainName")
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("AuthorityId")
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("AuthorityName")
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("RegistrationId")
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("Status")
				.setEnums(DOMAIN_STATUS)
				.setOptional()
				.build());
	}

	public void assertOrgDomainRoleClaims(JsonObject data) {
		validator.assertField(data,
			new StringField
				.Builder("OrganisationId")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("OrganisationAuthorityClaimId")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("AuthorityId")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("Status")
				.setEnums(DOMAIN_STATUS)
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("AuthorisationDomain")
				.setMaxLength(30)
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("Role")
				.setMaxLength(30)
				.setOptional()
				.build());

		validator.assertField(data,
			new ObjectArrayField
				.Builder("Authorisations")
				.setValidator(auth -> {
					validator.assertField(auth,
						new StringField
							.Builder("Status")
							.setEnums(DOMAIN_STATUS)
							.setOptional()
							.build());

					validator.assertField(auth,
						new StringField
							.Builder("MemberState")
							.setMaxLength(2)
							.setOptional()
							.build());
				})
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("RegistrationId")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(data,
			new StringArrayField
				.Builder("UniqueTechnicalIdenifier")
				.setMaxLength(255)
				.setOptional()
				.build());
	}

	public void assertExportContacts(JsonObject content) {
		validator.assertField(content,
			new StringField
				.Builder("ContactId")
				.setOptional()
				.build());

		validator.assertField(content, CommonFields.getOrganisationId());

		validator.assertField(content,
			new StringField
				.Builder("ContactType")
				.setEnums(CONTRACT_TYPES)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("FirstName")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("LastName")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("Department")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("EmailAddress")
				.setMaxLength(255)
				//.setPattern("^(.{1,}@[^.]{1,}).*") TODO: Pattern is not work
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("PhoneNumber")
				.setMaxLength(18)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("AddressLine1")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("AddressLine2")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("City")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("Postcode")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("Country")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("AdditionalInformation")
				.setMaxLength(65535)
				.setOptional()
				.build());

		validator.assertField(content,
			new StringField
				.Builder("PgpPublicKey")
				.setMaxLength(65535)
				.setOptional()
				.build());
	}

	public void assertAuthorisationServers(JsonObject authorisationServers) {
		validator.assertField(authorisationServers,
			new StringField
				.Builder("AuthorisationServerId")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new StringField
				.Builder("OrganisationId")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new BooleanField
				.Builder("AutoRegistrationSupported")
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new ObjectArrayField
				.Builder("ApiResources")
				.setValidator(this::assertApiResources)
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new StringField
				.Builder("CustomerFriendlyDescription")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new StringField
				.Builder("CustomerFriendlyLogoUri")
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*\\.(svg)$")
				//Pattern is'nt work cos in json example we have this - ex.  "ApiEndpoint": "string"
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new StringField
				.Builder("CustomerFriendlyName")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new StringField
				.Builder("DeveloperPortalUri")
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				//Pattern is'nt work cos in json example we have this ex. "ApiEndpoint": "string"
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new StringField
				.Builder("TermsOfServiceUri")
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				//Pattern is'nt work cos in json example we have this - ex.  "ApiEndpoint": "string"
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new StringField
				.Builder("NotificationWebhook")
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				//Pattern is'nt work cos in json example we have this - ex.  "ApiEndpoint": "string"
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new StringField
				.Builder("NotificationWebhookStatus")
				.setEnums(WEBHOOK_STATUS)
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new StringField
				.Builder("OpenIDDiscoveryDocument")
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				//Pattern is'nt work cos in json example we have this - ex.  "ApiEndpoint": "string"
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new StringField
				.Builder("PayloadSigningCertLocationUri")
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				//Pattern is'nt work cos in json example we have this - ex.  "ApiEndpoint": "string"
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(authorisationServers,
			new StringField
				.Builder("ParentAuthorisationServerId")
				.setMaxLength(40)
				.setOptional()
				.build());
	}

	public void assertApiResources(JsonObject data) {
		validator.assertField(data,
			new StringField
				.Builder("ApiResourceId")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("ApiFamilyType")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(data,
			new IntField
				.Builder("ApiVersion")
				.setOptional()
				.build());

		validator.assertField(data,
			new StringField
				.Builder("ApiCertificationUri")
				.setOptional()
				.build());

		validator.assertField(data,
			new ObjectArrayField
				.Builder("ApiDiscoveryEndpoints")
				.setValidator(content -> {
					validator.assertField(content,
						new StringField
							.Builder("ApiDiscoveryId")
							.setMaxLength(40)
							.setOptional()
							.build());

					validator.assertField(content,
						new StringField
							.Builder("ApiEndpoint")
							//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
							//Pattern is'nt work cos in json example we have this - "ApiEndpoint": "string"
							.setMaxLength(255)
							.setOptional()
							.build());
				})
				.setOptional()
				.build());
	}

	public void assertSoftwareDetails(JsonObject softwareDetails) {
		validator.assertField(softwareDetails, CommonFields.getStatus());

		validator.assertField(softwareDetails,
			new StringField
				.Builder("ClientId")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new StringField
				.Builder("ClientName")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new StringField
				.Builder("Description")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new StringField
				.Builder("Environment")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(softwareDetails, CommonFields.getOrganisationId());

		validator.assertField(softwareDetails,
			new StringField
				.Builder("SoftwareStatementId")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new StringField
				.Builder("Mode")
				.setMaxLength(8)
				.setEnums(MODE)
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new BooleanField
				.Builder("RtsClientCreated")
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new StringField
				.Builder("OnBehalfOf")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new StringField
				.Builder("PolicyUri")
				.setMaxLength(255)
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new StringField
				.Builder("ClientUri")
				.setMaxLength(255)
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new StringField
				.Builder("LogoUri")
				.setMaxLength(255)
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new StringArrayField
				.Builder("RedirectUri")
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new StringField
				.Builder("TermsOfServiceUri")
				.setMaxLength(255)
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new IntField
				.Builder("Version")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new BooleanField
				.Builder("Locked")
				.setOptional()
				.build());

		validator.assertField(softwareDetails,
			new StringField
				.Builder("AdditionalSoftwareMetadata")
				.setMaxLength(255)
				.setOptional()
				.build());
	}

	public void assertSoftwareAuthorityClaims(JsonObject authorityClaims) {
		validator.assertField(authorityClaims,
			new StringField
				.Builder("SoftwareStatementId")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(authorityClaims,
			new StringField
				.Builder("SoftwareAuthorityClaimId")
				.setMaxLength(40)
				.setMinLength(1)
				.setOptional()
				.build());

		validator.assertField(authorityClaims, CommonFields.getStatus());

		validator.assertField(authorityClaims,
			new StringField
				.Builder("AuthorisationDomain")
				.setMaxLength(30)
				.setOptional()
				.build());

		validator.assertField(authorityClaims,
			new StringField
				.Builder("Role")
				.setMaxLength(10)
				.setOptional()
				.build());
	}

	public void assertCertificates(JsonObject softwareCertificates) {
		validator.assertField(softwareCertificates, CommonFields.getOrganisationId());

		validator.assertField(softwareCertificates,
			new StringArrayField
				.Builder("SoftwareStatementIds")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("ClientName")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("Status")
				.setMaxLength(40)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("ValidFromDateTime")
				.setMaxLength(30)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("ExpiryDateTime")
				.setMaxLength(30)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("e")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("keyType")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("kid")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("kty")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("n")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("use")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringArrayField
				.Builder("x5c")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("x5t")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("x5thashS256")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("x5u")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("SignedCertPath")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("JwkPath")
				.setMaxLength(255)
				.setOptional()
				.build());

		validator.assertField(softwareCertificates,
			new StringField
				.Builder("OrgJwkPath")
				.setMaxLength(255)
				.setOptional()
				.build());
	}
}
