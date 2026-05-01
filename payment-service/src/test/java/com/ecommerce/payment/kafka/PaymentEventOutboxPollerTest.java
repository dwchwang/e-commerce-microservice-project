package com.ecommerce.payment.kafka;

import com.ecommerce.payment.entity.PaymentOutboxEvent;
import com.ecommerce.payment.entity.PaymentOutboxStatus;
import com.ecommerce.payment.repository.PaymentOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentEventOutboxPollerTest {

    private final PaymentOutboxRepository outboxRepository = mock(PaymentOutboxRepository.class);
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final PaymentEventOutboxPoller poller = new PaymentEventOutboxPoller(outboxRepository, kafkaTemplate);

    @Test
    void publishFailureDoesNotBlockRemainingBatchAndMarksFailedAfterMaxAttempts() {
        PaymentOutboxEvent failing = outboxEvent("payment-success", "order-1", 4);
        PaymentOutboxEvent succeeding = outboxEvent("payment-failed", "order-2", 0);

        when(outboxRepository.findPendingBatch()).thenReturn(List.of(failing, succeeding));
        when(kafkaTemplate.send("payment-success", "order-1", "{}"))
                .thenThrow(new KafkaException("broker unavailable"));
        when(kafkaTemplate.send("payment-failed", "order-2", "{}"))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        poller.poll();

        assertThat(failing.getAttempts()).isEqualTo(5);
        assertThat(failing.getStatus()).isEqualTo(PaymentOutboxStatus.FAILED);
        assertThat(failing.getLastError()).isEqualTo("broker unavailable");

        assertThat(succeeding.getStatus()).isEqualTo(PaymentOutboxStatus.PUBLISHED);
        assertThat(succeeding.getPublishedAt()).isNotNull();
        verify(kafkaTemplate).send("payment-failed", "order-2", "{}");
    }

    @Test
    void publishFailureKeepsEventPendingBeforeMaxAttempts() {
        PaymentOutboxEvent event = outboxEvent("payment-success", "order-1", 1);

        when(outboxRepository.findPendingBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new KafkaException("temporary failure"));

        poller.poll();

        assertThat(event.getAttempts()).isEqualTo(2);
        assertThat(event.getStatus()).isEqualTo(PaymentOutboxStatus.PENDING);
        assertThat(event.getLastError()).isEqualTo("temporary failure");
    }

    private PaymentOutboxEvent outboxEvent(String topic, String aggregateId, int attempts) {
        return PaymentOutboxEvent.builder()
                .aggregateId(aggregateId)
                .eventType("PAYMENT_EVENT")
                .topic(topic)
                .payload("{}")
                .status(PaymentOutboxStatus.PENDING)
                .attempts(attempts)
                .build();
    }
}
