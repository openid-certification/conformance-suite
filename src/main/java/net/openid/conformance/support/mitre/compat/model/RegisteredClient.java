package net.openid.conformance.support.mitre.compat.model;

import com.google.gson.JsonObject;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWT;
import net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity;
import net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity.AppType;
import net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity.AuthMethod;
import net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity.SubjectType;
import org.springframework.security.core.GrantedAuthority;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * @author jricher
 *
 */
public class RegisteredClient {

	// these fields are needed in addition to the ones in ClientDetailsEntity
	private String registrationAccessToken;
	private String registrationClientUri;
	private Date clientSecretExpiresAt;
	private Date clientIdIssuedAt;
	private ClientDetailsEntity client;
	private JsonObject src;

	/**
	 *
	 */
	public RegisteredClient() {
		this.client = new ClientDetailsEntity();
	}

	/**
	 * @param client
	 */
	public RegisteredClient(ClientDetailsEntity client) {
		this.client = client;
	}

	/**
	 * @param client
	 * @param registrationAccessToken
	 * @param registrationClientUri
	 */
	public RegisteredClient(ClientDetailsEntity client, String registrationAccessToken, String registrationClientUri) {
		this.client = client;
		this.registrationAccessToken = registrationAccessToken;
		this.registrationClientUri = registrationClientUri;
	}

	/**
	 * @return the client
	 */
	public ClientDetailsEntity getClient() {
		return client;
	}
	/**
	 * @param client the client to set
	 */
	public void setClient(ClientDetailsEntity client) {
		this.client = client;
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getClientDescription()
	 */
	public String getClientDescription() {
		return client.getClientDescription();
	}
	/**
	 * @param clientDescription
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setClientDescription(java.lang.String)
	 */
	public void setClientDescription(String clientDescription) {
		client.setClientDescription(clientDescription);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#isAllowRefresh()
	 */
	public boolean isAllowRefresh() {
		return client.isAllowRefresh();
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#isReuseRefreshToken()
	 */
	public boolean isReuseRefreshToken() {
		return client.isReuseRefreshToken();
	}
	/**
	 * @param reuseRefreshToken
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setReuseRefreshToken(boolean)
	 */
	public void setReuseRefreshToken(boolean reuseRefreshToken) {
		client.setReuseRefreshToken(reuseRefreshToken);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getIdTokenValiditySeconds()
	 */
	public Integer getIdTokenValiditySeconds() {
		return client.getIdTokenValiditySeconds();
	}
	/**
	 * @param idTokenValiditySeconds
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setIdTokenValiditySeconds(java.lang.Integer)
	 */
	public void setIdTokenValiditySeconds(Integer idTokenValiditySeconds) {
		client.setIdTokenValiditySeconds(idTokenValiditySeconds);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#isDynamicallyRegistered()
	 */
	public boolean isDynamicallyRegistered() {
		return client.isDynamicallyRegistered();
	}
	/**
	 * @param dynamicallyRegistered
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setDynamicallyRegistered(boolean)
	 */
	public void setDynamicallyRegistered(boolean dynamicallyRegistered) {
		client.setDynamicallyRegistered(dynamicallyRegistered);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#isAllowIntrospection()
	 */
	public boolean isAllowIntrospection() {
		return client.isAllowIntrospection();
	}
	/**
	 * @param allowIntrospection
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setAllowIntrospection(boolean)
	 */
	public void setAllowIntrospection(boolean allowIntrospection) {
		client.setAllowIntrospection(allowIntrospection);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#isSecretRequired()
	 */
	public boolean isSecretRequired() {
		return client.isSecretRequired();
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#isScoped()
	 */
	public boolean isScoped() {
		return client.isScoped();
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getClientId()
	 */
	public String getClientId() {
		return client.getClientId();
	}
	/**
	 * @param clientId
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setClientId(java.lang.String)
	 */
	public void setClientId(String clientId) {
		client.setClientId(clientId);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getClientSecret()
	 */
	public String getClientSecret() {
		return client.getClientSecret();
	}
	/**
	 * @param clientSecret
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setClientSecret(java.lang.String)
	 */
	public void setClientSecret(String clientSecret) {
		client.setClientSecret(clientSecret);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getScope()
	 */
	public Set<String> getScope() {
		return client.getScope();
	}
	/**
	 * @param scope
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setScope(java.util.Set)
	 */
	public void setScope(Set<String> scope) {
		client.setScope(scope);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getGrantTypes()
	 */
	public Set<String> getGrantTypes() {
		return client.getGrantTypes();
	}
	/**
	 * @param grantTypes
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setGrantTypes(java.util.Set)
	 */
	public void setGrantTypes(Set<String> grantTypes) {
		client.setGrantTypes(grantTypes);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getAuthorizedGrantTypes()
	 */
	public Set<String> getAuthorizedGrantTypes() {
		return client.getAuthorizedGrantTypes();
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getAuthorities()
	 */
	public Set<GrantedAuthority> getAuthorities() {
		return client.getAuthorities();
	}
	/**
	 * @param authorities
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setAuthorities(java.util.Set)
	 */
	public void setAuthorities(Set<GrantedAuthority> authorities) {
		client.setAuthorities(authorities);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getAccessTokenValiditySeconds()
	 */
	public Integer getAccessTokenValiditySeconds() {
		return client.getAccessTokenValiditySeconds();
	}
	/**
	 * @param accessTokenValiditySeconds
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setAccessTokenValiditySeconds(java.lang.Integer)
	 */
	public void setAccessTokenValiditySeconds(Integer accessTokenValiditySeconds) {
		client.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getRefreshTokenValiditySeconds()
	 */
	public Integer getRefreshTokenValiditySeconds() {
		return client.getRefreshTokenValiditySeconds();
	}
	/**
	 * @param refreshTokenValiditySeconds
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setRefreshTokenValiditySeconds(java.lang.Integer)
	 */
	public void setRefreshTokenValiditySeconds(Integer refreshTokenValiditySeconds) {
		client.setRefreshTokenValiditySeconds(refreshTokenValiditySeconds);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getRedirectUris()
	 */
	public Set<String> getRedirectUris() {
		return client.getRedirectUris();
	}
	/**
	 * @param redirectUris
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setRedirectUris(java.util.Set)
	 */
	public void setRedirectUris(Set<String> redirectUris) {
		client.setRedirectUris(redirectUris);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getRegisteredRedirectUri()
	 */
	public Set<String> getRegisteredRedirectUri() {
		return client.getRegisteredRedirectUri();
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getResourceIds()
	 */
	public Set<String> getResourceIds() {
		return client.getResourceIds();
	}
	/**
	 * @param resourceIds
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setResourceIds(java.util.Set)
	 */
	public void setResourceIds(Set<String> resourceIds) {
		client.setResourceIds(resourceIds);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getAdditionalInformation()
	 */
	public Map<String, Object> getAdditionalInformation() {
		return client.getAdditionalInformation();
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getApplicationType()
	 */
	public AppType getApplicationType() {
		return client.getApplicationType();
	}
	/**
	 * @param applicationType
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setApplicationType(net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity.AppType)
	 */
	public void setApplicationType(AppType applicationType) {
		client.setApplicationType(applicationType);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getClientName()
	 */
	public String getClientName() {
		return client.getClientName();
	}
	/**
	 * @param clientName
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setClientName(java.lang.String)
	 */
	public void setClientName(String clientName) {
		client.setClientName(clientName);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getTokenEndpointAuthMethod()
	 */
	public AuthMethod getTokenEndpointAuthMethod() {
		return client.getTokenEndpointAuthMethod();
	}
	/**
	 * @param tokenEndpointAuthMethod
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setTokenEndpointAuthMethod(net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity.AuthMethod)
	 */
	public void setTokenEndpointAuthMethod(AuthMethod tokenEndpointAuthMethod) {
		client.setTokenEndpointAuthMethod(tokenEndpointAuthMethod);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getSubjectType()
	 */
	public SubjectType getSubjectType() {
		return client.getSubjectType();
	}
	/**
	 * @param subjectType
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setSubjectType(net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity.SubjectType)
	 */
	public void setSubjectType(SubjectType subjectType) {
		client.setSubjectType(subjectType);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getContacts()
	 */
	public Set<String> getContacts() {
		return client.getContacts();
	}
	/**
	 * @param contacts
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setContacts(java.util.Set)
	 */
	public void setContacts(Set<String> contacts) {
		client.setContacts(contacts);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getLogoUri()
	 */
	public String getLogoUri() {
		return client.getLogoUri();
	}
	/**
	 * @param logoUri
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setLogoUri(java.lang.String)
	 */
	public void setLogoUri(String logoUri) {
		client.setLogoUri(logoUri);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getPolicyUri()
	 */
	public String getPolicyUri() {
		return client.getPolicyUri();
	}
	/**
	 * @param policyUri
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setPolicyUri(java.lang.String)
	 */
	public void setPolicyUri(String policyUri) {
		client.setPolicyUri(policyUri);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getClientUri()
	 */
	public String getClientUri() {
		return client.getClientUri();
	}
	/**
	 * @param clientUri
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setClientUri(java.lang.String)
	 */
	public void setClientUri(String clientUri) {
		client.setClientUri(clientUri);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getTosUri()
	 */
	public String getTosUri() {
		return client.getTosUri();
	}
	/**
	 * @param tosUri
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setTosUri(java.lang.String)
	 */
	public void setTosUri(String tosUri) {
		client.setTosUri(tosUri);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getJwksUri()
	 */
	public String getJwksUri() {
		return client.getJwksUri();
	}
	/**
	 * @param jwksUri
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setJwksUri(java.lang.String)
	 */
	public void setJwksUri(String jwksUri) {
		client.setJwksUri(jwksUri);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getJwks()
	 */
	public JWKSet getJwks() {
		return client.getJwks();
	}

	/**
	 * @param jwks
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setJwks(com.nimbusds.jose.jwk.JWKSet)
	 */
	public void setJwks(JWKSet jwks) {
		client.setJwks(jwks);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getSectorIdentifierUri()
	 */
	public String getSectorIdentifierUri() {
		return client.getSectorIdentifierUri();
	}
	/**
	 * @param sectorIdentifierUri
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setSectorIdentifierUri(java.lang.String)
	 */
	public void setSectorIdentifierUri(String sectorIdentifierUri) {
		client.setSectorIdentifierUri(sectorIdentifierUri);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getDefaultMaxAge()
	 */
	public Integer getDefaultMaxAge() {
		return client.getDefaultMaxAge();
	}
	/**
	 * @param defaultMaxAge
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setDefaultMaxAge(java.lang.Integer)
	 */
	public void setDefaultMaxAge(Integer defaultMaxAge) {
		client.setDefaultMaxAge(defaultMaxAge);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getRequireAuthTime()
	 */
	public Boolean getRequireAuthTime() {
		return client.getRequireAuthTime();
	}
	/**
	 * @param requireAuthTime
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setRequireAuthTime(java.lang.Boolean)
	 */
	public void setRequireAuthTime(Boolean requireAuthTime) {
		client.setRequireAuthTime(requireAuthTime);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getResponseTypes()
	 */
	public Set<String> getResponseTypes() {
		return client.getResponseTypes();
	}
	/**
	 * @param responseTypes
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setResponseTypes(java.util.Set)
	 */
	public void setResponseTypes(Set<String> responseTypes) {
		client.setResponseTypes(responseTypes);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getDefaultACRvalues()
	 */
	public Set<String> getDefaultACRvalues() {
		return client.getDefaultACRvalues();
	}
	/**
	 * @param defaultACRvalues
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setDefaultACRvalues(java.util.Set)
	 */
	public void setDefaultACRvalues(Set<String> defaultACRvalues) {
		client.setDefaultACRvalues(defaultACRvalues);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getInitiateLoginUri()
	 */
	public String getInitiateLoginUri() {
		return client.getInitiateLoginUri();
	}
	/**
	 * @param initiateLoginUri
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setInitiateLoginUri(java.lang.String)
	 */
	public void setInitiateLoginUri(String initiateLoginUri) {
		client.setInitiateLoginUri(initiateLoginUri);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getPostLogoutRedirectUris()
	 */
	public Set<String> getPostLogoutRedirectUris() {
		return client.getPostLogoutRedirectUris();
	}
	/**
	 * @param postLogoutRedirectUri
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setPostLogoutRedirectUris(Set)
	 */
	public void setPostLogoutRedirectUris(Set<String> postLogoutRedirectUri) {
		client.setPostLogoutRedirectUris(postLogoutRedirectUri);
	}
	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getRequestUris()
	 */
	public Set<String> getRequestUris() {
		return client.getRequestUris();
	}
	/**
	 * @param requestUris
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setRequestUris(java.util.Set)
	 */
	public void setRequestUris(Set<String> requestUris) {
		client.setRequestUris(requestUris);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getRequestObjectSigningAlg()
	 */
	public JWSAlgorithm getRequestObjectSigningAlg() {
		return client.getRequestObjectSigningAlg();
	}

	/**
	 * @param requestObjectSigningAlg
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setRequestObjectSigningAlg(com.nimbusds.jose.JWSAlgorithm)
	 */
	public void setRequestObjectSigningAlg(JWSAlgorithm requestObjectSigningAlg) {
		client.setRequestObjectSigningAlg(requestObjectSigningAlg);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getUserInfoSignedResponseAlg()
	 */
	public JWSAlgorithm getUserInfoSignedResponseAlg() {
		return client.getUserInfoSignedResponseAlg();
	}

	/**
	 * @param userInfoSignedResponseAlg
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setUserInfoSignedResponseAlg(com.nimbusds.jose.JWSAlgorithm)
	 */
	public void setUserInfoSignedResponseAlg(JWSAlgorithm userInfoSignedResponseAlg) {
		client.setUserInfoSignedResponseAlg(userInfoSignedResponseAlg);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getUserInfoEncryptedResponseAlg()
	 */
	public JWEAlgorithm getUserInfoEncryptedResponseAlg() {
		return client.getUserInfoEncryptedResponseAlg();
	}

	/**
	 * @param userInfoEncryptedResponseAlg
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setUserInfoEncryptedResponseAlg(com.nimbusds.jose.JWEAlgorithm)
	 */
	public void setUserInfoEncryptedResponseAlg(JWEAlgorithm userInfoEncryptedResponseAlg) {
		client.setUserInfoEncryptedResponseAlg(userInfoEncryptedResponseAlg);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getUserInfoEncryptedResponseEnc()
	 */
	public EncryptionMethod getUserInfoEncryptedResponseEnc() {
		return client.getUserInfoEncryptedResponseEnc();
	}

	/**
	 * @param userInfoEncryptedResponseEnc
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setUserInfoEncryptedResponseEnc(com.nimbusds.jose.EncryptionMethod)
	 */
	public void setUserInfoEncryptedResponseEnc(EncryptionMethod userInfoEncryptedResponseEnc) {
		client.setUserInfoEncryptedResponseEnc(userInfoEncryptedResponseEnc);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getIdTokenSignedResponseAlg()
	 */
	public JWSAlgorithm getIdTokenSignedResponseAlg() {
		return client.getIdTokenSignedResponseAlg();
	}

	/**
	 * @param idTokenSignedResponseAlg
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setIdTokenSignedResponseAlg(com.nimbusds.jose.JWSAlgorithm)
	 */
	public void setIdTokenSignedResponseAlg(JWSAlgorithm idTokenSignedResponseAlg) {
		client.setIdTokenSignedResponseAlg(idTokenSignedResponseAlg);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getIdTokenEncryptedResponseAlg()
	 */
	public JWEAlgorithm getIdTokenEncryptedResponseAlg() {
		return client.getIdTokenEncryptedResponseAlg();
	}

	/**
	 * @param idTokenEncryptedResponseAlg
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setIdTokenEncryptedResponseAlg(com.nimbusds.jose.JWEAlgorithm)
	 */
	public void setIdTokenEncryptedResponseAlg(JWEAlgorithm idTokenEncryptedResponseAlg) {
		client.setIdTokenEncryptedResponseAlg(idTokenEncryptedResponseAlg);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getIdTokenEncryptedResponseEnc()
	 */
	public EncryptionMethod getIdTokenEncryptedResponseEnc() {
		return client.getIdTokenEncryptedResponseEnc();
	}

	/**
	 * @param idTokenEncryptedResponseEnc
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setIdTokenEncryptedResponseEnc(com.nimbusds.jose.EncryptionMethod)
	 */
	public void setIdTokenEncryptedResponseEnc(EncryptionMethod idTokenEncryptedResponseEnc) {
		client.setIdTokenEncryptedResponseEnc(idTokenEncryptedResponseEnc);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getTokenEndpointAuthSigningAlg()
	 */
	public JWSAlgorithm getTokenEndpointAuthSigningAlg() {
		return client.getTokenEndpointAuthSigningAlg();
	}

	/**
	 * @param tokenEndpointAuthSigningAlg
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setTokenEndpointAuthSigningAlg(com.nimbusds.jose.JWSAlgorithm)
	 */
	public void setTokenEndpointAuthSigningAlg(JWSAlgorithm tokenEndpointAuthSigningAlg) {
		client.setTokenEndpointAuthSigningAlg(tokenEndpointAuthSigningAlg);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getCreatedAt()
	 */
	public Date getCreatedAt() {
		return client.getCreatedAt();
	}
	/**
	 * @param createdAt
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setCreatedAt(java.util.Date)
	 */
	public void setCreatedAt(Date createdAt) {
		client.setCreatedAt(createdAt);
	}
	/**
	 * @return the registrationAccessToken
	 */
	public String getRegistrationAccessToken() {
		return registrationAccessToken;
	}
	/**
	 * @param registrationAccessToken the registrationAccessToken to set
	 */
	public void setRegistrationAccessToken(String registrationAccessToken) {
		this.registrationAccessToken = registrationAccessToken;
	}
	/**
	 * @return the registrationClientUri
	 */
	public String getRegistrationClientUri() {
		return registrationClientUri;
	}
	/**
	 * @param registrationClientUri the registrationClientUri to set
	 */
	public void setRegistrationClientUri(String registrationClientUri) {
		this.registrationClientUri = registrationClientUri;
	}
	/**
	 * @return the clientSecretExpiresAt
	 */
	public Date getClientSecretExpiresAt() {
		return clientSecretExpiresAt;
	}
	/**
	 * @param expiresAt the clientSecretExpiresAt to set
	 */
	public void setClientSecretExpiresAt(Date expiresAt) {
		this.clientSecretExpiresAt = expiresAt;
	}
	/**
	 * @return the clientIdIssuedAt
	 */
	public Date getClientIdIssuedAt() {
		return clientIdIssuedAt;
	}
	/**
	 * @param issuedAt the clientIdIssuedAt to set
	 */
	public void setClientIdIssuedAt(Date issuedAt) {
		this.clientIdIssuedAt = issuedAt;
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getClaimsRedirectUris()
	 */
	public Set<String> getClaimsRedirectUris() {
		return client.getClaimsRedirectUris();
	}

	/**
	 * @param claimsRedirectUris
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setClaimsRedirectUris(java.util.Set)
	 */
	public void setClaimsRedirectUris(Set<String> claimsRedirectUris) {
		client.setClaimsRedirectUris(claimsRedirectUris);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getSoftwareStatement()
	 */
	public JWT getSoftwareStatement() {
		return client.getSoftwareStatement();
	}

	/**
	 * @param softwareStatement
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setSoftwareStatement(com.nimbusds.jwt.JWT)
	 */
	public void setSoftwareStatement(JWT softwareStatement) {
		client.setSoftwareStatement(softwareStatement);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getCodeChallengeMethod()
	 */
	public PKCEAlgorithm getCodeChallengeMethod() {
		return client.getCodeChallengeMethod();
	}

	/**
	 * @param codeChallengeMethod
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setCodeChallengeMethod(PKCEAlgorithm)
	 */
	public void setCodeChallengeMethod(PKCEAlgorithm codeChallengeMethod) {
		client.setCodeChallengeMethod(codeChallengeMethod);
	}

	/**
	 * @return the src
	 */
	public JsonObject getSource() {
		return src;
	}

	/**
	 * @param src the src to set
	 */
	public void setSource(JsonObject src) {
		this.src = src;
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getDeviceCodeValiditySeconds()
	 */
	public Integer getDeviceCodeValiditySeconds() {
		return client.getDeviceCodeValiditySeconds();
	}

	/**
	 * @param deviceCodeValiditySeconds
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setDeviceCodeValiditySeconds(java.lang.Integer)
	 */
	public void setDeviceCodeValiditySeconds(Integer deviceCodeValiditySeconds) {
		client.setDeviceCodeValiditySeconds(deviceCodeValiditySeconds);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getSoftwareId()
	 */
	public String getSoftwareId() {
		return client.getSoftwareId();
	}

	/**
	 * @param softwareId
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setSoftwareId(java.lang.String)
	 */
	public void setSoftwareId(String softwareId) {
		client.setSoftwareId(softwareId);
	}

	/**
	 * @return
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#getSoftwareVersion()
	 */
	public String getSoftwareVersion() {
		return client.getSoftwareVersion();
	}

	/**
	 * @param softwareVersion
	 * @see net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity#setSoftwareVersion(java.lang.String)
	 */
	public void setSoftwareVersion(String softwareVersion) {
		client.setSoftwareVersion(softwareVersion);
	}



}
