package net.openid.conformance.extensions;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import org.apache.commons.codec.digest.DigestUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class KmsJWSSigner implements JWSSigner {

	private static Map<JWSAlgorithm, SigningAlgorithmSpec> JWS_TO_AWS = Map.of(
		JWSAlgorithm.PS256, SigningAlgorithmSpec.RSASSA_PSS_SHA_256
	);

	private final KmsClient kmsClient;
	private String alias;

	private MessageType messageType = MessageType.DIGEST;

	public KmsJWSSigner(KmsClient kmsClient, JWK jwk) {
		this.kmsClient = kmsClient;
		RSAKey rsaKey = (RSAKey) jwk;
		alias = rsaKey.getPrivateExponent().decodeToString();
	}

	@Override
	public Base64URL sign(JWSHeader header, byte[] signingInput) throws JOSEException {
		Objects.requireNonNull(header, "JWSHeader is missing");
		Objects.requireNonNull(signingInput, "signingInput is missing");
		SdkBytes message = this.createMessage(signingInput);

		SigningAlgorithmSpec spec = JWS_TO_AWS.get(header.getAlgorithm());
		SignRequest signRequest = SignRequest.builder()
			.messageType(messageType)
			.signingAlgorithm(spec)
			.keyId(alias)
			.message(message)
			.build();
		SignResponse signResponse;
		try {
			signResponse = kmsClient.sign(signRequest);
		} catch (DisabledException | KeyUnavailableException | InvalidKeyUsageException | NotFoundException e) {
			throw new RemoteKeySourceException("KMS complained that the key is invalid", e);
		} catch (InvalidGrantTokenException | DependencyTimeoutException f) {
			throw new RemoteKeySourceException("A temporary exception was thrown from KMS.", f);
		}
		return Base64URL.encode(signResponse.signature().asByteArray());
	}

	@Override
	public Set<JWSAlgorithm> supportedJWSAlgorithms() {
		return JWS_TO_AWS.keySet();
	}

	@Override
	public JCAContext getJCAContext() {
		return null;
	}

	private SdkBytes createMessage(byte[] payloadBytes) {
		if (messageType == MessageType.DIGEST) {
			payloadBytes = DigestUtils.sha256(payloadBytes);
		}
		return SdkBytes.fromByteArray(payloadBytes);
	}

}
