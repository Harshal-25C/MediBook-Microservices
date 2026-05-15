package com.medibook.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PaymentServiceApplicationTests {
	
	@Test
    void paymentClassExists() {
        assertThat(PaymentServiceApplication.class).isNotNull();
    }

}
