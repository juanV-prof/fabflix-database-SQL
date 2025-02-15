DELIMITER $$

CREATE PROCEDURE add_movie(
    IN movieTitle VARCHAR(255),
    IN movieYear INT,
    IN movieDirector VARCHAR(255),
    IN starName VARCHAR(255),
    IN starBirthYear INT,
    IN genreName VARCHAR(255),
    OUT movieID VARCHAR(20),
    OUT starID VARCHAR(20),
    OUT genreID INT,
    OUT success BOOLEAN
)
proc_begin: BEGIN
    DECLARE maxMovieID VARCHAR(20);
    DECLARE numPart INT;
    DECLARE randomRating DECIMAL(2,1);
    DECLARE randomNumVotes INT;

    SELECT id INTO movieID FROM movies WHERE title = movieTitle AND year = movieYear AND director = movieDirector LIMIT 1;

    IF movieID IS NOT NULL THEN
        SET success = FALSE;
        LEAVE proc_begin;
    END IF;

    SELECT MAX(id) INTO maxMovieID FROM movies WHERE id LIKE 'tt%';

    IF maxMovieID IS NOT NULL THEN
        SET numPart = CAST(SUBSTRING(maxMovieID, 3) AS UNSIGNED) + 1;
        SET movieID = CONCAT('tt', LPAD(numPart, 7, '0'));
    ELSE
        SET movieID = 'tt0000001';
    END IF;

    INSERT INTO movies (id, title, year, director, price) 
    VALUES (movieID, movieTitle, movieYear, movieDirector, FLOOR(10 + (RAND() * 91)));

    SET success = TRUE;

    SELECT id INTO starID FROM stars WHERE name = starName LIMIT 1;

    IF starID IS NULL THEN
        SELECT MAX(id) INTO starID FROM stars WHERE id LIKE 'nm%';
        IF starID IS NOT NULL THEN
            SET numPart = CAST(SUBSTRING(starID, 3) AS UNSIGNED) + 1;
            SET starID = CONCAT('nm', LPAD(numPart, 7, '0'));
        ELSE
            SET starID = 'nm0000001';
        END IF;
        
        INSERT INTO stars (id, name, birthYear) 
        VALUES (starID, starName, IF(starBirthYear IS NULL OR starBirthYear = 0, NULL, starBirthYear));
    END IF;

    SELECT id INTO genreID FROM genres WHERE name = genreName LIMIT 1;

    IF genreID IS NULL THEN
        INSERT INTO genres (name) VALUES (genreName);
        SET genreID = LAST_INSERT_ID();
    END IF;

    INSERT IGNORE INTO genres_in_movies (movieId, genreId) VALUES (movieID, genreID);
    INSERT IGNORE INTO stars_in_movies (starId, movieId) VALUES (starID, movieID);

    SET randomRating = ROUND(RAND() * 10, 1);
    SET randomNumVotes = FLOOR(1 + RAND() * 299999);

    INSERT INTO ratings (movieId, rating, numVotes) VALUES (movieID, randomRating, randomNumVotes);

END proc_begin$$

DELIMITER ;