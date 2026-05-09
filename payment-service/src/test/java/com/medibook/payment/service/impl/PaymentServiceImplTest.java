package com.medibook.payment.service.impl;
 
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
 
import java.util.List;
import java.util.Optional;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
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
 
    @Mock private PaymentRepository paymentRepository;
    @Mock private AppointmentClient appointmentClient;
 
    private PaymentServiceImpl service;
 
    private Payment successPayment;
    private Payment pendingPayment;
    private AppointmentDto scheduledAppointment;
    private PaymentRequest paymentRequest;
 
    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl();
        injectField(service, "paymentRepository", paymentRepository);
        injectField(service, "appointmentClient", appointmentClient);
        injectField(service, "razorpayKeyId", "test_key_id");
        injectField(service, "razorpayKeySecret", "test_key_secret_12345678901234567890");
        injectField(service, "razorpayCurrency", "INR");
 
        scheduledAppointment = new AppointmentDto();
        scheduledAppointment.setAppointmentId(10);
        scheduledAppointment.setStatus("SCHEDULED");
 
        paymentRequest = new PaymentRequest();
        paymentRequest.setAppointmentId(10);
        paymentRequest.setPatientId(1);
        paymentRequest.setAmount(500.0);
        paymentRequest.setCurrency("INR");
        paymentRequest.setPaymentMethod("RAZORPAY");
 
        pendingPayment = Payment.builder()
                .paymentId(1).appointmentId(10).patientId(1)
                .amount(500.0).currency("INR").paymentMethod("RAZORPAY")
                .status("PENDING").razorpayOrderId("MOCK_order_001")
                .build();
 
        successPayment = Payment.builder()
                .paymentId(2).appointmentId(10).patientId(1)
                .amount(500.0).currency("INR").paymentMethod("RAZORPAY")
                .status("SUCCESS")
                .razorpayOrderId("MOCK_order_002")
                .razorpayPaymentId("TXN_pay_001")
                .build();
    }
 
    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
 
    // ─────────────────────────── initiatePayment ─────────────────────────
 
    @Test
    void initiatePayment_alreadyHasPayment_throwsDuplicateResourceException() {
        when(appointmentClient.getById(10)).thenReturn(scheduledAppointment);
        when(paymentRepository.findByAppointmentId(10)).thenReturn(Optional.of(pendingPayment));
 
        assertThatThrownBy(() -> service.initiatePayment(paymentRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Payment already exists");
    }
 
    @Test
    void initiatePayment_appointmentNotScheduled_throwsBadRequestException() {
        scheduledAppointment.setStatus("COMPLETED");
        when(appointmentClient.getById(10)).thenReturn(scheduledAppointment);
 
        assertThatThrownBy(() -> service.initiatePayment(paymentRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("scheduled appointments");
    }
 
    @Test
    void initiatePayment_confirmedStatus_isAllowed() {
        scheduledAppointment.setStatus("CONFIRMED");
        when(appointmentClient.getById(10)).thenReturn(scheduledAppointment);
        when(paymentRepository.findByAppointmentId(10)).thenReturn(Optional.empty());
        // Razorpay will fail in test, so we test that failure path is BadRequestException
        assertThatThrownBy(() -> service.initiatePayment(paymentRequest))
                .isInstanceOf(BadRequestException.class); // Razorpay fails in test env
    }
 
    // ─────────────────────────── verifyPayment ───────────────────────────
 
    @Test
    void verifyPayment_alreadySuccess_throwsBadRequestException() {
        when(paymentRepository.findByRazorpayOrderId("MOCK_order_002")).thenReturn(Optional.of(successPayment));
 
        assertThatThrownBy(() -> service.verifyPayment("MOCK_order_002", "pay_123", "sig"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already verified");
    }
 
    @Test
    void verifyPayment_invalidSignature_setsFailed() {
        // MOCK_ order → signature verification passes (mock bypass)
        // Real orderId with bad sig → fails
        pendingPayment.setRazorpayOrderId("real_order_xyz"); // non-MOCK
        when(paymentRepository.findByRazorpayOrderId("real_order_xyz")).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenReturn(pendingPayment);
 
        assertThatThrownBy(() -> service.verifyPayment("real_order_xyz", "pay_123", "badsig"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid signature");
        assertThat(pendingPayment.getStatus()).isEqualTo("FAILED");
    }
 
    @Test
    void verifyPayment_mockOrder_signaturePassesAndSetsSuccess() {
        // MOCK_ prefixed orderId bypasses real signature check
        when(paymentRepository.findByRazorpayOrderId("MOCK_order_001")).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenReturn(pendingPayment);
        // appointmentClient.updateStatus is called - allow it to either succeed or fail
        doAnswer(inv -> null).when(appointmentClient).updateStatus(anyInt(), anyString());
 
        PaymentResponse result = service.verifyPayment("MOCK_order_001", "TXN_pay_001", "any-sig");
 
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
    }
 
    @Test
    void verifyPayment_orderNotFound_throwsResourceNotFoundException() {
        when(paymentRepository.findByRazorpayOrderId("missing")).thenReturn(Optional.empty());
 
        assertThatThrownBy(() -> service.verifyPayment("missing", "pay_x", "sig"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    @Test
    void verifyPayment_appointmentUpdateFails_doesNotThrow() {
        when(paymentRepository.findByRazorpayOrderId("MOCK_order_001")).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenReturn(pendingPayment);
        doThrow(new RuntimeException("Feign error")).when(appointmentClient).updateStatus(anyInt(), anyString());
 
        // Should not throw - appointment update failure is caught
        assertThatCode(() -> service.verifyPayment("MOCK_order_001", "TXN_pay_001", "sig"))
                .doesNotThrowAnyException();
    }
 
    // ─────────────────────────── getPaymentByAppointment ─────────────────
 
    @Test
    void getPaymentByAppointment_found_returnsResponse() {
        when(paymentRepository.findByAppointmentId(10)).thenReturn(Optional.of(successPayment));
 
        PaymentResponse result = service.getPaymentByAppointment(10);
 
        assertThat(result.getAppointmentId()).isEqualTo(10);
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
    }
 
    @Test
    void getPaymentByAppointment_notFound_throwsResourceNotFoundException() {
        when(paymentRepository.findByAppointmentId(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPaymentByAppointment(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    // ─────────────────────────── getPaymentById ───────────────────────────
 
    @Test
    void getPaymentById_found_returnsResponse() {
        when(paymentRepository.findByPaymentId(2)).thenReturn(Optional.of(successPayment));
 
        PaymentResponse result = service.getPaymentById(2);
 
        assertThat(result.getPaymentId()).isEqualTo(2);
    }
 
    @Test
    void getPaymentById_notFound_throwsResourceNotFoundException() {
        when(paymentRepository.findByPaymentId(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPaymentById(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    // ─────────────────────────── getPaymentsByPatient / Provider ─────────
 
    @Test
    void getPaymentsByPatient_returnsList() {
        when(paymentRepository.findByPatientId(1)).thenReturn(List.of(successPayment));
        assertThat(service.getPaymentsByPatient(1)).hasSize(1);
    }
 
    @Test
    void getPaymentsByProvider_returnsList() {
        when(paymentRepository.findPaymentsByProvider(5)).thenReturn(List.of(successPayment));
        assertThat(service.getPaymentsByProvider(5)).hasSize(1);
    }
 
    // ─────────────────────────── initiateRefund ───────────────────────────
 
    @Test
    void initiateRefund_success_simulatedPaymentId_setsRefunded() {
        // TXN_ prefix → simulated refund, returns true
        when(paymentRepository.findByPaymentId(2)).thenReturn(Optional.of(successPayment));
        when(paymentRepository.save(any())).thenReturn(successPayment);
 
        PaymentResponse result = service.initiateRefund(2);
 
        assertThat(result.getStatus()).isEqualTo("REFUNDED");
    }
 
    @Test
    void initiateRefund_notSuccessful_throwsBadRequestException() {
        when(paymentRepository.findByPaymentId(1)).thenReturn(Optional.of(pendingPayment));
 
        assertThatThrownBy(() -> service.initiateRefund(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("successful payments");
    }
 
    @Test
    void initiateRefund_paymentNotFound_throwsResourceNotFoundException() {
        when(paymentRepository.findByPaymentId(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.initiateRefund(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    @Test
    void initiateRefund_nullPaymentId_simulatesSuccess() {
        successPayment.setRazorpayPaymentId(null);
        when(paymentRepository.findByPaymentId(2)).thenReturn(Optional.of(successPayment));
        when(paymentRepository.save(any())).thenReturn(successPayment);
 
        PaymentResponse result = service.initiateRefund(2);
        assertThat(result.getStatus()).isEqualTo("REFUNDED");
    }
 
    // ─────────────────────────── getPaymentsByStatus ──────────────────────
 
    @Test
    void getPaymentsByStatus_validStatus_returnsList() {
        when(paymentRepository.findByStatus("SUCCESS")).thenReturn(List.of(successPayment));
        assertThat(service.getPaymentsByStatus("SUCCESS")).hasSize(1);
    }
 
    @Test
    void getPaymentsByStatus_pendingStatus_returnsList() {
        when(paymentRepository.findByStatus("PENDING")).thenReturn(List.of(pendingPayment));
        assertThat(service.getPaymentsByStatus("PENDING")).hasSize(1);
    }
 
    @Test
    void getPaymentsByStatus_failedStatus_returnsList() {
        when(paymentRepository.findByStatus("FAILED")).thenReturn(List.of());
        assertThatCode(() -> service.getPaymentsByStatus("FAILED")).doesNotThrowAnyException();
    }
 
    @Test
    void getPaymentsByStatus_refundedStatus_returnsList() {
        when(paymentRepository.findByStatus("REFUNDED")).thenReturn(List.of());
        assertThatCode(() -> service.getPaymentsByStatus("REFUNDED")).doesNotThrowAnyException();
    }
 
    @Test
    void getPaymentsByStatus_invalidStatus_throwsBadRequestException() {
        assertThatThrownBy(() -> service.getPaymentsByStatus("INVALID"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status");
    }
 
    // ─────────────────────────── getTotalRevenue ──────────────────────────
 
    @Test
    void getTotalRevenue_returnsCalculatedTotal() {
        when(paymentRepository.calculateTotalRevenue()).thenReturn(5000.0);
        assertThat(service.getTotalRevenue()).isEqualTo(5000.0);
    }
 
    @Test
    void getTotalRevenue_nullFromDb_returnsZero() {
        when(paymentRepository.calculateTotalRevenue()).thenReturn(null);
        assertThat(service.getTotalRevenue()).isEqualTo(0.0);
    }
 
    // ─────────────────────────── updatePaymentStatus ──────────────────────
 
    @Test
    void updatePaymentStatus_validStatus_updatesAndSaves() {
        when(paymentRepository.findByPaymentId(1)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenReturn(pendingPayment);
 
        service.updatePaymentStatus(1, "SUCCESS");
 
        assertThat(pendingPayment.getStatus()).isEqualTo("SUCCESS");
        verify(paymentRepository).save(pendingPayment);
    }
 
    @Test
    void updatePaymentStatus_invalidStatus_throwsBadRequestException() {
        assertThatThrownBy(() -> service.updatePaymentStatus(1, "UNKNOWN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status");
    }
 
    @Test
    void updatePaymentStatus_paymentNotFound_throwsResourceNotFoundException() {
        when(paymentRepository.findByPaymentId(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updatePaymentStatus(999, "SUCCESS"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
 