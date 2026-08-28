package net.openid.conformance.token;

import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public interface TokenService {

	Map createToken(boolean permanent);

	boolean deleteToken(String id);

	List<Map> getAllTokens();

	Map<String, Object> findToken(String token);

	void createIndexes();

	/**
	 * Hand every API token owned by the given legacy (issuer, subject) over to the
	 * currently authenticated user. Without this the tokens keep authenticating as
	 * the legacy identity but own nothing, so every /api/** call they make comes
	 * back empty and they vanish from getAllTokens() — silent, and indistinguishable
	 * from data loss.
	 *
	 * @return the number of tokens whose ownership changed
	 */
	long migrateOwnership(String oldIss, String oldSub);
}
