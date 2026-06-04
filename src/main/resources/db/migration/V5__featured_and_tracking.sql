ALTER TABLE products ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE orders ADD COLUMN tracking_number VARCHAR(50);

CREATE INDEX idx_products_featured ON products(featured);
