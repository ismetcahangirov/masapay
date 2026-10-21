package az.masapay.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecretMaskerTest {

	@Test
	void keepsOnlyTheLastFourCharacters() {
		assertThat(SecretMasker.mask("sk_live_1234abcd")).isEqualTo(SecretMasker.MASK + "abcd");
	}

	@Test
	void maskWidthDoesNotDependOnSecretLength() {
		assertThat(SecretMasker.mask("12345")).hasSameSizeAs(SecretMasker.mask("a-much-longer-secret-value"));
	}

	@Test
	void shortSecretIsFullyMasked() {
		assertThat(SecretMasker.mask("abcd")).isEqualTo(SecretMasker.MASK);
		assertThat(SecretMasker.mask("x")).isEqualTo(SecretMasker.MASK);
	}

	@Test
	void missingSecretMasksToNull() {
		assertThat(SecretMasker.mask(null)).isNull();
		assertThat(SecretMasker.mask("")).isNull();
	}

	@Test
	void nullBlankOrMaskedValueMeansUnchanged() {
		String current = "sk_live_1234abcd";
		assertThat(SecretMasker.isUnchanged(null, current)).isTrue();
		assertThat(SecretMasker.isUnchanged("   ", current)).isTrue();
		assertThat(SecretMasker.isUnchanged(SecretMasker.mask(current), current)).isTrue();
	}

	@Test
	void newValueIsAChange() {
		assertThat(SecretMasker.isUnchanged("sk_live_new", "sk_live_1234abcd")).isFalse();
		assertThat(SecretMasker.isUnchanged("sk_live_new", null)).isFalse();
	}
}
