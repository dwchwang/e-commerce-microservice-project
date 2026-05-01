package com.ecommerce.notification.service.impl;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceImplTest {

    @Test
    void sendsUtf8MimeMessageWithConfiguredFromAddress() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        EmailServiceImpl service = new EmailServiceImpl(mailSender);
        ReflectionTestUtils.setField(service, "from", "shop@example.com");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.send("buyer@example.com", "Đơn hàng đã được xác nhận", "Cảm ơn bạn đã mua hàng.");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).isEqualTo("Đơn hàng đã được xác nhận");
        assertThat(sent.getFrom()).containsExactly(new InternetAddress("shop@example.com"));
        assertThat(sent.getAllRecipients()).containsExactly(new InternetAddress("buyer@example.com"));
    }
}
