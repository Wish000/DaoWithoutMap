# DaoWithoutMap
A Dao framework based on Spring Boot and Mybatis but without mappers.

Mybatis framework provides a way to operate DB, in which we must create mappers, a interface including abstract methods
with an annotation containing a SQL. The parameters in the SQL are provided by the method.

  In this way, one query needs one SQL, which means one query needs one method. Thus, many queries from a table may need
many methods. However, expect some parameters, these methods are almost duplicated. For example, we sometimes query a
table by name, while sometimes by age. To solve this problem, Mybatis provides a `Provider` class which can handle ambiguous
parameters contained in the pojo/entity that corresponding to the table queried. But some entities/pojos may have many
fields as the tables, it courses the Provider class very long and hard to maintain.

In this framework, mappers or providers are no longer needed in many cases. Instead, a empty interface extending Dao
shall provide basic and useful SELECT, INSERT, UPDATE, DELETE methods and custom methods just defined by annotation,
which can prevent classes including many redundant methods.

The framework code is writen in `compiler` directory, and the example and the use details(not in the md) is in
`controller`, `service`, `dao`, `entity` as we usually do in web projects.