package az.masapay.auth;

import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.UserRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticates a Google ID token and provisions the corresponding user. A first
 * time user is stored disabled and without a role; an admin activates them later
 * (#16). Subsequent logins refresh the stored profile and last-login timestamp.
 * <p>
 * Emails listed in {@code masapay.auth.super-admin-emails} are bootstrapped as
 * enabled SUPER_ADMINs on every login, which seeds the first administrator who
 * can then provision everyone else.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GoogleAuthService {

	private final GoogleTokenVerifier tokenVerifier;
	private final UserRepository userRepository;
	private final Set<String> superAdminEmails;

	public GoogleAuthService(GoogleTokenVerifier tokenVerifier, UserRepository userRepository,
			@Value("${masapay.auth.super-admin-emails:}") String superAdminEmails) {
		this.tokenVerifier = tokenVerifier;
		this.userRepository = userRepository;
		this.superAdminEmails = Arrays.stream(superAdminEmails.split(","))
			.map(email -> email.trim().toLowerCase(Locale.ROOT))
			.filter(email -> !email.isEmpty())
			.collect(Collectors.toUnmodifiableSet());
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

		if (isConfiguredSuperAdmin(info.email())) {
			user.setEnabled(true);
			user.setRole(UserRole.SUPER_ADMIN);
		}

		return userRepository.save(user);
	}

	private boolean isConfiguredSuperAdmin(String email) {
		return email != null && superAdminEmails.contains(email.toLowerCase(Locale.ROOT));
	}
}
