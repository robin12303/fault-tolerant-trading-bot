CREATE TABLE positions (
    symbol varchar(20) PRIMARY KEY,
    base_qty decimal(30,8) NOT NULL,
    updated_at datetime(6) NOT NULL
);