package com.ecommerce.flashsale.util;

import java.util.UUID;

public final class FlashSaleRedisKeys {

    private static final String PREFIX = "flash_sale:";

    private FlashSaleRedisKeys() {
    }

    public static String stockKey(UUID campaignId) {
        return PREFIX + campaignId + ":stock";
    }

    public static String buyersKey(UUID campaignId) {
        return PREFIX + campaignId + ":buyers";
    }
}
