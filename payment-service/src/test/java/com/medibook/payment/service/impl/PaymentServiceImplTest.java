package com.medibook.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.medibook.payment.client.AppointmentClient;
import com.medibook.payment.dto.request.AppointmentDto;
import com.medibook.payment.dto.request.PaymentRequest;
import com.medibook.payment.dto.response.PaymentResponse;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.exception.BadRequestException;
import com.medibook.payment.exception.DuplicateResourceException;
import com.medibook.payment.exception.ResourceNotFoundException;
import com.medibook.payment.repository.PaymentRepository;


@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository  paymentRepository;
    @Mock private AppointmentClient  appointmentClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private AppointmentDto scheduledAppointment;
    private AppointmentDto completedAppointment;
    private Payment pendingPayment;
    private Payment successPayment;

    @BeforeEach
    void setUp() {
        // Inject fields so service doesn't NPE on @Value fields
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId",     "mock_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "mock_secret");
        ReflectionTestUtils.setField(paymentService, "razorpayCurrency",  "INR");

        scheduledAppointment = new AppointmentDto();
        scheduledAppointment.setAppointmentId(1);
        scheduledAppointment.setStatus("SCHEDULED");

        completedAppointment = new AppointmentDto();
        completedAppointment.setAppointmentId(2);
        completedAppointment.setStatus("COMPLETED");

        pendingPayment = Payment.builder()
                .paymentId(100)
                .appointmentId(1)
                .patientId(2)
                .amount(500.0)
                .currency("INR")
                .paymentMethod("CASH")
                .status("PENDING")
                .razorpayOrderId("MOCK_ORDER_1")
                .razorpayPaymentId(null)
                .notes("Pay at clinic")
                .createdAt(LocalDateTime.now())
                .build();

        successPayment = Payment.builder()
                .paymentId(101)
                .appointmentId(1)
                .patientId(2)
                .amount(500.0)
                .currency("INR")
                .paymentMethod("UPI")
                .status("SUCCESS")
                .razorpayOrderId("MOCK_ORDER_1")
                .razorpayPaymentId("MOCK_PAY_1")
                .razorpaySignature("valid_sig")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /* ── initiatePayment() — validation tests (no Razorpay) ── */

    @Test
    @DisplayName("initiatePayment: throws BadRequestException when appointment is not SCHEDULED")
    void initiatePayment_notScheduled_throwsException() {
        // appointment is COMPLETED — payment should be rejected
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(2);
        req.setPatientId(2);
        req.setAmount(500.0);
        req.setCurrency("INR");
        req.setPaymentMethod("CASH");

        when(appointmentClient.getById(2)).thenReturn(completedAppointment);

        // Should throw BEFORE hitting Razorpay
        assertThatThrownBy(() -> paymentService.initiatePayment(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("scheduled appointments");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("initiatePayment: throws DuplicateResourceException when payment already exists")
    void initiatePayment_duplicate_throwsException() {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(1);
        req.setPatientId(2);
        req.setAmount(500.0);
        req.setCurrency("INR");
        req.setPaymentMethod("CASH");

        when(appointmentClient.getById(1)).thenReturn(scheduledAppointment);
        // Payment already exists for this appointment
        when(paymentRepository.findByAppointmentId(1)).thenReturn(Optional.of(successPayment));

        // Should throw BEFORE hitting Razorpay
        assertThatThrownBy(() -> paymentService.initiatePayment(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("initiatePayment: gateway failure is converted to BadRequestException")
    void initiatePayment_gatewayFailure_throwsBadRequest() {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(1);
        req.setPatientId(2);
        req.setAmount(500.0);
        req.setCurrency("INR");
        req.setPaymentMethod("UPI");

        when(appointmentClient.getById(1)).thenReturn(scheduledAppointment);
        when(paymentRepository.findByAppointmentId(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.initiatePayment(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Razorpay");
    }

    /* ── verifyPayment() ────────────────────────────────────── */

    @Test
    @DisplayName("verifyPayment: throws BadRequestException when payment already SUCCESS")
    void verifyPayment_alreadySuccess_throwsException() {
        when(paymentRepository.findByRazorpayOrderId("MOCK_ORDER_1"))
                .thenReturn(Optional.of(successPayment));

        assertThatThrownBy(() -> paymentService.verifyPayment(
                "MOCK_ORDER_1", "MOCK_PAY_1", "some_sig"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already verified");
    }

    @Test
    @DisplayName("verifyPayment: throws ResourceNotFoundException for unknown orderId")
    void verifyPayment_notFound_throwsException() {
        when(paymentRepository.findByRazorpayOrderId("UNKNOWN_ORDER"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.verifyPayment(
                "UNKNOWN_ORDER", "PAY_1", "sig"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("verifyPayment: MOCK order marks payment SUCCESS and updates appointment")
    void verifyPayment_mockOrder_success() {
        Payment pending = Payment.builder()
                .paymentId(100)
                .appointmentId(1)
                .patientId(2)
                .amount(500.0)
                .currency("INR")
                .paymentMethod("UPI")
                .status("PENDING")
                .razorpayOrderId("MOCK_ORDER_1")
                .createdAt(LocalDateTime.now())
                .build();
        when(paymentRepository.findByRazorpayOrderId("MOCK_ORDER_1"))
                .thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.verifyPayment("MOCK_ORDER_1", "pay_mock", "ignored");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        verify(appointmentClient).updateStatus(1, "SCHEDULED");
    }

    @Test
    @DisplayName("verifyPayment: remains successful when appointment update fails")
    void verifyPayment_appointmentUpdateFailure_stillSuccess() {
        Payment pending = Payment.builder()
                .paymentId(102)
                .appointmentId(1)
                .patientId(2)
                .amount(500.0)
                .currency("INR")
                .paymentMethod("UPI")
                .status("PENDING")
                .razorpayOrderId("MOCK_ORDER_2")
                .createdAt(LocalDateTime.now())
                .build();
        when(paymentRepository.findByRazorpayOrderId("MOCK_ORDER_2")).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("appointment down")).when(appointmentClient).updateStatus(1, "SCHEDULED");

        PaymentResponse response = paymentService.verifyPayment("MOCK_ORDER_2", "pay_mock_2", "ignored");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("verifyRazorpaySignature: accepts a real HMAC signature")
    void verifyRazorpaySignature_validHmac_returnsTrue() throws Exception {
        String orderId = "order_123";
        String paymentId = "pay_123";
        String signature = hmac(orderId + "|" + paymentId, "mock_secret");

        Boolean valid = ReflectionTestUtils.invokeMethod(
                paymentService, "verifyRazorpaySignature", orderId, paymentId, signature);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("verifyRazorpaySignature: wraps crypto errors in BadRequestException")
    void verifyRazorpaySignature_cryptoError_throwsBadRequest() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", null);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                paymentService, "verifyRazorpaySignature", "order_123", "pay_123", "sig"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Signature verification failed");

        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "mock_secret");
    }

    @Test
    @DisplayName("verifyPayment: invalid signature marks payment FAILED")
    void verifyPayment_invalidSignature_marksFailed() {
        Payment pending = Payment.builder()
                .paymentId(100)
                .appointmentId(1)
                .patientId(2)
                .amount(500.0)
                .currency("INR")
                .paymentMethod("UPI")
                .status("PENDING")
                .razorpayOrderId("order_real")
                .createdAt(LocalDateTime.now())
                .build();
        when(paymentRepository.findByRazorpayOrderId("order_real"))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> paymentService.verifyPayment("order_real", "pay_real", "bad-signature"))
                .isInstanceOf(BadRequestException.class);
        verify(paymentRepository).save(argThat(p -> p.getStatus().equals("FAILED")));
    }

    /* ── initiateRefund() ───────────────────────────────────── */

    @Test
    @DisplayName("initiateRefund: throws BadRequestException when payment is not SUCCESS")
    void initiateRefund_notSuccess_throwsException() {
        when(paymentRepository.findByPaymentId(100)).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.initiateRefund(100))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("successful payments");
    }

    @Test
    @DisplayName("initiateRefund: throws ResourceNotFoundException for unknown paymentId")
    void initiateRefund_notFound_throwsException() {
        when(paymentRepository.findByPaymentId(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.initiateRefund(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("initiateRefund: simulated payment id marks payment REFUNDED")
    void initiateRefund_simulatedPayment_success() {
        successPayment.setRazorpayPaymentId("TXN_LOCAL_1");
        when(paymentRepository.findByPaymentId(101)).thenReturn(Optional.of(successPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.initiateRefund(101);

        assertThat(response.getStatus()).isEqualTo("REFUNDED");
    }

    @Test
    @DisplayName("callRefundGateway: treats null and non-pay ids as simulated refunds")
    void callRefundGateway_simulatedIds_returnTrue() {
        Boolean nullResult = ReflectionTestUtils.invokeMethod(paymentService, "callRefundGateway", (String) null);
        Boolean localResult = ReflectionTestUtils.invokeMethod(paymentService, "callRefundGateway", "LOCAL_1");

        assertThat(nullResult).isTrue();
        assertThat(localResult).isTrue();
    }

    /* ── getPaymentByAppointment() ──────────────────────────── */

    @Test
    @DisplayName("getPaymentByAppointment: returns payment when found")
    void getPaymentByAppointment_found() {
        when(paymentRepository.findByAppointmentId(1))
                .thenReturn(Optional.of(successPayment));

        PaymentResponse response = paymentService.getPaymentByAppointment(1);

        assertThat(response).isNotNull();
        assertThat(response.getAppointmentId()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("getPaymentByAppointment: throws ResourceNotFoundException when not found")
    void getPaymentByAppointment_notFound_throwsException() {
        when(paymentRepository.findByAppointmentId(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentByAppointment(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getPaymentById: returns payment and throws when missing")
    void getPaymentById_paths() {
        when(paymentRepository.findByPaymentId(101)).thenReturn(Optional.of(successPayment));
        when(paymentRepository.findByPaymentId(404)).thenReturn(Optional.empty());

        assertThat(paymentService.getPaymentById(101).getPaymentId()).isEqualTo(101);
        assertThatThrownBy(() -> paymentService.getPaymentById(404))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /* ── getPaymentsByPatient() ─────────────────────────────── */

    @Test
    @DisplayName("getPaymentsByPatient: returns all payments for given patientId")
    void getPaymentsByPatient_returnsList() {
        when(paymentRepository.findByPatientId(2))
                .thenReturn(List.of(successPayment, pendingPayment));

        List<Payment> result = paymentService.getPaymentsByPatient(2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPatientId()).isEqualTo(2);
    }

    @Test
    @DisplayName("getPaymentsByPatient: returns empty list when no payments exist")
    void getPaymentsByPatient_noPayments_returnsEmptyList() {
        when(paymentRepository.findByPatientId(99)).thenReturn(List.of());

        List<Payment> result = paymentService.getPaymentsByPatient(99);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getPaymentsByProvider: delegates to provider payment query")
    void getPaymentsByProvider_returnsList() {
        when(paymentRepository.findPaymentsByProvider(3)).thenReturn(List.of(successPayment));

        assertThat(paymentService.getPaymentsByProvider(3)).containsExactly(successPayment);
    }

    /* ── getTotalRevenue() ──────────────────────────────────── */

    @Test
    @DisplayName("getTotalRevenue: returns correct sum from repository")
    void getTotalRevenue_returnsSum() {
        when(paymentRepository.calculateTotalRevenue()).thenReturn(15000.0);

        assertThat(paymentService.getTotalRevenue()).isEqualTo(15000.0);
    }

    @Test
    @DisplayName("getTotalRevenue: returns 0.0 when repository returns null")
    void getTotalRevenue_nullResult_returnsZero() {
        when(paymentRepository.calculateTotalRevenue()).thenReturn(null);

        assertThat(paymentService.getTotalRevenue()).isEqualTo(0.0);
    }

    /* ── updatePaymentStatus() ──────────────────────────────── */

    @Test
    @DisplayName("updatePaymentStatus: success — status updated to REFUNDED")
    void updatePaymentStatus_success() {
        when(paymentRepository.findByPaymentId(101)).thenReturn(Optional.of(successPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(successPayment);

        paymentService.updatePaymentStatus(101, "REFUNDED");

        verify(paymentRepository).save(argThat(p -> p.getStatus().equals("REFUNDED")));
    }

    @Test
    @DisplayName("updatePaymentStatus: throws BadRequestException for invalid status string")
    void updatePaymentStatus_invalidStatus_throwsException() {
        assertThatThrownBy(() -> paymentService.updatePaymentStatus(101, "INVALID_XYZ"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status");

        verify(paymentRepository, never()).findByPaymentId(anyInt());
    }

    @Test
    @DisplayName("updatePaymentStatus: throws ResourceNotFoundException for missing payment")
    void updatePaymentStatus_missingPayment_throwsException() {
        when(paymentRepository.findByPaymentId(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.updatePaymentStatus(404, "SUCCESS"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /* ── getPaymentsByStatus() ──────────────────────────────── */

    @Test
    @DisplayName("getPaymentsByStatus: returns payments for valid status SUCCESS")
    void getPaymentsByStatus_validStatus_returnsList() {
        when(paymentRepository.findByStatus("SUCCESS")).thenReturn(List.of(successPayment));

        List<Payment> result = paymentService.getPaymentsByStatus("SUCCESS");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("getPaymentsByStatus: throws BadRequestException for invalid status")
    void getPaymentsByStatus_invalidStatus_throwsException() {
        assertThatThrownBy(() -> paymentService.getPaymentsByStatus("WRONG_STATUS"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status");
    }

    private String hmac(String message, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] hash = mac.doFinal(message.getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String part = Integer.toHexString(0xff & b);
            if (part.length() == 1) {
                hex.append('0');
            }
            hex.append(part);
        }
        return hex.toString();
    }
}
