package net.openid.conformance.openid.ssf.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OIDSSFTransmitterMetadata {

	@JsonProperty("spec_version")
	private String specVersion;

	@JsonProperty("issuer")
	private String issuer;

	@JsonProperty("jwks_uri")
	private String jwksUri;

	@JsonProperty("delivery_methods_supported")
	private Set<String> deliveryMethodSupported;

	@JsonProperty("configuration_endpoint")
	private String configurationEndpoint;

	@JsonProperty("status_endpoint")
	private String statusEndpoint;

	@JsonProperty("add_subject_endpoint")
	private String addSubjectEndpoint;

	@JsonProperty("remove_subject_endpoint")
	private String removeSubjectEndpoint;

	@JsonProperty("verification_endpoint")
	private String verificationEndpoint;

	@JsonProperty("critical_subject_members")
	private Set<String> criticalSubjectMembers;

	@JsonProperty("default_subjects")
	private String defaultSubjects;

	@JsonProperty("authorization_schemes")
	private List<Object> authorizationSchemes;

	public String getSpecVersion() {
		return specVersion;
	}

	public void setSpecVersion(String specVersion) {
		this.specVersion = specVersion;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getJwksUri() {
		return jwksUri;
	}

	public void setJwksUri(String jwksUri) {
		this.jwksUri = jwksUri;
	}

	public Set<String> getDeliveryMethodSupported() {
		return deliveryMethodSupported;
	}

	public void setDeliveryMethodSupported(Set<String> deliveryMethodSupported) {
		this.deliveryMethodSupported = deliveryMethodSupported;
	}

	public String getConfigurationEndpoint() {
		return configurationEndpoint;
	}

	public void setConfigurationEndpoint(String configurationEndpoint) {
		this.configurationEndpoint = configurationEndpoint;
	}

	public String getStatusEndpoint() {
		return statusEndpoint;
	}

	public void setStatusEndpoint(String statusEndpoint) {
		this.statusEndpoint = statusEndpoint;
	}

	public String getAddSubjectEndpoint() {
		return addSubjectEndpoint;
	}

	public void setAddSubjectEndpoint(String addSubjectEndpoint) {
		this.addSubjectEndpoint = addSubjectEndpoint;
	}

	public String getRemoveSubjectEndpoint() {
		return removeSubjectEndpoint;
	}

	public void setRemoveSubjectEndpoint(String removeSubjectEndpoint) {
		this.removeSubjectEndpoint = removeSubjectEndpoint;
	}

	public String getVerificationEndpoint() {
		return verificationEndpoint;
	}

	public void setVerificationEndpoint(String verificationEndpoint) {
		this.verificationEndpoint = verificationEndpoint;
	}

	public Set<String> getCriticalSubjectMembers() {
		return criticalSubjectMembers;
	}

	public void setCriticalSubjectMembers(Set<String> criticalSubjectMembers) {
		this.criticalSubjectMembers = criticalSubjectMembers;
	}

	public String getDefaultSubjects() {
		return defaultSubjects;
	}

	public void setDefaultSubjects(String defaultSubjects) {
		this.defaultSubjects = defaultSubjects;
	}

	public List<Object> getAuthorizationSchemes() {
		return authorizationSchemes;
	}

	public void setAuthorizationSchemes(List<Object> authorizationSchemes) {
		this.authorizationSchemes = authorizationSchemes;
	}

	@Override
	public String toString() {
		return "OIDSSFTransmitterMetadata{" +
			"specVersion='" + specVersion + '\'' +
			", issuer='" + issuer + '\'' +
			", jwksUri='" + jwksUri + '\'' +
			", deliveryMethodSupported=" + deliveryMethodSupported +
			", configurationEndpoint='" + configurationEndpoint + '\'' +
			", statusEndpoint='" + statusEndpoint + '\'' +
			", addSubjectEndpoint='" + addSubjectEndpoint + '\'' +
			", removeSubjectEndpoint='" + removeSubjectEndpoint + '\'' +
			", verificationEndpoint='" + verificationEndpoint + '\'' +
			", criticalSubjectMembers=" + criticalSubjectMembers +
			", defaultSubjects='" + defaultSubjects + '\'' +
			", authorizationSchemes=" + authorizationSchemes +
			'}';
	}
}
