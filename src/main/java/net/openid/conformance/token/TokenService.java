package net.openid.conformance.token;

import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public interface TokenService {

	Map createToken(boolean permanent);

	boolean deleteToken(String id);

	List<Map> getAllTokens();

	Map findToken(String token);

	void createIndexes();
}
