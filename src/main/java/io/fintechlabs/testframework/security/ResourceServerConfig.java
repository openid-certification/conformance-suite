/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.security;

import org.mitre.oauth2.introspectingfilter.IntrospectingTokenService;
import org.mitre.oauth2.introspectingfilter.service.IntrospectionAuthorityGranter;
import org.mitre.oauth2.introspectingfilter.service.IntrospectionConfigurationService;
import org.mitre.oauth2.introspectingfilter.service.impl.SimpleIntrospectionAuthorityGranter;
import org.mitre.oauth2.introspectingfilter.service.impl.StaticIntrospectionConfigurationService;
import org.mitre.oauth2.model.RegisteredClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import com.google.common.collect.Lists;

@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	// Config for the OAuth introspection filters
	@Value("${oauth.introspection_url}")
	private String introspectionUrl;
	@Value("${oauth.resource_id}")
	private String resourceId;
	@Value("${oauth.resource_secret}")
	private String resourceSecret;
	
	/* (non-Javadoc)
	 * @see org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter#configure(org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer)
	 */
	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		resources.tokenServices(tokenServices());
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter#configure(org.springframework.security.config.annotation.web.builders.HttpSecurity)
	 */
	@Override
	public void configure(HttpSecurity http) throws Exception {

		http.authorizeRequests()
			.antMatchers("/currentuser", "/runner/**", "/log/**", "/info/**", "/test/**")
			.authenticated();
		
	}
	@Bean
	@Primary
	public ResourceServerTokenServices tokenServices() {

		IntrospectingTokenService tokenService = new IntrospectingTokenService();
		tokenService.setCacheTokens(true);
		tokenService.setIntrospectionConfigurationService(introspectionConfiguration());
		tokenService.setIntrospectionAuthorityGranter(introspectionAuthorityGranter());

		return tokenService;
	}

	/**
	 * @return
	 */
	@Bean
	public IntrospectionAuthorityGranter introspectionAuthorityGranter() {
		SimpleIntrospectionAuthorityGranter authorityGranter = new SimpleIntrospectionAuthorityGranter();

		// TODO: right now if you get a token you're an admin :shrug:
		authorityGranter.setAuthorities(Lists.newArrayList(GoogleHostedDomainAdminAuthoritiesMapper.ROLE_ADMIN));

		return authorityGranter;
	}

	/**
	 * @return
	 */
	@Bean
	public IntrospectionConfigurationService introspectionConfiguration() {
		StaticIntrospectionConfigurationService config = new StaticIntrospectionConfigurationService();

		config.setClientConfiguration(getResource());

		// set this value to the introspection endpoint of this system's trusted AS
		config.setIntrospectionUrl(introspectionUrl);

		return config;
	}

	/**
	 * @return
	 */
	@Bean
	public RegisteredClient getResource() {
		RegisteredClient resource = new RegisteredClient();

		// set these values to the keys needed to call the introspection endpoint
		resource.setClientId(resourceId);
		resource.setClientSecret(resourceSecret);

		return resource;

	}	

}