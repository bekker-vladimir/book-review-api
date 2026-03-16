-- Remove conflicting tables
DROP TABLE IF EXISTS author CASCADE;
DROP TABLE IF EXISTS book CASCADE;
DROP TABLE IF EXISTS book_author CASCADE;
DROP TABLE IF EXISTS review CASCADE;
DROP TABLE IF EXISTS "user" CASCADE;
DROP TABLE IF EXISTS complaint CASCADE;
-- End of removing

-- Insert into author
INSERT INTO author (full_name)
VALUES ('Agatha Christie'),
       ('J.R.R. Tolkien'),
       ('Stephen King'),
       ('C.S. Lewis'),
       ('Fyodor Dostoevsky'),
       ('Haruki Murakami'),
       ('Simone de Beauvoir'),
       ('Charles Dickens'),
       ('Gabriel García Márquez'),
       ('Margaret Atwood'),
       ('Johann Wolfgang von Goethe'),
       ('Dante Alighieri'),
       ('John One'),
       ('Arthur Two'),
       ('Huston Three');


-- Insert into book
INSERT INTO book (description, genre, publication_date, title)
VALUES ('A detective novel featuring Hercule Poirot.', 'Mystery', '1920-01-01', 'The Mysterious Affair at Styles'),
       ('A high fantasy novel set in Middle-earth.', 'Fantasy', '1954-07-29', 'The Fellowship of the Ring'),
       ('A horror novel about a haunted hotel.', 'Horror', '1977-01-28', 'The Shining'),
       ('A fantasy novel about four siblings and a magical wardrobe.', 'Fantasy', '1950-10-16',
        'The Lion, the Witch and the Wardrobe'),
       ('A psychological novel exploring morality and redemption.', 'Philosophical', '1866-01-01',
        'Crime and Punishment'),
       ('A surreal novel about loneliness and love.', 'Magical Realism', '1987-01-01', 'Norwegian Wood'),
       ('A novel about existentialism and feminism.', 'Philosophical', '1949-01-01', 'The Second Sex'),
       ('A classic novel about the struggles of a young orphan.', 'Classic', '1861-12-01', 'Great Expectations'),
       ('A tale of magical realism and family history.', 'Magical Realism', '1967-05-30',
        'One Hundred Years of Solitude'),
       ('A dystopian novel exploring societal collapse.', 'Dystopian', '1985-01-01', 'The Handmaids Tale'),
       ('A tragedy about a young man struggling with love and ideals.', 'Classic', '1774-09-29',
        'The Sorrows of Young Werther'),
       ('An epic poem depicting the journey through Hell, Purgatory, and Heaven.', 'Epic', '1320-01-01',
        'The Divine Comedy'),
       ('A collaborative work by three renowned authors.', 'Science Fiction', '2024-01-01', 'The Triad Chronicles');


-- Insert into book_author
INSERT INTO book_author (id_book, id_author)
VALUES (1, 1),
       (2, 2),
       (3, 3),
       (4, 4),
       (5, 5),
       (6, 6),
       (7, 7),
       (8, 8),
       (9, 9),
       (10, 10),
       (11, 11),
       (12, 12),
       (13, 13),
       (13, 14),
       (13, 15);



