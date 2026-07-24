package az.masapay.auth;

import az.masapay.domain.User;
import az.masapay.repository.UserRepository;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticates a Google ID token and provisions the corresponding user. A first
 * time user is stored disabled and without a role; an admin activates them later
 * (#16). Subsequent logins refresh the stored profile and last-login timestamp.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GoogleAuthService {

	private final GoogleTokenVerifier tokenVerifier;
	private final UserRepository userRepository;

	public GoogleAuthService(GoogleTokenVerifier tokenVerifier, UserRepository userRepository) {
		this.tokenVerifier = tokenVerifier;
		this.userRepository = userRepository;
	}

	@Transactional
	public User authenticate(String idToken) {
		GoogleUserInfo info = tokenVerifier.verify(idToken);

		User user = userRepository.findByGoogleSub(info.subject())
			.or(() -> userRepository.findByEmail(info.email()))
			.orElseGet(User::new);

		if (user.getGoogleSub() == null) {
			// First login: link the Google account; stays disabled with no role.
			user.setGoogleSub(info.subject());
		}
		user.setEmail(info.email());
		user.setName(info.name());
		user.setPictureUrl(info.pictureUrl());
		user.setLastLoginAt(Instant.now());

		return userRepository.save(user);
	}
}
