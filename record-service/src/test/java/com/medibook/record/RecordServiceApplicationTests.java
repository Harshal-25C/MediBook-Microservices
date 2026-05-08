package com.medibook.record;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class RecordServiceApplicationTests {
	
	@Test
    void recordClassExists() {
        assertThat(RecordServiceApplication.class).isNotNull();
    }

}
