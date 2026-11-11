INSERT INTO book (isbn, title, author, author_id, published_year, price_amount, price_currency, version)
VALUES
  ('9780134685991', 'Effective Java', 'Joshua Bloch', 42, 2018, 285000.00, 'VND', 0),
  ('9781617294549', 'Microservices Patterns', 'Chris Richardson', NULL, 2018, 520000.00, 'VND', 0),
  ('9780132350884', 'Clean Code', 'Robert C. Martin', 10, 2008, 350000.00, 'VND', 0),
  ('9780134494166', 'Clean Architecture', 'Robert C. Martin', 10, 2017, 380000.00, 'VND', 0),
  ('9780321127426', 'Enterprise Integration Patterns', 'Gregor Hohpe', NULL, 2003, 680000.00, 'VND', 0),
  ('9781491950357', 'Building Microservices', 'Sam Newman', NULL, 2015, 450000.00, 'VND', 0),
  ('9781492031772', 'Designing Data-Intensive Applications', 'Martin Kleppmann', NULL, 2017, 580000.00, 'VND', 0),
  ('9781617298530', 'Microservices in Action', 'Morgan Bruce', NULL, 2019, 490000.00, 'VND', 0)
ON CONFLICT (isbn) DO NOTHING;
