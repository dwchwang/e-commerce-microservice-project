CREATE UNIQUE INDEX ux_payments_completed_cod ON payments(order_id)
    WHERE status = 'COMPLETED' AND payment_method = 'COD';
