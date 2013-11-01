CREATE TABLE IF NOT EXISTS local_page (
  id BIGSERIAL PRIMARY KEY NOT NULL,
  lang_id SMALLINT NOT NULL,
  page_id INT NOT NULL,
  title VARCHAR(256) NOT NULL,
  name_space SMALLINT NOT NULL,
  is_redirect BOOLEAN NOT NULL,
  is_disambig BOOLEAN NOT NULL
);

