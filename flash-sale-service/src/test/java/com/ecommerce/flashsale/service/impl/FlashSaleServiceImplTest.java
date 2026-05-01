package com.ecommerce.flashsale.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.flashsale.dto.CampaignResponse;
import com.ecommerce.flashsale.dto.CreateCampaignRequest;
import com.ecommerce.flashsale.dto.PaymentMethod;
import com.ecommerce.flashsale.dto.PurchaseRequest;
import com.ecommerce.flashsale.dto.PurchaseResponse;
import com.ecommerce.flashsale.entity.CampaignStatus;
import com.ecommerce.flashsale.entity.FlashSaleCampaign;
import com.ecommerce.flashsale.exception.SlotReservationException;
import com.ecommerce.flashsale.kafka.FlashSaleEventProducer;
import com.ecommerce.flashsale.repository.FlashSaleCampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlashSaleServiceImplTest {

    @Mock
    private FlashSaleCampaignRepository campaignRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FlashSaleEventProducer eventProducer;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Test
    void purchasePublishesEventAndIncrementsSoldCount() throws Exception {
        UUID campaignId = UUID.randomUUID();
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(activeCampaign(campaignId)));
        when(redisTemplate.execute(anyRedisScript(), anyList(), eq("user-1"), anyString())).thenReturn(4L);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Integer> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        when(campaignRepository.incrementSoldCount(campaignId)).thenReturn(1);

        FlashSaleServiceImpl service = new FlashSaleServiceImpl(
                campaignRepository, redisTemplate, eventProducer, transactionTemplate);

        PurchaseResponse response = service.purchase(campaignId, "user-1", "user@example.com", purchaseRequest());

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getRemainingStock()).isEqualTo(4);
        verify(eventProducer).publishSync(any());
        verify(campaignRepository).incrementSoldCount(campaignId);
    }

    @Test
    void purchaseCompensatesRedisSlotWhenKafkaPublishFails() throws Exception {
        UUID campaignId = UUID.randomUUID();
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(activeCampaign(campaignId)));
        when(redisTemplate.execute(anyRedisScript(), anyList(), eq("user-1"), anyString())).thenReturn(4L);
        doThrow(new IllegalStateException("broker unavailable")).when(eventProducer).publishSync(any());

        FlashSaleServiceImpl service = new FlashSaleServiceImpl(
                campaignRepository, redisTemplate, eventProducer, transactionTemplate);

        assertThatThrownBy(() -> service.purchase(campaignId, "user-1", "user@example.com", purchaseRequest()))
                .isInstanceOf(SlotReservationException.class)
                .hasMessage("Cannot create flash sale order. Please retry.");

        verify(redisTemplate).execute(anyRedisScript(), anyList(), eq("user-1"));
        verify(campaignRepository, never()).incrementSoldCount(campaignId);
    }

    @Test
    void purchaseRejectsSoldOutWithoutPublishingEvent() throws Exception {
        UUID campaignId = UUID.randomUUID();
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(activeCampaign(campaignId)));
        when(redisTemplate.execute(anyRedisScript(), anyList(), eq("user-1"), anyString())).thenReturn(-1L);

        FlashSaleServiceImpl service = new FlashSaleServiceImpl(
                campaignRepository, redisTemplate, eventProducer, transactionTemplate);

        assertThatThrownBy(() -> service.purchase(campaignId, "user-1", "user@example.com", purchaseRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Sold out");

        verify(eventProducer, never()).publishSync(any());
    }

    @Test
    void purchaseRejectsDuplicateBuyerWithoutPublishingEvent() throws Exception {
        UUID campaignId = UUID.randomUUID();
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(activeCampaign(campaignId)));
        when(redisTemplate.execute(anyRedisScript(), anyList(), eq("user-1"), anyString())).thenReturn(-2L);

        FlashSaleServiceImpl service = new FlashSaleServiceImpl(
                campaignRepository, redisTemplate, eventProducer, transactionTemplate);

        assertThatThrownBy(() -> service.purchase(campaignId, "user-1", "user@example.com", purchaseRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("You already purchased this campaign");

        verify(eventProducer, never()).publishSync(any());
    }

    @Test
    void purchaseRejectsMissingRedisStockWithoutPublishingEvent() throws Exception {
        UUID campaignId = UUID.randomUUID();
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(activeCampaign(campaignId)));
        when(redisTemplate.execute(anyRedisScript(), anyList(), eq("user-1"), anyString())).thenReturn(-3L);

        FlashSaleServiceImpl service = new FlashSaleServiceImpl(
                campaignRepository, redisTemplate, eventProducer, transactionTemplate);

        assertThatThrownBy(() -> service.purchase(campaignId, "user-1", "user@example.com", purchaseRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Campaign stock is not ready");

        verify(eventProducer, never()).publishSync(any());
    }

    @Test
    void createCampaignRejectsSalePriceGreaterThanOriginalPrice() {
        FlashSaleServiceImpl service = new FlashSaleServiceImpl(
                campaignRepository, redisTemplate, eventProducer, transactionTemplate);
        CreateCampaignRequest request = campaignRequest();
        request.setSalePrice(BigDecimal.valueOf(120));
        request.setOriginalPrice(BigDecimal.valueOf(100));

        assertThatThrownBy(() -> service.createCampaign(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Sale price must be less than original price");

        verify(campaignRepository, never()).save(any());
    }

    @Test
    void updateCampaignReturnsRemainingStockFromRedis() {
        UUID campaignId = UUID.randomUUID();
        FlashSaleCampaign campaign = scheduledCampaign(campaignId);
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(FlashSaleCampaign.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("flash_sale:" + campaignId + ":stock")).thenReturn("7");

        FlashSaleServiceImpl service = new FlashSaleServiceImpl(
                campaignRepository, redisTemplate, eventProducer, transactionTemplate);

        CampaignResponse response = service.updateCampaign(campaignId, campaignRequest());

        assertThat(response.getRemainingStock()).isEqualTo(7);
    }

    @SuppressWarnings("unchecked")
    private RedisScript<Long> anyRedisScript() {
        return any(RedisScript.class);
    }

    private FlashSaleCampaign activeCampaign(UUID campaignId) {
        return FlashSaleCampaign.builder()
                .id(campaignId)
                .productId(UUID.randomUUID())
                .sku("SKU-FLASH")
                .productName("Flash product")
                .salePrice(BigDecimal.valueOf(9.99))
                .quantity(5)
                .soldCount(0)
                .startTime(LocalDateTime.now().minusMinutes(1))
                .endTime(LocalDateTime.now().plusMinutes(5))
                .status(CampaignStatus.ACTIVE)
                .build();
    }

    private FlashSaleCampaign scheduledCampaign(UUID campaignId) {
        return FlashSaleCampaign.builder()
                .id(campaignId)
                .productId(UUID.randomUUID())
                .sku("SKU-FLASH")
                .productName("Flash product")
                .originalPrice(BigDecimal.valueOf(20))
                .salePrice(BigDecimal.valueOf(9.99))
                .quantity(5)
                .soldCount(0)
                .startTime(LocalDateTime.now().plusMinutes(5))
                .endTime(LocalDateTime.now().plusMinutes(10))
                .status(CampaignStatus.SCHEDULED)
                .build();
    }

    private CreateCampaignRequest campaignRequest() {
        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setProductId(UUID.randomUUID());
        request.setSku("SKU-FLASH");
        request.setProductName("Flash product");
        request.setOriginalPrice(BigDecimal.valueOf(20));
        request.setSalePrice(BigDecimal.valueOf(9.99));
        request.setQuantity(5);
        request.setStartTime(LocalDateTime.now().plusMinutes(5));
        request.setEndTime(LocalDateTime.now().plusMinutes(10));
        return request;
    }

    private PurchaseRequest purchaseRequest() {
        PurchaseRequest request = new PurchaseRequest();
        request.setPaymentMethod(PaymentMethod.COD);
        request.setShippingName("Test User");
        request.setShippingPhone("0901234567");
        request.setShippingAddress("123 Test Street");
        return request;
    }
}
