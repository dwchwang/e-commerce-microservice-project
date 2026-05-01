CREATE TABLE flash_sale_campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    sku VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    original_price DECIMAL(15,2),
    sale_price DECIMAL(15,2) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    sold_count INTEGER NOT NULL DEFAULT 0 CHECK (sold_count >= 0),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT chk_flash_sale_time CHECK (end_time > start_time),
    CONSTRAINT chk_flash_sale_sold_count CHECK (sold_count <= quantity)
);

CREATE INDEX idx_flash_sale_status_time ON flash_sale_campaigns(status, start_time, end_time);
CREATE INDEX idx_flash_sale_product ON flash_sale_campaigns(product_id);
CREATE INDEX idx_flash_sale_sku ON flash_sale_campaigns(sku);
