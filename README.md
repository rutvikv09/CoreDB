# CoreDB 📂

Welcome to the CoreDB project! 🎉 CoreDB is a simple yet powerful database management system built from scratch in Java. This project demonstrates our understanding of essential database components, including query optimization, transaction processing, and data management.


## Objectives 🎯

By completing this project, the team gained experience in:
- Describing common designs for core database system components.
- Implementing query optimizers, query executors, storage managers, access methods, and transaction processors.
- Explaining techniques used for data replication and allocation during database design.

## Features 🚀

### Module 1: DB Design 🏗️
- Implemented linear data structures for query and data processing.
- Developed a custom file format for data and metadata storage.
- Explained the logic behind data structure and file format selection.

### Module 2: Query Implementation 🔍
- Supported standard SQL commands:
  - `CREATE DATABASE`
  - `USE DATABASE`
  - `CREATE TABLE`
  - `INSERT INTO TABLE`
  - `SELECT FROM TABLE` with simple `WHERE` conditions
  - `UPDATE` a column with a single `WHERE` condition
  - `DELETE` a row with a single `WHERE` condition
  - `DROP TABLE`

### Module 3: Transaction Processing 🔄
- Differentiated between transactions and normal queries.
- Implemented ACID properties for transactions.
- Used linear data structures for transaction operations.

### Module 4: Log Management 📋
- Created logs for:
  - General information (query execution time, database state)
  - Events (changes, concurrent transactions, crash reports)
  - Queries (user queries, timestamps)
- Implemented log management without external libraries.

### Module 5: Data Modelling – Reverse Engineering 🛠️
- Generated ERD (Entity-Relationship Diagram) based on the current database state.
- Provided a text file with tables, columns, relationships, and cardinality.

### Module 6: Export Structure & Value 💾
- Exported database structure and values in SQL format.
- Ensured the data dump reflects the current state of the database.

### Module 7: User Interface & Login Security 🔐
- Developed a basic console-based user interface.
- Implemented user registration and login with hashed credentials.
- Provided options for query execution, data export, ERD generation, and system exit.

## Getting Started 🚀

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/yourusername/coredb.git
2. **Navigate to the Project Directory:**
    ```bash
       cd coredb
3. **Compile and Run: ** Use your preferred Java IDE or command line to compile and run the project.
## 🙋‍♂️ Contact
- For any questions or feedback, please reach out to vaghanirutvik777@example.com.
