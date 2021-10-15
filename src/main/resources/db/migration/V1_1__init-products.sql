CREATE TABLE PRODUCTS (
                       id INT NOT NULL AUTO_INCREMENT,
                       product_name VARCHAR2(100) NOT NULL,
                       seller_id INT NOT NULL,
                       cost INT NOT NULL,
                       amount_available INT NOT NULL,
                       PRIMARY KEY (id),
                       FOREIGN KEY (seller_id) references USERS(id)
);