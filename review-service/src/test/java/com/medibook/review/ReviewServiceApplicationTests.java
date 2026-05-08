package com.medibook.review;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ReviewServiceApplicationTests {
	
	@Test
    void reviewClassExists() {
        assertThat(ReviewServiceApplication.class).isNotNull();
    }

}
