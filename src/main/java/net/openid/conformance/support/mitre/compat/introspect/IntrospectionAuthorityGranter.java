package net.openid.conformance.support.mitre.compat.introspect;

import com.google.gson.JsonObject;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

/**
 * @author jricher
 *
 */
public interface IntrospectionAuthorityGranter {

	public List<GrantedAuthority> getAuthorities(JsonObject introspectionResponse);

}
