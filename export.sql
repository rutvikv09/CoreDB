CREATE TABLE PEOPLE (
  id INT(PK),
  name STRING,
  age INT,
  ID REFERENCES SCHOOL(ID)
);
-- No data to insert for table PEOPLE

CREATE TABLE SCHOOL (
  id INT(PK),
  name STRING,
  age INT,
  NAME REFERENCES USERS(USERID)
);
-- No data to insert for table SCHOOL

CREATE TABLE USERS (
  userid INT(PK),
  username VARCHAR(50),
  email VARCHAR(100),
  dateofbirth DATE
);
-- No data to insert for table USERS

