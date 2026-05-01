CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_flash_sale_user
ON orders(flash_sale_id, user_id)
WHERE is_flash_sale = TRUE AND flash_sale_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_orders_flash_sale
ON orders(flash_sale_id)
WHERE is_flash_sale = TRUE AND flash_sale_id IS NOT NULL;
