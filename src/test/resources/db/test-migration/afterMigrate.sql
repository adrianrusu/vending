insert into users(id, username, password, role, deposit) values (8, 'buyer', 'buyer', 'BUYER', 15);
insert into users(id, username, password, role, deposit) values (9, 'seller', 'seller', 'SELLER', 0);
insert into users(id, username, password, role, deposit) values (10, 'test-seller', 'seller', 'SELLER', 0);
insert into users(id, username, password, role, deposit) values (11, 'test-buyer', 'seller', 'SELLER', 125);

insert into products(id, product_name, seller_id, cost, amount_available) values (1, 'Coca Cola', 9, 12, 10);
insert into products(id, product_name, seller_id, cost, amount_available) values (2, 'Pepsi', 9, 5, 5);
insert into products(id, product_name, seller_id, cost, amount_available) values (3, 'Snickers', 9, 3, 12);
insert into products(id, product_name, seller_id, cost, amount_available) values (4, 'Fanta', 10, 6, 7);