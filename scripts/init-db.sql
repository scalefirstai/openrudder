-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    customer VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    location VARCHAR(255),
    driver_assigned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create drivers table
CREATE TABLE IF NOT EXISTS drivers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    available BOOLEAN DEFAULT TRUE,
    location VARCHAR(255),
    capacity INTEGER DEFAULT 10,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT INTO orders (customer, status, location, driver_assigned) VALUES
    ('John Doe', 'PREPARING', '123 Main St', FALSE),
    ('Jane Smith', 'READY_FOR_PICKUP', '456 Oak Ave', FALSE),
    ('Bob Johnson', 'PREPARING', '789 Pine Rd', FALSE);

INSERT INTO drivers (name, available, location, capacity) VALUES
    ('Driver A', TRUE, '100 Main St', 15),
    ('Driver B', TRUE, '200 Oak Ave', 10),
    ('Driver C', FALSE, '300 Pine Rd', 20);

-- Create publication for CDC
CREATE PUBLICATION openrudder_publication FOR ALL TABLES;
