package io.fintechlabs.testframework.token;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;

@SuppressWarnings("rawtypes")
public interface TokenService {

	Map createToken(boolean permanent);

	boolean deleteToken(String id);

	List<Map> getAllTokens();

	Authentication getAuthenticationForToken(String token);

	void createIndexes();
}
