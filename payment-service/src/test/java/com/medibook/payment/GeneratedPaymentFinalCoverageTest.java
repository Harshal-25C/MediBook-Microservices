
package com.medibook.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.medibook.payment.entity.Payment;
import com.medibook.payment.exception.BadRequestException;
import com.medibook.payment.resource.PaymentResource;
import com.medibook.payment.service.PaymentService;
import com.medibook.payment.service.impl.PaymentServiceImpl;

import org.junit.jupiter.api.Test;

class GeneratedPaymentFinalCoverageTest {
    @Test
    void resourceProviderEndpointCoversSuccessAndFailure() throws Exception {
        PaymentService service = mock(PaymentService.class);
        PaymentResource resource = new PaymentResource();
        set(resource, "paymentService", service);
        Payment payment = Payment.builder().paymentId(1).status("SUCCESS").build();
        when(service.getPaymentsByProvider(3)).thenReturn(List.of(payment));
        assertThat(resource.getPaymentsByProvider(3).getStatusCode().value()).isEqualTo(200);
        when(service.getPaymentsByProvider(4)).thenThrow(new RuntimeException("down"));
        assertThat(resource.getPaymentsByProvider(4).getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void privateGatewayHelpersAreExercised() throws Exception {
        PaymentServiceImpl service = new PaymentServiceImpl();
        set(service, "razorpayKeyId", "bad_key");
        set(service, "razorpayKeySecret", "secret");
        set(service, "razorpayCurrency", "INR");

        Class<?> gatewayType = Class.forName("com.medibook.payment.service.impl.PaymentServiceImpl$GatewayResponse");
        Constructor<?> ctor = gatewayType.getDeclaredConstructor(String.class, String.class, String.class);
        ctor.setAccessible(true);
        Object response = ctor.newInstance("order", "payment", "PENDING");
        Field status = gatewayType.getDeclaredField("status");
        status.setAccessible(true);
        assertThat(status.get(response)).isEqualTo("PENDING");

        Method verify = PaymentServiceImpl.class.getDeclaredMethod(
                "verifyRazorpaySignature", String.class, String.class, String.class);
        verify.setAccessible(true);
        String signature = hmac("order_1|pay_1", "secret");
        assertThat((Boolean) verify.invoke(service, "order_1", "pay_1", signature)).isTrue();
        set(service, "razorpayKeySecret", null);
        assertBadRequest(() -> verify.invoke(service, "order_1", "pay_1", "sig"));

        set(service, "razorpayKeySecret", "secret");
        Method refund = PaymentServiceImpl.class.getDeclaredMethod("callRefundGateway", String.class);
        refund.setAccessible(true);
        assertThat((Boolean) refund.invoke(service, "manual-id")).isTrue();
        assertBadRequest(() -> refund.invoke(service, "pay_real_id"));
    }

    private static String hmac(String value, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder out = new StringBuilder();
        for (byte b : mac.doFinal(value.getBytes(StandardCharsets.UTF_8))) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) out.append('0');
            out.append(hex);
        }
        return out.toString();
    }

    private static void assertBadRequest(ThrowingRunnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(BadRequestException.class);
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
