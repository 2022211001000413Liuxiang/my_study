# MySQL 数据库学习笔记

## 一、数据库基础概念

### 1.1 什么是数据库
- **数据库（Database）**：按照数据结构来组织、存储和管理数据的仓库
- **关系型数据库（RDBMS）**：使用表格结构存储数据，表与表之间可以建立关系
- **MySQL**：开源的关系型数据库管理系统，由 Oracle 公司维护

### 1.2 数据库三范式
| 范式        | 要求             | 示例           |
| --------- | -------------- | ------------ |
| 第一范式（1NF） | 字段不可再分，原子性     | 地址应拆分为省、市、区  |
| 第二范式（2NF） | 非主键字段完全依赖于主键   | 订单表中不应存储商品名称 |
| 第三范式（3NF） | 非主键字段不能传递依赖于主键 | 学生表中不应存储学院院长 |

---

## 二、SQL 语言基础

### 3.1 SQL 分类
| 类型 | 全称 | 用途 | 关键字 |
|------|------|------|--------|
| DDL | Data Definition Language | 定义数据库结构 | CREATE, ALTER, DROP, TRUNCATE |
| DML | Data Manipulation Language | 操作数据 | INSERT, UPDATE, DELETE |
| DQL | Data Query Language | 查询数据 | SELECT |
| DCL | Data Control Language | 权限控制 | GRANT, REVOKE |
| TCL | Transaction Control Language | 事务控制 | COMMIT, ROLLBACK, SAVEPOINT |

### 3.2 数据类型

#### 整数类型
| 类型 | 字节 | 范围（有符号） |
|------|------|----------------|
| TINYINT | 1 | -128 ~ 127 |
| SMALLINT | 2 | -32768 ~ 32767 |
| MEDIUMINT | 3 | -8388608 ~ 8388607 |
| INT | 4 | -2147483648 ~ 2147483647 |
| BIGINT | 8 | -2^63 ~ 2^63-1 |

#### 浮点类型
| 类型 | 说明 |
|------|------|
| FLOAT | 单精度浮点数，4字节 |
| DOUBLE | 双精度浮点数，8字节 |
| DECIMAL(M,D) | 精确小数，M总位数，D小数位数 |

#### 字符串类型
| 类型 | 最大长度 | 说明 |
|------|----------|------|
| CHAR(N) | 255字符 | 定长字符串 |
| VARCHAR(N) | 65535字节 | 变长字符串 |
| TEXT | 65535字节 | 长文本 |
| MEDIUMTEXT | 16M | 中等文本 |
| LONGTEXT | 4G | 超长文本 |
| ENUM | 65535个值 | 枚举类型 |

#### 日期时间类型
| 类型        | 格式                  | 范围                      |
| --------- | ------------------- | ----------------------- |
| DATE      | YYYY-MM-DD          | 1000-01-01 ~ 9999-12-31 |
| TIME      | HH:MM:SS            | -838:59:59 ~ 838:59:59  |
| DATETIME  | YYYY-MM-DD HH:MM:SS | 1000-01-01 ~ 9999-12-31 |
| TIMESTAMP | YYYY-MM-DD HH:MM:SS | 1970-01-01 ~ 2038-01-19 |
| YEAR      | YYYY                | 1901 ~ 2155             |

---

## 三、数据库与表操作（DDL）

### 4.1 数据库操作
```sql
-- 创建数据库
CREATE DATABASE mydb;
CREATE DATABASE IF NOT EXISTS mydb;
CREATE DATABASE mydb DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 查看所有数据库
SHOW DATABASES;

-- 使用数据库
USE mydb;

-- 查看当前数据库
SELECT DATABASE();

-- 删除数据库
DROP DATABASE mydb;
DROP DATABASE IF EXISTS mydb;

-- 修改数据库字符集
ALTER DATABASE mydb CHARACTER SET utf8mb4;
```

### 4.2 表操作
```sql
-- 创建表
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    age INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 查看所有表
SHOW TABLES;

-- 查看表结构
DESC users;
DESCRIBE users;
SHOW COLUMNS FROM users;

-- 查看建表语句
SHOW CREATE TABLE users;

-- 修改表名
ALTER TABLE users RENAME TO members;

-- 添加字段
ALTER TABLE users ADD phone VARCHAR(20);
ALTER TABLE users ADD address VARCHAR(200) AFTER email;

-- 修改字段类型
ALTER TABLE users MODIFY phone VARCHAR(30);

-- 修改字段名和类型
ALTER TABLE users CHANGE phone mobile VARCHAR(30);

-- 删除字段
ALTER TABLE users DROP mobile;

-- 删除表
DROP TABLE users;
DROP TABLE IF EXISTS users;

-- 清空表（保留结构）
TRUNCATE TABLE users;
```

### 4.3 约束
```sql
-- 主键约束
CREATE TABLE t1 (id INT PRIMARY KEY);

-- 自增
CREATE TABLE t2 (id INT AUTO_INCREMENT PRIMARY KEY);

-- 非空约束
CREATE TABLE t3 (name VARCHAR(50) NOT NULL);

-- 唯一约束
CREATE TABLE t4 (email VARCHAR(100) UNIQUE);

-- 默认约束
CREATE TABLE t5 (status INT DEFAULT 1);

-- 检查约束（MySQL 8.0+）
CREATE TABLE t6 (age INT CHECK (age >= 0 AND age <= 150));

-- 外键约束
CREATE TABLE orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- 添加约束
ALTER TABLE users ADD CONSTRAINT uk_email UNIQUE (email);

-- 删除约束
ALTER TABLE users DROP INDEX uk_email;
ALTER TABLE users DROP FOREIGN KEY fk_name;
```

---

## 四、数据操作（DML）

### 5.1 插入数据
```sql
-- 插入单条
INSERT INTO users (username, email, age) VALUES ('张三', 'zhangsan@example.com', 25);

-- 插入多条
INSERT INTO users (username, email, age) VALUES
    ('李四', 'lisi@example.com', 30),
    ('王五', 'wangwu@example.com', 28),
    ('赵六', 'zhaoliu@example.com', 35);

-- 插入查询结果
INSERT INTO backup_users (username, email)
SELECT username, email FROM users WHERE age > 25;
```

### 5.2 更新数据
```sql
-- 更新所有记录
UPDATE users SET age = 30;

-- 条件更新
UPDATE users SET age = 30 WHERE username = '张三';

-- 多字段更新
UPDATE users SET age = age + 1, email = 'new@example.com' WHERE id = 1;
```

### 5.3 删除数据
```sql
-- 删除所有记录
DELETE FROM users;

-- 条件删除
DELETE FROM users WHERE id = 1;

-- 删除并重置自增
TRUNCATE TABLE users;
```

---

## 五、数据查询（DQL）

### 6.1 基础查询
```sql
-- 查询所有字段
SELECT * FROM users;

-- 查询指定字段
SELECT username, email FROM users;

-- 别名
SELECT username AS '用户名', email AS '邮箱' FROM users;

-- 去重
SELECT DISTINCT age FROM users;
```

### 6.2 条件查询
```sql
-- 比较运算符
SELECT * FROM users WHERE age > 25;
SELECT * FROM users WHERE age >= 25 AND age <= 30;
SELECT * FROM users WHERE age != 25;

-- 逻辑运算符
SELECT * FROM users WHERE age > 25 AND gender = '男';
SELECT * FROM users WHERE age < 25 OR age > 35;
SELECT * FROM users WHERE NOT (age = 25);

-- BETWEEN
SELECT * FROM users WHERE age BETWEEN 25 AND 30;

-- IN
SELECT * FROM users WHERE age IN (25, 28, 30);

-- LIKE 模糊查询
SELECT * FROM users WHERE username LIKE '张%';     -- 以张开头
SELECT * FROM users WHERE username LIKE '%三%';    -- 包含三
SELECT * FROM users WHERE username LIKE '张_';     -- 张后面一个字符

-- NULL 判断
SELECT * FROM users WHERE email IS NULL;
SELECT * FROM users WHERE email IS NOT NULL;
```

### 6.3 聚合函数
```sql
-- COUNT 统计行数
SELECT COUNT(*) FROM users;
SELECT COUNT(email) FROM users;    -- 不统计NULL

-- SUM 求和
SELECT SUM(age) FROM users;

-- AVG 平均值
SELECT AVG(age) FROM users;

-- MAX / MIN
SELECT MAX(age) FROM users;
SELECT MIN(age) FROM users;
```

### 6.4 分组查询
```sql
-- 按年龄分组统计
SELECT age, COUNT(*) AS count
FROM users
GROUP BY age;

-- 分组后过滤（HAVING）
SELECT age, COUNT(*) AS count
FROM users
GROUP BY age
HAVING count > 2;

-- WHERE 与 HAVING 区别
-- WHERE：分组前过滤，不能用聚合函数
-- HAVING：分组后过滤，可以用聚合函数

-- 完整执行顺序
SELECT age, COUNT(*) AS count
FROM users
WHERE id > 1           -- 1. 先过滤
GROUP BY age           -- 2. 再分组
HAVING count > 1       -- 3. 分组后过滤
ORDER BY age DESC      -- 4. 排序
LIMIT 0, 10;           -- 5. 分页
```

### 6.5 排序查询
```sql
-- 单字段排序
SELECT * FROM users ORDER BY age ASC;      -- 升序
SELECT * FROM users ORDER BY age DESC;     -- 降序

-- 多字段排序
SELECT * FROM users ORDER BY age DESC, id ASC;
```

### 6.6 分页查询
```sql
-- LIMIT offset, count
SELECT * FROM users LIMIT 0, 10;     -- 第1页，每页10条
SELECT * FROM users LIMIT 10, 10;    -- 第2页
SELECT * FROM users LIMIT 20, 10;    -- 第3页

-- 公式：LIMIT (page-1) * size, size
```

---

## 六、多表查询

### 7.1 内连接（INNER JOIN）
```sql
-- 查询用户及其订单
SELECT u.username, o.order_no
FROM users u
INNER JOIN orders o ON u.id = o.user_id;

-- 三表连接
SELECT u.username, o.order_no, p.product_name
FROM users u
INNER JOIN orders o ON u.id = o.user_id
INNER JOIN order_items oi ON o.id = oi.order_id
INNER JOIN products p ON oi.product_id = p.id;
```

### 7.2 左外连接（LEFT JOIN）
```sql
-- 查询所有用户，包括没有订单的
SELECT u.username, o.order_no
FROM users u
LEFT JOIN orders o ON u.id = o.user_id;
```

### 7.3 右外连接（RIGHT JOIN）
```sql
-- 查询所有订单，包括没有匹配用户的
SELECT u.username, o.order_no
FROM users u
RIGHT JOIN orders o ON u.id = o.user_id;
```

### 7.4 自连接
```sql
-- 查询员工及其上级
SELECT e.name AS employee, m.name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;
```

### 7.5 子查询
```sql
-- 标量子查询
SELECT * FROM users WHERE age = (SELECT MAX(age) FROM users);

-- 列子查询
SELECT * FROM users WHERE id IN (SELECT user_id FROM orders);

-- 行子查询
SELECT * FROM users WHERE (age, email) = (
    SELECT age, email FROM users WHERE id = 1
);

-- EXISTS 子查询
SELECT * FROM users u
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);

-- FROM 子查询（临时表）
SELECT age_group, COUNT(*) FROM (
    SELECT CASE
        WHEN age < 20 THEN '青年'
        WHEN age < 40 THEN '中年'
        ELSE '老年'
    END AS age_group
    FROM users
) t
GROUP BY age_group;
```

---

## 七、索引

### 8.1 索引类型
| 类型 | 说明 | 适用场景 |
|------|------|----------|
| 主键索引 | PRIMARY KEY | 唯一标识记录 |
| 唯一索引 | UNIQUE | 字段值不能重复 |
| 普通索引 | INDEX | 加速查询 |
| 全文索引 | FULLTEXT | 文本搜索 |
| 组合索引 | INDEX(col1, col2) | 多字段查询 |

### 8.2 索引操作
```sql
-- 创建索引
CREATE INDEX idx_username ON users(username);
CREATE UNIQUE INDEX idx_email ON users(email);
CREATE INDEX idx_name_age ON users(username, age);

-- 查看索引
SHOW INDEX FROM users;

-- 删除索引
DROP INDEX idx_username ON users;

-- ALTER 方式添加
ALTER TABLE users ADD INDEX idx_age (age);
ALTER TABLE users ADD UNIQUE INDEX idx_email (email);
```

### 8.3 索引失效场景
```sql
-- 1. 对索引列使用函数
SELECT * FROM users WHERE LEFT(username, 1) = '张';  -- 失效
SELECT * FROM users WHERE username LIKE '张%';        -- 生效

-- 2. 对索引列进行运算
SELECT * FROM users WHERE age + 1 = 25;  -- 失效
SELECT * FROM users WHERE age = 24;       -- 生效

-- 3. 隐式类型转换
SELECT * FROM users WHERE username = 123;  -- 失效（varchar与int比较）

-- 4. LIKE 以%开头
SELECT * FROM users WHERE username LIKE '%三';  -- 失效

-- 5. OR 条件中有非索引列
SELECT * FROM users WHERE username = '张三' OR age = 25;  -- 可能失效

-- 6. 不满足最左前缀原则（组合索引）
-- 组合索引 (a, b, c)
WHERE a = 1;              -- 生效
WHERE a = 1 AND b = 2;    -- 生效
WHERE b = 2;              -- 失效
WHERE a = 1 AND c = 3;    -- 只用到a
```

### 8.4 EXPLAIN 执行计划
```sql
EXPLAIN SELECT * FROM users WHERE username = '张三';
```

| 字段 | 说明 |
|------|------|
| id | 查询序号 |
| select_type | 查询类型（SIMPLE/PRIMARY/SUBQUERY） |
| table | 访问的表 |
| type | 访问类型（system > const > eq_ref > ref > range > index > ALL） |
| possible_keys | 可能使用的索引 |
| key | 实际使用的索引 |
| key_len | 索引长度 |
| rows | 预估扫描行数 |
| Extra | 额外信息（Using index/Using filesort/Using temporary） |

---

## 八、事务

### 9.1 事务特性（ACID）
| 特性 | 说明 |
|------|------|
| 原子性（Atomicity） | 事务是不可分割的工作单位 |
| 一致性（Consistency） | 事务前后数据保持一致 |
| 隔离性（Isolation） | 并发事务之间互不干扰 |
| 持久性（Durability） | 事务提交后数据永久保存 |

### 9.2 事务操作
```sql
-- 开启事务
START TRANSACTION;
-- 或
BEGIN;

-- 执行操作
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;

-- 提交事务
COMMIT;

-- 回滚事务
ROLLBACK;

-- 保存点
SAVEPOINT sp1;
ROLLBACK TO sp1;
```

### 9.3 隔离级别
| 隔离级别 | 脏读 | 不可重复读 | 幻读 |
|----------|------|------------|------|
| READ UNCOMMITTED | 有 | 有 | 有 |
| READ COMMITTED | 无 | 有 | 有 |
| REPEATABLE READ（默认） | 无 | 无 | 有 |
| SERIALIZABLE | 无 | 无 | 无 |

```sql
-- 查看隔离级别
SELECT @@transaction_isolation;

-- 设置隔离级别
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

---

## 九、存储过程与函数

### 10.1 存储过程
```sql
-- 创建存储过程
DELIMITER //
CREATE PROCEDURE get_users_by_age(IN min_age INT)
BEGIN
    SELECT * FROM users WHERE age >= min_age;
END //
DELIMITER ;

-- 调用
CALL get_users_by_age(25);

-- 带输出参数
DELIMITER //
CREATE PROCEDURE get_user_count(OUT total INT)
BEGIN
    SELECT COUNT(*) INTO total FROM users;
END //
DELIMITER ;

CALL get_user_count(@total);
SELECT @total;

-- 删除存储过程
DROP PROCEDURE IF EXISTS get_users_by_age;
```

### 10.2 函数
```sql
-- 创建函数
DELIMITER //
CREATE FUNCTION get_age_level(age INT)
RETURNS VARCHAR(10)
DETERMINISTIC
BEGIN
    DECLARE level VARCHAR(10);
    IF age < 18 THEN
        SET level = '未成年';
    ELSEIF age < 60 THEN
        SET level = '成年';
    ELSE
        SET level = '老年';
    END IF;
    RETURN level;
END //
DELIMITER ;

-- 使用
SELECT username, get_age_level(age) AS age_level FROM users;
```

### 10.3 流程控制
```sql
-- IF
IF condition THEN
    -- statements
ELSEIF condition THEN
    -- statements
ELSE
    -- statements
END IF;

-- CASE
CASE score
    WHEN 90 THEN SET grade = 'A';
    WHEN 80 THEN SET grade = 'B';
    ELSE SET grade = 'C';
END CASE;

-- WHILE
WHILE i <= 10 DO
    SET i = i + 1;
END WHILE;

-- REPEAT
REPEAT
    SET i = i + 1;
UNTIL i > 10
END REPEAT;

-- LOOP
my_loop: LOOP
    SET i = i + 1;
    IF i > 10 THEN
        LEAVE my_loop;
    END IF;
END LOOP;
```

---

## 十、触发器

```sql
-- 创建触发器
DELIMITER //
CREATE TRIGGER before_insert_users
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    SET NEW.created_at = NOW();
END //
DELIMITER ;

-- AFTER INSERT 触发器
DELIMITER //
CREATE TRIGGER after_insert_users
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO user_logs (user_id, action, created_at)
    VALUES (NEW.id, 'INSERT', NOW());
END //
DELIMITER ;

-- BEFORE UPDATE
DELIMITER //
CREATE TRIGGER before_update_users
BEFORE UPDATE ON users
FOR EACH ROW
BEGIN
    SET NEW.updated_at = NOW();
END //
DELIMITER ;

-- 删除触发器
DROP TRIGGER IF EXISTS before_insert_users;

-- 查看触发器
SHOW TRIGGERS;
```

---

## 十一、视图

```sql
-- 创建视图
CREATE VIEW v_active_users AS
SELECT id, username, email, age
FROM users
WHERE status = 1;

-- 使用视图
SELECT * FROM v_active_users WHERE age > 25;

-- 修改视图
ALTER VIEW v_active_users AS
SELECT id, username, email
FROM users
WHERE status = 1;

-- 删除视图
DROP VIEW IF EXISTS v_active_users;

-- 查看视图定义
SHOW CREATE VIEW v_active_users;
```

---

## 十二、锁机制

### 13.1 锁分类
| 分类方式 | 类型                  |
| ---- | ------------------- |
| 粒度   | 表锁、行锁、页锁            |
| 类型   | 共享锁（S锁）、排他锁（X锁）     |
| 意向   | 意向共享锁（IS）、意向排他锁（IX） |

### 13.2 锁操作
```sql
-- 表锁
LOCK TABLES users READ;     -- 加读锁
LOCK TABLES users WRITE;    -- 加写锁
UNLOCK TABLES;              -- 释放锁

-- 行锁
SELECT * FROM users WHERE id = 1 FOR UPDATE;           -- 排他锁
SELECT * FROM users WHERE id = 1 LOCK IN SHARE MODE;   -- 共享锁

-- 死锁处理
SHOW ENGINE INNODB STATUS;   -- 查看死锁信息
```

---

## 十三、性能优化

### 14.1 慢查询
```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 2;  -- 超过2秒记录

-- 查看慢查询状态
SHOW VARIABLES LIKE 'slow_query%';
```

### 14.2 优化建议
1. **避免 SELECT ***：只查询需要的字段
2. **合理使用索引**：为经常查询的字段建索引
3. **避免在 WHERE 中对字段进行函数操作**
4. **使用 LIMIT 限制返回数据量**
5. **避免使用 != 或 <>**
6. **合理使用 JOIN 替代子查询**
7. **大批量插入时使用 LOAD DATA**

### 14.3 查询优化
```sql
-- 使用 LIMIT 1 当只需要一条记录
SELECT * FROM users WHERE username = '张三' LIMIT 1;

-- 使用 EXISTS 替代 IN（大数据量时）
SELECT * FROM users u
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);

-- 避免 SELECT *
SELECT id, username, email FROM users;
```

---

## 十四、备份与恢复

### 15.1 命令行备份
```bash
# 备份单个数据库
mysqldump -u root -p mydb > mydb_backup.sql

# 备份多个数据库
mysqldump -u root -p --databases db1 db2 > backup.sql

# 备份所有数据库
mysqldump -u root -p --all-databases > all_backup.sql

# 备份指定表
mysqldump -u root -p mydb users orders > tables_backup.sql
```

### 15.2 恢复
```bash
# 恢复数据库
mysql -u root -p mydb < mydb_backup.sql

# 在 MySQL 命令行中恢复
source /path/to/backup.sql;
```

---

## 十五、用户与权限管理

### 16.1 用户操作
```sql
-- 创建用户
CREATE USER 'newuser'@'localhost' IDENTIFIED BY 'password';
CREATE USER 'newuser'@'%' IDENTIFIED BY 'password';  -- 允许远程

-- 修改密码
ALTER USER 'newuser'@'localhost' IDENTIFIED BY 'newpassword';

-- 删除用户
DROP USER 'newuser'@'localhost';

-- 查看用户
SELECT user, host FROM mysql.user;
```

### 16.2 权限管理
```sql
-- 授权
GRANT SELECT, INSERT, UPDATE ON mydb.* TO 'newuser'@'localhost';
GRANT ALL PRIVILEGES ON mydb.* TO 'newuser'@'localhost';

-- 查看权限
SHOW GRANTS FOR 'newuser'@'localhost';

-- 撤销权限
REVOKE INSERT ON mydb.* FROM 'newuser'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;
```

---

## 十六、常用函数

### 17.1 字符串函数
```sql
SELECT CONCAT('Hello', ' ', 'World');           -- 拼接
SELECT LENGTH('Hello');                          -- 字节长度
SELECT CHAR_LENGTH('Hello');                     -- 字符长度
SELECT UPPER('hello');                           -- 转大写
SELECT LOWER('HELLO');                           -- 转小写
SELECT TRIM('  Hello  ');                        -- 去空格
SELECT SUBSTRING('Hello World', 1, 5);           -- 截取
SELECT REPLACE('Hello World', 'World', 'MySQL'); -- 替换
SELECT REVERSE('Hello');                         -- 反转
```

### 17.2 数学函数
```sql
SELECT ABS(-10);           -- 绝对值
SELECT CEIL(1.2);          -- 向上取整 → 2
SELECT FLOOR(1.8);         -- 向下取整 → 1
SELECT ROUND(1.55, 1);     -- 四舍五入 → 1.6
SELECT MOD(10, 3);         -- 取余 → 1
SELECT RAND();             -- 随机数 0~1
SELECT POWER(2, 10);       -- 幂运算 → 1024
```

### 17.3 日期函数
```sql
SELECT NOW();                              -- 当前日期时间
SELECT CURDATE();                          -- 当前日期
SELECT CURTIME();                          -- 当前时间
SELECT YEAR(NOW());                        -- 年份
SELECT MONTH(NOW());                       -- 月份
SELECT DAY(NOW());                         -- 日
SELECT DATE_FORMAT(NOW(), '%Y-%m-%d');     -- 格式化
SELECT DATEDIFF('2025-12-31', '2025-01-01'); -- 日期差
SELECT DATE_ADD(NOW(), INTERVAL 7 DAY);   -- 日期加减
SELECT DATE_SUB(NOW(), INTERVAL 1 MONTH);
SELECT UNIX_TIMESTAMP();                   -- 时间戳
SELECT FROM_UNIXTIME(1700000000);          -- 时间戳转日期
```

### 17.4 条件函数
```sql
-- IF
SELECT IF(age >= 18, '成年', '未成年') FROM users;

-- IFNULL
SELECT IFNULL(email, '未填写') FROM users;

-- CASE WHEN
SELECT username,
    CASE
        WHEN age < 18 THEN '未成年'
        WHEN age < 60 THEN '成年'
        ELSE '老年'
    END AS age_group
FROM users;
```

### 17.5 窗口函数（MySQL 8.0+）
```sql
-- ROW_NUMBER：行号
SELECT username, age,
    ROW_NUMBER() OVER (ORDER BY age DESC) AS rank_num
FROM users;

-- RANK：排名（有并列会跳号）
SELECT username, age,
    RANK() OVER (ORDER BY age DESC) AS rank_num
FROM users;

-- DENSE_RANK：排名（有并列不跳号）
SELECT username, age,
    DENSE_RANK() OVER (ORDER BY age DESC) AS rank_num
FROM users;

-- 分组排名
SELECT username, age, department,
    ROW_NUMBER() OVER (PARTITION BY department ORDER BY age DESC) AS dept_rank
FROM users;

-- 累计求和
SELECT order_date, amount,
    SUM(amount) OVER (ORDER BY order_date) AS running_total
FROM orders;

-- 移动平均
SELECT order_date, amount,
    AVG(amount) OVER (ORDER BY order_date ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS moving_avg
FROM orders;
```

---

## 十七、常见面试题

### Q1: MySQL 中 in 和 exists 的区别？
- `IN`：先执行子查询，将结果集缓存，再与外层查询匹配
- `EXISTS`：对外层查询逐行执行子查询，子查询返回 true/false
- **选择**：子查询表小用 IN，外表小用 EXISTS

### Q2: CHAR 和 VARCHAR 的区别？
- `CHAR`：定长，存储时右侧补空格，查询时去掉，效率高
- `VARCHAR`：变长，需要额外1-2字节存储长度，节省空间

### Q3: MySQL 为什么用 B+ 树而不是 B 树？
- B+ 树叶子节点形成链表，范围查询更高效
- B+ 树非叶子节点只存索引，能存更多数据，树更矮
- B+ 树查询效率更稳定（都到叶子节点）

### Q4: 聚簇索引和非聚簇索引？
- **聚簇索引**：数据和索引在一起，叶子节点存完整数据（InnoDB 主键索引）
- **非聚簇索引**：叶子节点存主键值，需要回表查询（二级索引）

### Q5: 事务隔离级别如何选择？
- **RC（READ COMMITTED）**：互联网业务常用，性能好
- **RR（REPEATABLE READ）**：MySQL 默认，一致性好
- 一般选 RC，对一致性要求高选 RR

---

## 十八、实践练习

### 19.1 建库建表练习
```sql
-- 创建电商数据库
CREATE DATABASE IF NOT EXISTS shop DEFAULT CHARSET utf8mb4;
USE shop;

-- 用户表
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    status TINYINT DEFAULT 1 COMMENT '0-禁用 1-正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 商品表
CREATE TABLE products (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    category_id INT,
    price DECIMAL(10,2) NOT NULL,
    stock INT DEFAULT 0,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- 订单表
CREATE TABLE orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status TINYINT DEFAULT 0 COMMENT '0-待付款 1-已付款 2-已发货 3-已完成',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 订单明细表
CREATE TABLE order_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

### 19.2 查询练习
```sql
-- 1. 查询每个用户的订单数量
SELECT u.username, COUNT(o.id) AS order_count
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id;

-- 2. 查询订单金额前10的用户
SELECT u.username, SUM(o.total_amount) AS total
FROM users u
INNER JOIN orders o ON u.id = o.user_id
GROUP BY u.id
ORDER BY total DESC
LIMIT 10;

-- 3. 查询没有订单的用户
SELECT * FROM users
WHERE id NOT IN (SELECT DISTINCT user_id FROM orders);

-- 4. 查询每个商品的销售数量
SELECT p.name, SUM(oi.quantity) AS sold_count
FROM products p
INNER JOIN order_items oi ON p.id = oi.product_id
GROUP BY p.id
ORDER BY sold_count DESC;

-- 5. 查询本月订单统计
SELECT
    COUNT(*) AS total_orders,
    SUM(total_amount) AS total_amount,
    AVG(total_amount) AS avg_amount
FROM orders
WHERE YEAR(created_at) = YEAR(NOW())
AND MONTH(created_at) = MONTH(NOW());
```

---

## 十九、学习资源

- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [MySQL Tutorial](https://www.mysqltutorial.org/)
- [菜鸟教程 MySQL](https://www.runoob.com/mysql/mysql-tutorial.html)

---

> 学习建议：先掌握基础的 CRUD 操作，再深入索引优化和事务，最后学习高级特性。多动手练习，通过实际项目巩固知识。
