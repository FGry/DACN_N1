create database BooHhub;

use BookHub;


CREATE TABLE Users (
    id_user INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) CHARACTER SET utf8mb4 NOT NULL,
    password VARCHAR(50) CHARACTER SET utf8mb4 NOT NULL,
    email VARCHAR(255) CHARACTER SET utf8mb4 ,
    gender VARCHAR(10) CHARACTER SET utf8mb4 NOT NULL,
    phone VARCHAR(11) CHARACTER SET utf8mb4 NOT NULL,
    roles VARCHAR(10) CHARACTER SET utf8mb4 NOT NULL,
    update_date DATE NOT NULL,
    create_date DATE NOT NULL
);

CREATE TABLE Addresses (
    id_address INT AUTO_INCREMENT PRIMARY KEY,
    address VARCHAR(255) CHARACTER SET utf8mb4,
    phone VARCHAR(11) CHARACTER SET utf8mb4,
    Users_id_user INT,
    FOREIGN KEY (Users_id_user) REFERENCES Users(id_user)
);

CREATE TABLE Products (
    id_products INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL,
    price BIGINT NOT NULL,
    author VARCHAR(255) CHARACTER SET utf8mb4,
    publisher VARCHAR(365) CHARACTER SET utf8mb4,
    publication_year DATE,
    pages INT,
    stock_quantity INT,
    language VARCHAR(255) CHARACTER SET utf8mb4,
    discount INT,
    description VARCHAR(255) CHARACTER SET utf8mb4
);

CREATE TABLE Image_products (
    id_image_product INT AUTO_INCREMENT PRIMARY KEY,
    image_link VARCHAR(255) CHARACTER SET utf8mb4,
    Products_id_products INT,
    FOREIGN KEY (Products_id_products) REFERENCES Products(id_products)
);

CREATE TABLE Categories (
    id_categories INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) CHARACTER SET utf8mb4 NOT NULL,
    description VARCHAR(255) CHARACTER SET utf8mb4
);

CREATE TABLE Categories_Products (
    Categories_id_categories INT,
    Products_id_products INT,
    PRIMARY KEY (Categories_id_categories, Products_id_products),
    FOREIGN KEY (Categories_id_categories) REFERENCES Categories(id_categories),
    FOREIGN KEY (Products_id_products) REFERENCES Products(id_products)
);

CREATE TABLE Comments (
    id_comment INT AUTO_INCREMENT PRIMARY KEY,
    date DATE,
    messages VARCHAR(255) CHARACTER SET utf8mb4,
    rate TINYINT,
    Users_id_user INT,
    Products_id_products INT,
    FOREIGN KEY (Users_id_user) REFERENCES Users(id_user),
    FOREIGN KEY (Products_id_products) REFERENCES Products(id_products)
);

CREATE TABLE Vouchers (
    id_vouchers INT AUTO_INCREMENT PRIMARY KEY,
    code_name VARCHAR(255) CHARACTER SET utf8mb4,
    discount_percent INT,
    discount_value BIGINT,
    max_discount BIGINT,
    min_order_value BIGINT,
    start_date DATE,
    end_date DATE,
    Users_id_user INT,
    FOREIGN KEY (Users_id_user) REFERENCES Users(id_user)
);


CREATE TABLE Orders (
    id_order INT AUTO_INCREMENT PRIMARY KEY,
    address VARCHAR(255) CHARACTER SET utf8mb4,
    date DATE,
    phone VARCHAR(11) CHARACTER SET utf8mb4,
    status_order VARCHAR(20) CHARACTER SET utf8mb4,
    total BIGINT,
    payment_method VARCHAR(20) CHARACTER SET utf8mb4,
    note VARCHAR(255) CHARACTER SET utf8mb4,
    Users_id_user INT,
    Vouchers_id_vouchers INT,
    FOREIGN KEY (Users_id_user) REFERENCES Users(id_user),
    FOREIGN KEY (Vouchers_id_vouchers) REFERENCES Vouchers(id_vouchers)
);

CREATE TABLE Order_details (
    id_order_detail INT AUTO_INCREMENT PRIMARY KEY,
    discount INT,
    price_date BIGINT,
    quantity BIGINT,
    total BIGINT,
    Orders_id_order INT,
    Products_id_products INT,
    FOREIGN KEY (Orders_id_order) REFERENCES Orders(id_order),
    FOREIGN KEY (Products_id_products) REFERENCES Products(id_products)
);

CREATE TABLE Carts (
    id_cart INT AUTO_INCREMENT PRIMARY KEY,
    quantity INT,
    Users_id_user INT,
    Products_id_products INT,
    description VARCHAR(255) CHARACTER SET utf8mb4,
    FOREIGN KEY (Users_id_user) REFERENCES Users(id_user),
    FOREIGN KEY (Products_id_products) REFERENCES Products(id_products)
);
