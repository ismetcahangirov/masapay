package az.masapay.payment.payriff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PayriffSignatureTest {

	private final PayriffSignature signature = new PayriffSignature("test-secret-0123456789-0123456789");

	@Test
	void signsTheSameDataTheSameWay() {
		assertThat(signature.sign("tx-1")).isEqualTo(signature.sign("tx-1")).hasSize(64);
	}

	@Test
	void differentDataOrKeyGivesADifferentSignature() {
		PayriffSignature otherKey = new PayriffSignature("another-secret-0123456789-012345");

		assertThat(signature.sign("tx-1")).isNotEqualTo(signature.sign("tx-2"));
		assertThat(signature.sign("tx-1")).isNotEqualTo(otherKey.sign("tx-1"));
	}

	@Test
	void acceptsItsOwnSignature() {
		String sig = signature.sign("tx-1");

		assertThat(signature.matches("tx-1", sig)).isTrue();
		assertThatCode(() -> signature.verify("tx-1", sig)).doesNotThrowAnyException();
	}

	@Test
	void rejectsATamperedOrMissingSignatureWith403() {
		String sigForOtherTx = signature.sign("tx-2");

		assertThat(signature.matches("tx-1", sigForOtherTx)).isFalse();
		assertThat(signature.matches("tx-1", null)).isFalse();
		assertThatThrownBy(() -> signature.verify("tx-1", sigForOtherTx))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}
}
