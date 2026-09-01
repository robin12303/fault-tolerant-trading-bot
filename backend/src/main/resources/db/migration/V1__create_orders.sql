CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,

                        client_order_id VARCHAR(64) NOT NULL,
                        exchange_order_id BIGINT NULL,

                        symbol VARCHAR(20) NOT NULL,
                        side VARCHAR(10) NOT NULL,
                        type VARCHAR(20) NOT NULL,
                        status VARCHAR(30) NOT NULL,

                        requested_quote_qty DECIMAL(30, 8) NULL,
                        requested_base_qty DECIMAL(30, 8) NULL,

                        executed_qty DECIMAL(30, 8) NOT NULL DEFAULT 0,
                        executed_quote_qty DECIMAL(30, 8) NOT NULL DEFAULT 0,

                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL,

                        CONSTRAINT uk_orders_client_order_id
                            UNIQUE (client_order_id)
);