package net.openid.conformance.security;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.converter.ClaimConversionService;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;


/**
 * This is a implementation of Spring UserService to map the role Admin to
 * users from a client with registration id, and claim "attribute" having the "value",
 * provided on the constructor
 */
public class OidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final String INVALID_USER_INFO_RESPONSE_ERROR_CODE = "invalid_user_info_response";

    private static final Converter<Map<String, Object>, Map<String, Object>> DEFAULT_CLAIM_TYPE_CONVERTER = new ClaimTypeConverter(
            createDefaultClaimTypeConverters());

    private Set<String> accessibleScopes = new HashSet<>(
            Arrays.asList(OidcScopes.PROFILE, OidcScopes.EMAIL, OidcScopes.ADDRESS, OidcScopes.PHONE));

    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService = new DefaultOAuth2UserService();

    private Function<ClientRegistration, Converter<Map<String, Object>, Map<String, Object>>> claimTypeConverterFactory = (
            clientRegistration) -> DEFAULT_CLAIM_TYPE_CONVERTER;

    private String registrationid, attribute, value;

    public OidcUserService(String registrationid, String attribute, String value) {
        this.registrationid = registrationid;
        this.attribute = attribute;
        this.value = value;
    }

    /**
     * Returns the default {@link Converter}'s used for type conversion of claim values
     * for an {@link OidcUserInfo}.
     *
     * @return a {@link Map} of {@link Converter}'s keyed by {@link StandardClaimNames
     * claim name}
     * @since 5.2
     */
    public static Map<String, Converter<Object, ?>> createDefaultClaimTypeConverters() {
        Converter<Object, ?> booleanConverter = getConverter(TypeDescriptor.valueOf(Boolean.class));
        Converter<Object, ?> instantConverter = getConverter(TypeDescriptor.valueOf(Instant.class));
        Map<String, Converter<Object, ?>> claimTypeConverters = new HashMap<>();
        claimTypeConverters.put(StandardClaimNames.EMAIL_VERIFIED, booleanConverter);
        claimTypeConverters.put(StandardClaimNames.PHONE_NUMBER_VERIFIED, booleanConverter);
        claimTypeConverters.put(StandardClaimNames.UPDATED_AT, instantConverter);
        return claimTypeConverters;
    }

    private static Converter<Object, ?> getConverter(TypeDescriptor targetDescriptor) {
        TypeDescriptor sourceDescriptor = TypeDescriptor.valueOf(Object.class);
        return (source) -> ClaimConversionService.getSharedInstance().convert(source, sourceDescriptor,
                targetDescriptor);
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        Assert.notNull(userRequest, "userRequest cannot be null");
        OidcUserInfo userInfo = null;
        if (this.shouldRetrieveUserInfo(userRequest)) {
            OAuth2User oauth2User = this.oauth2UserService.loadUser(userRequest);
            Map<String, Object> claims = getClaims(userRequest, oauth2User);
            userInfo = new OidcUserInfo(claims);
            // https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse
            // 1) The sub (subject) Claim MUST always be returned in the UserInfo Response
            if (userInfo.getSubject() == null) {
                OAuth2Error oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE);
                throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
            }
            // 2) Due to the possibility of token substitution attacks (see Section
            // 16.11),
            // the UserInfo Response is not guaranteed to be about the End-User
            // identified by the sub (subject) element of the ID Token.
            // The sub Claim in the UserInfo Response MUST be verified to exactly match
            // the sub Claim in the ID Token; if they do not match,
            // the UserInfo Response values MUST NOT be used.
            if (!userInfo.getSubject().equals(userRequest.getIdToken().getSubject())) {
                OAuth2Error oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE);
                throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
            }
        }
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new OidcUserAuthority(userRequest.getIdToken(), userInfo));
        OAuth2AccessToken token = userRequest.getAccessToken();
        for (String authority : token.getScopes()) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + authority));
        }

        /**********
         * this will check if the user should receive the role admin on conformance
         */
        if (registrationid.equals(userRequest.getClientRegistration().getRegistrationId())) {
            if (userInfo.getClaimAsStringList(this.attribute).contains(this.value)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_admin"));
            }
        }

        return getUser(userRequest, userInfo, authorities);
    }

    private Map<String, Object> getClaims(OidcUserRequest userRequest, OAuth2User oauth2User) {
        Converter<Map<String, Object>, Map<String, Object>> converter = this.claimTypeConverterFactory
                .apply(userRequest.getClientRegistration());
        if (converter != null) {
            return converter.convert(oauth2User.getAttributes());
        }
        return DEFAULT_CLAIM_TYPE_CONVERTER.convert(oauth2User.getAttributes());
    }

    private OidcUser getUser(OidcUserRequest userRequest, OidcUserInfo userInfo, Set<GrantedAuthority> authorities) {
        ClientRegistration.ProviderDetails providerDetails = userRequest.getClientRegistration().getProviderDetails();
        String userNameAttributeName = providerDetails.getUserInfoEndpoint().getUserNameAttributeName();
        if (StringUtils.hasText(userNameAttributeName)) {
            return new DefaultOidcUser(authorities, userRequest.getIdToken(), userInfo, userNameAttributeName);
        }
        return new DefaultOidcUser(authorities, userRequest.getIdToken(), userInfo);
    }

    private boolean shouldRetrieveUserInfo(OidcUserRequest userRequest) {
        // Auto-disabled if UserInfo Endpoint URI is not provided
        ClientRegistration.ProviderDetails providerDetails = userRequest.getClientRegistration().getProviderDetails();
        if (providerDetails.getUserInfoEndpoint().getUri() != null &&
                !"".equals(providerDetails.getUserInfoEndpoint().getUri())) {
            return false;
        }
        // The Claims requested by the profile, email, address, and phone scope values
        // are returned from the UserInfo Endpoint (as described in Section 5.3.2),
        // when a response_type value is used that results in an Access Token being
        // issued.
        // However, when no Access Token is issued, which is the case for the
        // response_type=id_token,
        // the resulting Claims are returned in the ID Token.
        // The Authorization Code Grant Flow, which is response_type=code, results in an
        // Access Token being issued.
        if (AuthorizationGrantType.AUTHORIZATION_CODE
                .equals(userRequest.getClientRegistration().getAuthorizationGrantType())) {
            // Return true if there is at least one match between the authorized scope(s)
            // and accessible scope(s)
            return this.accessibleScopes.isEmpty()
                    || CollectionUtils.containsAny(userRequest.getAccessToken().getScopes(), this.accessibleScopes);
        }
        return false;
    }

    /**
     * Sets the {@link OAuth2UserService} used when requesting the user info resource.
     *
     * @param oauth2UserService the {@link OAuth2UserService} used when requesting the
     *                          user info resource.
     * @since 5.1
     */
    public final void setOauth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService) {
        Assert.notNull(oauth2UserService, "oauth2UserService cannot be null");
        this.oauth2UserService = oauth2UserService;
    }

    /**
     * Sets the factory that provides a {@link Converter} used for type conversion of
     * claim values for an {@link OidcUserInfo}. The default is {@link ClaimTypeConverter}
     * for all {@link ClientRegistration clients}.
     *
     * @param claimTypeConverterFactory the factory that provides a {@link Converter} used
     *                                  for type conversion of claim values for a specific {@link ClientRegistration
     *                                  client}
     * @since 5.2
     */
    public final void setClaimTypeConverterFactory(
            Function<ClientRegistration, Converter<Map<String, Object>, Map<String, Object>>> claimTypeConverterFactory) {
        Assert.notNull(claimTypeConverterFactory, "claimTypeConverterFactory cannot be null");
        this.claimTypeConverterFactory = claimTypeConverterFactory;
    }

    /**
     * Sets the scope(s) that allow access to the user info resource. The default is
     * {@link OidcScopes#PROFILE profile}, {@link OidcScopes#EMAIL email},
     * {@link OidcScopes#ADDRESS address} and {@link OidcScopes#PHONE phone}. The scope(s)
     * are checked against the "granted" scope(s) associated to the
     * {@link OidcUserRequest#getAccessToken() access token} to determine if the user info
     * resource is accessible or not. If there is at least one match, the user info
     * resource will be requested, otherwise it will not.
     *
     * @param accessibleScopes the scope(s) that allow access to the user info resource
     * @since 5.2
     */
    public final void setAccessibleScopes(Set<String> accessibleScopes) {
        Assert.notNull(accessibleScopes, "accessibleScopes cannot be null");
        this.accessibleScopes = accessibleScopes;
    }

}

