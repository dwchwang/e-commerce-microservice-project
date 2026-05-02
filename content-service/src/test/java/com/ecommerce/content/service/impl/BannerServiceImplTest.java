package com.ecommerce.content.service.impl;

import com.ecommerce.content.dto.BannerRequest;
import com.ecommerce.content.entity.Banner;
import com.ecommerce.content.repository.BannerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BannerServiceImplTest {

    @Mock
    private BannerRepository bannerRepository;

    private BannerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BannerServiceImpl(bannerRepository);
    }

    @Test
    void activeBannersUseRepositoryWindowQuery() {
        when(bannerRepository.findActiveBanners(any())).thenReturn(List.of(Banner.builder()
                .title("Sale")
                .imageUrl("https://example.test/banner.jpg")
                .displayOrder(1)
                .isActive(true)
                .build()));

        assertThat(service.getActiveBanners()).hasSize(1);
        verify(bannerRepository).findActiveBanners(any());
    }

    @Test
    void createBannerDefaultsActiveAndDisplayOrder() {
        BannerRequest request = new BannerRequest();
        request.setTitle("Sale");
        request.setImageUrl("https://example.test/banner.jpg");
        when(bannerRepository.save(any(Banner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createBanner(request);

        ArgumentCaptor<Banner> captor = ArgumentCaptor.forClass(Banner.class);
        verify(bannerRepository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isTrue();
        assertThat(captor.getValue().getDisplayOrder()).isZero();
    }
}
