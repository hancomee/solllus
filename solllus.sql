
CREATE TABLE users (
  id int(11) NOT NULL AUTO_INCREMENT,
  name varchar(25) NOT NULL,
  pass varchar(25) NOT NULL,
  roles varchar(25) NOT NULL DEFAULT '',
  removal tinyint NOT NULL DEFAULT 0,
  datetime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE display (
  id int(11) NOT NULL AUTO_INCREMENT,
  name varchar(25) NOT NULL,
  idx varchar(25) NOT NULL,
  json text NOT NULL DEFAULT '',
  PRIMARY KEY (id)
);

CREATE TABLE apps (
  id int(11) NOT NULL AUTO_INCREMENT,
  name varchar(25) NOT NULL,
  idx varchar(25) NOT NULL,
  path varchar(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE display_key (
	value varchar(25) NOT NULL,
    datetime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name varchar(25) NOT NULL,
    nickname varchar(255) NOT NULL DEFAULT '',
	idx varchar(25) NOT NULL,
    PRIMARY KEY (value)
);


SELECT * FROM display_key;

