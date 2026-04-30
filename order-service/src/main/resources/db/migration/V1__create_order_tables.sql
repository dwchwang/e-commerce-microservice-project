CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    user_email VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20) NOT NULL,
    subtotal DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    total_amount DECIMAL(15,2) NOT NULL,
    voucher_code VARCHAR(50),
    shipping_name VARCHAR(255),
    shipping_phone VARCHAR(20),
    shipping_address VARCHAR(500),
    cancel_reason VARCHAR(500),
    is_flash_sale BOOLEAN DEFAULT FALSE,
    flash_sale_id UUID,
    reservation_expired_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_reservation_expiry ON orders(status, payment_method, reservation_expired_at);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    sku VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal DECIMAL(15,2) NOT NULL
);

CREATE INDEX idx_order_items_order ON order_items(order_id);

CREATE TABLE outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP
);

CREATE INDEX idx_outbox_unprocessed ON outbox(processed, created_at) WHERE processed = FALSE;

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP DEFAULT NOW()
);
