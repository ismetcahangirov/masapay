package az.masapay.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

	@Mock
	private GoogleTokenVerifier tokenVerifier;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private GoogleAuthService service;

	private static final GoogleUserInfo INFO =
		new GoogleUserInfo("google-sub-123", "diner@example.com", "Test Diner", "https://pic/1.png");

	@Test
	void firstLoginProvisionsDisabledUserWithoutRole() {
		when(tokenVerifier.verify("token")).thenReturn(INFO);
		when(userRepository.findByGoogleSub("google-sub-123")).thenReturn(Optional.empty());
		when(userRepository.findByEmail("diner@example.com")).thenReturn(Optional.empty());
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		service.authenticate("token");

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User saved = captor.getValue();
		assertThat(saved.getGoogleSub()).isEqualTo("google-sub-123");
		assertThat(saved.getEmail()).isEqualTo("diner@example.com");
		assertThat(saved.getName()).isEqualTo("Test Diner");
		assertThat(saved.getPictureUrl()).isEqualTo("https://pic/1.png");
		assertThat(saved.getLastLoginAt()).isNotNull();
		assertThat(saved.isEnabled()).isFalse();
		assertThat(saved.getRole()).isNull();
	}

	@Test
	void returningUserIsUpdatedNotDuplicated() {
		User existing = new User();
		existing.setGoogleSub("google-sub-123");
		existing.setEmail("old@example.com");
		existing.setEnabled(true);
		existing.setRole(UserRole.WAITER);
		when(tokenVerifier.verify("token")).thenReturn(INFO);
		when(userRepository.findByGoogleSub("google-sub-123")).thenReturn(Optional.of(existing));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		User result = service.authenticate("token");

		// Profile refreshed, but activation and role are preserved.
		assertThat(result.getEmail()).isEqualTo("diner@example.com");
		assertThat(result.isEnabled()).isTrue();
		assertThat(result.getRole()).isEqualTo(UserRole.WAITER);
		verify(userRepository).save(existing);
	}

	@Test
	void invalidTokenIsRejectedAndNoUserSaved() {
		when(tokenVerifier.verify("bad")).thenThrow(new BadCredentialsException("Invalid Google ID token"));

		try {
			service.authenticate("bad");
			assertThat(false).as("expected BadCredentialsException").isTrue();
		} catch (BadCredentialsException expected) {
			// verified below
		}
		verify(userRepository, org.mockito.Mockito.never()).save(any());
	}
}
