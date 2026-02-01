#!/bin/bash

echo "Testing OpenRudder CDC Flow"
echo "============================"
echo ""

echo "Step 1: Inserting a new order that matches the query..."
docker exec -i openrudder-postgres psql -U postgres -d orders << 'EOF'
INSERT INTO orders (customer, status, location, driver_assigned) 
VALUES ('Test Customer', 'READY_FOR_PICKUP', '123 Test Street', FALSE);
EOF

echo ""
echo "Step 2: Waiting 5 seconds for CDC to process..."
sleep 5

echo ""
echo "Step 3: Checking the orders table..."
docker exec openrudder-postgres psql -U postgres -d orders -c "SELECT * FROM orders WHERE customer LIKE 'Test%';"

echo ""
echo "Step 4: Updating an order to trigger CDC..."
docker exec -i openrudder-postgres psql -U postgres -d orders << 'EOF'
UPDATE orders 
SET status = 'READY_FOR_PICKUP' 
WHERE customer = 'John Doe';
EOF

echo ""
echo "Step 5: Waiting 5 seconds for CDC to process..."
sleep 5

echo ""
echo "Done! Check the application logs for CDC events and AI responses."
echo "Look for log lines containing:"
echo "  - 'Change event from source'"
echo "  - 'Query matched event'"
echo "  - 'AI Dispatcher Response'"
