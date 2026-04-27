package com.medibook.payment.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.payment.client.AppointmentClient;
import com.medibook.payment.dto.request.AppointmentDto;
import com.medibook.payment.dto.request.PaymentRequest;
import com.medibook.payment.dto.response.PaymentResponse;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.exception.BadRequestException;
import com.medibook.payment.exception.DuplicateResourceException;
import com.medibook.payment.exception.ResourceNotFoundException;
import com.medibook.payment.repository.PaymentRepository;
import com.medibook.payment.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Replaces direct @Autowired AppointmentService.
     * Calls appointment-service via Feign: GET /appointments/{id}
     */
    @Autowired
    private AppointmentClient appointmentClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String razorpayCurrency;

    // ── Inner class ────────────────────────────────────────────
    private static class GatewayResponse {
        String orderId; String paymentId; String status;
        GatewayResponse(String o, String p, String s) { orderId=o; paymentId=p; status=s; }
    }

    private GatewayResponse callGateway(PaymentRequest request) {
        return razorpayGateway(request);
    }

    private boolean callRefundGateway(String razorpayPaymentId) {
        return razorpayRefund(razorpayPaymentId);
    }

    private GatewayResponse razorpayGateway(PaymentRequest request) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(request.getAmount() * 100));
            orderRequest.put("currency", razorpayCurrency);
            orderRequest.put("receipt", "appt_" + request.getAppointmentId());
            orderRequest.put("payment_capture", 1);
            Order order = client.orders.create(orderRequest);
            return new GatewayResponse(order.get("id"), null, "PENDING");
        } catch (RazorpayException e) {
            throw new BadRequestException("Razorpay order creation failed: " + e.getMessage());
        }
    }

    private boolean razorpayRefund(String razorpayPaymentId) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject refundRequest = new JSONObject();
            Refund refund = client.payments.refund(razorpayPaymentId, refundRequest);
            String refundStatus = refund.get("status");
            return refundStatus.equals("processed") || refundStatus.equals("initiated");
        } catch (RazorpayException e) {
            throw new BadRequestException("Razorpay refund failed: " + e.getMessage());
        }
    }

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        if (orderId != null && orderId.startsWith("MOCK_")) return true;
        try {
            String message = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(razorpayKeySecret.getBytes("UTF-8"), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(message.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            throw new BadRequestException("Signature verification failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {

        // Feign call to appointment-service GET /appointments/{id}
        // Replaces: appointmentService.getById(request.getAppointmentId())
        AppointmentDto appointment = appointmentClient.getById(request.getAppointmentId());

        if (!appointment.getStatus().equals("SCHEDULED") && !appointment.getStatus().equals("PENDING_PAYMENT")) {
            throw new BadRequestException(
                "Payment can only be made for scheduled appointments. Status: " + appointment.getStatus());
        }

        if (paymentRepository.findByAppointmentId(request.getAppointmentId()).isPresent()) {
            throw new DuplicateResourceException(
                "Payment already exists for appointment: " + request.getAppointmentId());
        }

        GatewayResponse gatewayResponse = callGateway(request);

        Payment payment = Payment.builder()
                .appointmentId(request.getAppointmentId())
                .patientId(request.getPatientId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(gatewayResponse.status)
                .razorpayOrderId(gatewayResponse.orderId)
                .razorpayPaymentId(gatewayResponse.paymentId)
                .notes("Payment initiated via " + request.getPaymentMethod())
                .build();

        Payment saved = paymentRepository.save(payment);
        return buildResponse(saved, "Order created. Complete payment in popup.");
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", razorpayOrderId));

        if (payment.getStatus().equals("SUCCESS"))
            throw new BadRequestException("Payment is already verified and successful.");

        boolean isValidSignature = verifyRazorpaySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

        if (!isValidSignature) {
            payment.setStatus("FAILED");
            payment.setNotes("Payment failed — invalid signature detected.");
            paymentRepository.save(payment);
            throw new BadRequestException("Payment verification failed. Invalid signature.");
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setStatus("SUCCESS");  // Payment Successful
        payment.setRazorpayPaymentId("TXN_" + System.currentTimeMillis());  // Transaction ID
        payment.setNotes("Payment verified via Razorpay. Transaction: " + razorpayPaymentId);  // Payment Completed
        
        try {
            appointmentClient.markCompleted(payment.getAppointmentId());
            System.out.println("✅ Appointment marked completed");
        } catch (Exception e) {
            System.out.println("❌ Failed to call appointment-service: " + e.getMessage());
        }

        Payment saved = paymentRepository.save(payment);
        return buildResponse(saved, "Payment successful. Appointment confirmed.");
    }

    @Override
    public PaymentResponse getPaymentByAppointment(int appointmentId) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "appointmentId", appointmentId));
        return buildResponse(payment, "Payment details retrieved.");
    }

    @Override
    public PaymentResponse getPaymentById(int paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        return buildResponse(payment, "Payment details retrieved.");
    }

    @Override
    public List<Payment> getPaymentsByPatient(int patientId) {
        return paymentRepository.findByPatientId(patientId);
    }

    @Override
    public List<Payment> getPaymentsByProvider(int providerId) {
        return paymentRepository.findPaymentsByProvider(providerId);
    }

    @Override
    @Transactional
    public PaymentResponse initiateRefund(int paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (!payment.getStatus().equals("SUCCESS"))
            throw new BadRequestException("Refund can only be initiated for successful payments. Status: " + payment.getStatus());

        boolean refundSuccess = callRefundGateway(payment.getRazorpayPaymentId());

        if (refundSuccess) {
            payment.setStatus("REFUNDED");
            payment.setNotes("Refund initiated via Razorpay. Amount will be credited in 5-7 business days.");
        } else {
            throw new BadRequestException("Refund failed. Please contact support.");
        }

        Payment saved = paymentRepository.save(payment);
        return buildResponse(saved, "Refund initiated successfully.");
    }

    @Override
    public List<Payment> getPaymentsByStatus(String status) {
        if (!status.equals("PENDING") && !status.equals("SUCCESS")
                && !status.equals("FAILED") && !status.equals("REFUNDED")) {
            throw new BadRequestException("Invalid status. Allowed: PENDING, SUCCESS, FAILED, REFUNDED");
        }
        return paymentRepository.findByStatus(status);
    }

    @Override
    public double getTotalRevenue() {
        Double total = paymentRepository.calculateTotalRevenue();
        return total != null ? total : 0.0;
    }

    @Override
    public void updatePaymentStatus(int paymentId, String status) {
        if (!status.equals("PENDING") && !status.equals("SUCCESS")
                && !status.equals("FAILED") && !status.equals("REFUNDED")) {
            throw new BadRequestException("Invalid status.");
        }
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        payment.setStatus(status);
        payment.setNotes("Status manually updated to: " + status);
        paymentRepository.save(payment);
    }

    private PaymentResponse buildResponse(Payment payment, String message) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .appointmentId(payment.getAppointmentId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .message(message)
                .transactionTime(
                    payment.getCreatedAt() != null
                        ? payment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
                .build();
    }
}
