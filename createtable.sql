CREATE TABLE movies(
    id varchar(10) PRIMARY KEY,
    title varchar(100) NOT NULL DEFAULT '',
    year int NOT NULL,
    director varchar(100) NOT NULL DEFAULT ''
);

CREATE TABLE stars(
    id varchar(10) PRIMARY KEY,
    name varchar(100) NOT NULL DEFAULT '',
    birthYear int NULL
);

CREATE TABLE stars_in_movies(
    starId varchar(10) REFERENCES stars(id) NOT NULL,
    movieId varchar(10) REFERENCES movies(id) NOT NULL
);

CREATE TABLE genres (
    id int AUTO_INCREMENT PRIMARY KEY,
    name varchar(32) NOT NULL DEFAULT ''
);

CREATE TABLE genres_in_movies (
    genreId int REFERENCES genres(id) NOT NULL,
    movieId varchar(10) REFERENCES movies(id) NOT NULL
);

CREATE TABLE customers(
    id int AUTO_INCREMENT PRIMARY KEY,
    firstName varchar(50) NOT NULL DEFAULT '',
    lastName varchar(50) NOT NULL DEFAULT '',
    ccId varchar(20) REFERENCES creditcards(id) NOT NULL,
    address varchar(200) NOT NULL DEFAULT '',
    email varchar(50) NOT NULL DEFAULT '',
    password varchar(20) NOT NULL DEFAULT ''
);

CREATE TABLE sales(
    id int AUTO_INCREMENT PRIMARY KEY,
    customerId int REFERENCES customers(id) NOT NULL,
    movieId varchar(10) REFERENCES movies(id) NOT NULL,
    saleDate date NOT NULL
);

CREATE TABLE creditcards(
    id varchar(20) PRIMARY KEY,
    firstName varchar(50) NOT NULL DEFAULT '',
    lastName varchar(50) NOT NULL DEFAULT '',
    expiration date NOT NULL
);

CREATE TABLE ratings(
    movieId varchar(10) REFERENCES movies(id) NOT NULL,
    rating float  NOT NULL,
    numVotes int NOT NULL
);