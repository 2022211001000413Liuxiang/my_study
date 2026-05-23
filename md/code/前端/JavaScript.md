# JavaScript 基础复习

## 目录
- [1. 变量与数据类型](#1-变量与数据类型)
- [2. 运算符](#2-运算符)
- [3. 流程控制](#3-流程控制)
- [4. 函数](#4-函数)
- [5. 数组](#5-数组)
- [6. 对象](#6-对象)
- [7. 字符串](#7-字符串)
- [8. DOM 操作](#8-dom-操作)
- [9. 事件处理](#9-事件处理)
- [10. 异步编程](#10-异步编程)
- [11. ES6+ 新特性](#11-es6-新特性)
- [12. 面向对象](#12-面向对象)
- [13. 错误处理](#13-错误处理)
- [14. 本地存储](#14-本地存储)
- [15. 正则表达式](#15-正则表达式)

---

## 1. 变量与数据类型

### 声明方式
| 关键词 | 特点 | 示例 |
|--------|------|------|
| `var` | 函数作用域，可重复声明，有变量提升 | `var x = 10` |
| `let` | 块级作用域，不可重复声明，无变量提升 | `let x = 10` |
| `const` | 块级作用域，必须初始化，不可重新赋值 | `const x = 10` |

### 数据类型
| 类型          | 说明         | 示例                   |
| ----------- | ---------- | -------------------- |
| `number`    | 数字（整数和浮点数） | `42`, `3.14`         |
| `string`    | 字符串        | `"hello"`, `'world'` |
| `boolean`   | 布尔值        | `true`, `false`      |
| `undefined` | 未定义        | `let x;`             |
| `null`      | 空值         | `let x = null;`      |
| `object`    | 对象         | `{name: "张三"}`       |
| `array`     | 数组（特殊对象）   | `[1, 2, 3]`          |
| `function`  | 函数（特殊对象）   | `function() {}`      |

### 类型检测
```javascript
typeof "hello"     // "string"
typeof 42          // "number"
typeof true        // "boolean"
typeof undefined   // "undefined"
typeof null        // "object" (历史遗留bug)
typeof {}          // "object"
typeof []          // "object"
Array.isArray([])  // true
```

### 类型转换
```javascript
// 显式转换
Number("123")      // 123
String(123)        // "123"
Boolean(0)         // false
Boolean("")        // false
Boolean("hello")   // true

// 隐式转换
"5" + 3            // "53" (字符串拼接)
"5" - 3            // 2 (数学运算)
```

---

## 2. 运算符

### 算术运算符
```javascript
+   // 加
-   // 减
*   // 乘
/   // 除
%   // 取余
++  // 自增
--  // 自减
```

### 比较运算符
| 运算符 | 说明 | 示例 |
|--------|------|------|
| `==` | 宽松相等（会转换类型） | `"5" == 5` → `true` |
| `===` | 严格相等（不转换类型） | `"5" === 5` → `false` |
| `!=` | 宽松不等 | |
| `!==` | 严格不等 | |

### 逻辑运算符
```javascript
&&   // 与（两个都为真）
||   // 或（至少一个为真）
!    // 非（取反）

// 短路求值
value && doSomething()   // value 为真时执行
value || defaultValue     // value 为假时返回默认值
```

### 赋值运算符
```javascript
=    // 赋值
+=   // 加赋值
-=   // 减赋值
*=   // 乘赋值
/=   // 除赋值
%=   // 取余赋值
```

### 三元运算符
```javascript
条件 ? 真值 : 假值
// 示例
let age = 18;
let result = age >= 18 ? "成年" : "未成年";
```

---

## 3. 流程控制

### 条件语句
```javascript
// if...else
if (条件) {
  // ...
} else if (条件2) {
  // ...
} else {
  // ...
}

// switch
switch (表达式) {
  case 值1:
    // ...
    break;
  case 值2:
    // ...
    break;
  default:
    // ...
}
```

### 循环语句
```javascript
// for 循环
for (let i = 0; i < 10; i++) {
  // ...
}

// while 循环
while (条件) {
  // ...
}

// do...while 循环
do {
  // ...
} while (条件);

// for...of (遍历值)
for (let item of array) {
  console.log(item);
}

// for...in (遍历键)
for (let key in object) {
  console.log(key, object[key]);
}
```

### break 和 continue
```javascript
break;      // 跳出整个循环
continue;   // 跳过本次循环，继续下一次
```

---

## 4. 函数

### 声明方式
```javascript
// 函数声明（有提升）
function add(a, b) {
  return a + b;
}

// 函数表达式（无提升）
const add = function(a, b) {
  return a + b;
};

// 箭头函数（简洁写法）
const add = (a, b) => a + b;

// 箭头函数（多行）
const add = (a, b) => {
  const sum = a + b;
  return sum;
};
```

### 参数
```javascript
// 默认参数
function greet(name = "世界") {
  console.log(`你好，${name}`);
}

// 剩余参数
function sum(...numbers) {
  return numbers.reduce((a, b) => a + b, 0);
}
sum(1, 2, 3);  // 6

// 解构参数
function printUser({name, age}) {
  console.log(name, age);
}
printUser({name: "张三", age: 18});
```

### 作用域
| 类型    | 说明                       |
| ----- | ------------------------ |
| 全局作用域 | 在函数外声明，任何地方可访问           |
| 函数作用域 | 函数内声明，仅函数内可访问            |
| 块级作用域 | `{}` 内用 `let`/`const` 声明 |

### 闭包
```javascript
function outer() {
  let count = 0;
  return function inner() {
    count++;
    return count;
  };
}

const counter = outer();
counter();  // 1
counter();  // 2
counter();  // 3
```

---

## 5. 数组

### 创建数组
```javascript
const arr1 = [1, 2, 3];
const arr2 = new Array(3);  // 创建长度为3的空数组
const arr3 = Array.from("hello");  // ['h', 'e', 'l', 'l', 'o']
```

### 常用方法
| 方法 | 说明 | 返回值 |
|------|------|--------|
| `push()` | 末尾添加元素 | 新长度 |
| `pop()` | 末尾删除元素 | 被删元素 |
| `unshift()` | 开头添加元素 | 新长度 |
| `shift()` | 开头删除元素 | 被删元素 |
| `splice()` | 删除/插入元素 | 被删元素数组 |
| `slice()` | 截取数组 | 新数组 |
| `concat()` | 合并数组 | 新数组 |
| `indexOf()` | 查找元素索引 | 索引/-1 |
| `includes()` | 是否包含元素 | boolean |
| `reverse()` | 反转数组 | 原数组 |
| `sort()` | 排序 | 原数组 |
| `join()` | 转字符串 | 字符串 |
| `flat()` | 扁平化数组 | 新数组 |

### 迭代方法
```javascript
const arr = [1, 2, 3, 4, 5];

// forEach - 遍历（无返回值）
arr.forEach(item => console.log(item));

// map - 映射（返回新数组）
const doubled = arr.map(item => item * 2);  // [2, 4, 6, 8, 10]

// filter - 过滤（返回新数组）
const even = arr.filter(item => item % 2 === 0);  // [2, 4]

// reduce - 归纳（返回单个值）
const sum = arr.reduce((acc, item) => acc + item, 0);  // 15

// find - 查找（返回第一个匹配元素）
const found = arr.find(item => item > 3);  // 4

// some - 至少一个满足（返回boolean）
const hasEven = arr.some(item => item % 2 === 0);  // true

// every - 全部满足（返回boolean）
const allPositive = arr.every(item => item > 0);  // true
```

### 解构赋值
```javascript
const [a, b, ...rest] = [1, 2, 3, 4, 5];
// a = 1, b = 2, rest = [3, 4, 5]
```

---

## 6. 对象

### 创建对象
```javascript
// 字面量
const person = {
  name: "张三",
  age: 18
};

// 构造函数
const person = new Object();
person.name = "张三";

// 工厂函数
function createPerson(name, age) {
  return { name, age };
}
```

### 属性操作
```javascript
// 访问属性
person.name        // 点语法
person["name"]     // 方括号语法（动态属性名）

// 添加/修改属性
person.email = "test@example.com";
person["age"] = 20;

// 删除属性
delete person.email;

// 检查属性
"name" in person;           // true
person.hasOwnProperty("name");  // true
```

### 对象方法
```javascript
const calculator = {
  value: 0,

  // 方法简写
  add(n) {
    this.value += n;
    return this;  // 链式调用
  },

  subtract(n) {
    this.value -= n;
    return this;
  },

  getResult() {
    return this.value;
  }
};

calculator.add(5).subtract(2).getResult();  // 3
```

### 对象操作方法
```javascript
Object.keys(obj)      // 返回键数组
Object.values(obj)    // 返回值数组
Object.entries(obj)   // 返回键值对数组
Object.assign(target, source)  // 合并对象
Object.freeze(obj)    // 冻结对象
Object.fromEntries(entries)  // 从键值对创建对象
```

### 解构赋值
```javascript
const { name, age, email = "默认" } = person;

// 重命名
const { name: userName } = person;
```

### 展开运算符
```javascript
const obj1 = { a: 1, b: 2 };
const obj2 = { b: 3, c: 4 };

const merged = { ...obj1, ...obj2 };
// { a: 1, b: 3, c: 4 } (后面的覆盖前面的)
```

---

## 7. 字符串

### 常用属性和方法
| 方法 | 说明 |
|------|------|
| `.length` | 字符串长度 |
| `.charAt()` | 获取指定位置字符 |
| `.indexOf()` | 查找子串位置 |
| `.includes()` | 是否包含子串 |
| `.slice()` | 截取子串 |
| `.replace()` | 替换子串 |
| `.split()` | 分割为数组 |
| `.trim()` | 去除首尾空格 |
| `.toUpperCase()` | 转大写 |
| `.toLowerCase()` | 转小写 |
| `.startsWith()` | 是否以...开头 |
| `.endsWith()` | 是否以...结尾 |
| `.repeat()` | 重复字符串 |
| `.padStart()` | 前面填充 |
| `.padEnd()` | 后面填充 |

### 模板字符串
```javascript
const name = "张三";
const age = 18;

// 反引号 + ${}
const greeting = `你好，我是${name}，今年${age}岁`;

// 多行字符串
const html = `
  <div>
    <p>${name}</p>
  </div>
`;
```

---

## 8. DOM 操作

### 获取元素
```javascript
document.getElementById("id")           // 通过ID
document.getElementsByClassName("class") // 通过类名
document.getElementsByTagName("tag")     // 通过标签名
document.querySelector(".class")         // CSS选择器（第一个）
document.querySelectorAll(".class")      // CSS选择器（所有）
```

### 创建和插入元素
```javascript
// 创建
const div = document.createElement("div");

// 插入
parent.appendChild(child);           // 末尾插入
parent.insertBefore(new, ref);      // 参考元素前插入
element.insertAdjacentHTML(position, html);  // 插入HTML

// 位置参数
"beforebegin"  // 元素前
"afterbegin"   // 元素内开头
"beforeend"    // 元素内末尾
"afterend"     // 元素后
```

### 修改内容
```javascript
element.textContent    // 文本内容
element.innerHTML      // HTML内容
element.innerText      // 文本内容（考虑样式）
element.outerHTML      // 包含元素的HTML
```

### 修改样式
```javascript
// 内联样式
element.style.color = "red";
element.style.backgroundColor = "blue";

// CSS类
element.classList.add("active");
element.classList.remove("active");
element.classList.toggle("active");
element.classList.contains("active");
```

### 修改属性
```javascript
element.setAttribute("id", "newId");
element.getAttribute("id");
element.removeAttribute("id");

// dataset (data-* 属性)
element.dataset.userId = "123";
```

---

## 9. 事件处理

### 事件绑定
```javascript
// HTML 属性
<button onclick="handleClick()">点击</button>

// DOM 属性
button.onclick = function() { /* ... */ };

// addEventListener（推荐）
button.addEventListener("click", function() { /* ... */ });

// 移除事件监听
button.removeEventListener("click", handler);
```

### 事件对象
```javascript
button.addEventListener("click", function(event) {
  event.target        // 触发事件的元素
  event.currentTarget // 绑定事件的元素
  event.type          // 事件类型
  event.preventDefault()   // 阻止默认行为
  event.stopPropagation()  // 阻止冒泡
});
```

### 常用事件
| 事件 | 说明 |
|------|------|
| `click` | 点击 |
| `dblclick` | 双击 |
| `mouseenter` | 鼠标进入 |
| `mouseleave` | 鼠标离开 |
| `mousemove` | 鼠标移动 |
| `keydown` | 按键按下 |
| `keyup` | 按键抬起 |
| `submit` | 表单提交 |
| `change` | 表单元素值改变 |
| `input` | 输入框内容变化 |
| `focus` | 获得焦点 |
| `blur` | 失去焦点 |
| `load` | 页面加载完成 |

### 事件委托
```javascript
// 利用事件冒泡，在父元素上监听
document.querySelector("ul").addEventListener("click", function(e) {
  if (e.target.tagName === "LI") {
    console.log(e.target.textContent);
  }
});
```

### 事件冒泡与捕获
```
捕获阶段: document → window → 目标元素的父元素 → 目标元素
目标阶段: 目标元素
冒泡阶段: 目标元素 → 父元素 → ... → document
```
```javascript
// 捕获阶段触发（第三个参数为true）
element.addEventListener("click", handler, true);

// 冒泡阶段触发（默认）
element.addEventListener("click", handler, false);
```

---

## 10. 异步编程

### 回调函数
```javascript
function fetchData(callback) {
  setTimeout(() => {
    callback(null, "数据");
  }, 1000);
}

fetchData((err, data) => {
  if (err) console.error(err);
  else console.log(data);
});
```

### Promise
```javascript
// 创建 Promise
const promise = new Promise((resolve, reject) => {
  setTimeout(() => {
    resolve("成功");
    // reject("失败");
  }, 1000);
});

// 使用 Promise
promise
  .then(data => console.log(data))
  .catch(err => console.error(err))
  .finally(() => console.log("完成"));

// 链式调用
fetch(url)
  .then(res => res.json())
  .then(data => console.log(data))
  .catch(err => console.error(err));

// 静态方法
Promise.all([p1, p2, p3])      // 全部成功才成功
Promise.allSettled([p1, p2, p3])  // 等待全部完成
Promise.race([p1, p2, p3])     // 第一个完成的
Promise.any([p1, p2, p3])      // 第一个成功的
```

### async/await
```javascript
// 声明异步函数
async function fetchData() {
  try {
    const response = await fetch(url);
    const data = await response.json();
    return data;
  } catch (err) {
    console.error(err);
  }
}

// 并行执行
async function getData() {
  const [users, posts] = await Promise.all([
    fetch("/api/users").then(r => r.json()),
    fetch("/api/posts").then(r => r.json())
  ]);
  return { users, posts };
}
```

### Promise 状态
| 状态 | 说明 |
|------|------|
| Pending | 初始状态 |
| Fulfilled | 操作成功 |
| Rejected | 操作失败 |

---

## 11. ES6+ 新特性

### 解构赋值
```javascript
// 数组解构
const [a, b] = [1, 2];

// 对象解构
const { name, age } = { name: "张三", age: 18 };

// 默认值
const { name, age = 18 } = {};

// 嵌套解构
const { address: { city } } = { address: { city: "北京" } };
```

### 展开运算符
```javascript
// 数组展开
const arr1 = [1, 2];
const arr2 = [...arr1, 3, 4];  // [1, 2, 3, 4]

// 对象展开
const obj1 = { a: 1 };
const obj2 = { ...obj1, b: 2 };  // { a: 1, b: 2 }

// 函数参数
function sum(...numbers) {
  return numbers.reduce((a, b) => a + b, 0);
}
```

### 模板字符串
```javascript
const name = "张三";
const greeting = `你好，${name}！`;
```

### 箭头函数
```javascript
const add = (a, b) => a + b;
const square = n => n * n;
```

### 默认参数
```javascript
function greet(name = "世界") {
  console.log(`你好，${name}`);
}
```

### 可选链 (?.)
```javascript
const city = user?.address?.city;
const result = arr?.[0];
const value = obj?.method?.();
```

### 空值合并 (??)
```javascript
const value = null ?? "默认";    // "默认"
const value = "" ?? "默认";      // "" (空字符串不为null)
const value = undefined ?? "默认"; // "默认"
```

### Map 和 Set
```javascript
// Map - 键值对集合
const map = new Map();
map.set("key", "value");
map.get("key");
map.has("key");
map.delete("key");
map.size;

// Set - 唯一值集合
const set = new Set([1, 2, 3, 3]);  // {1, 2, 3}
set.add(4);
set.has(1);
set.delete(1);
set.size;

// 数组去重
const unique = [...new Set(array)];
```

### Symbol
```javascript
const sym1 = Symbol("描述");
const sym2 = Symbol("描述");
sym1 === sym2;  // false

// 作为对象键
const myMethod = Symbol("myMethod");
const obj = {
  [myMethod]() { return "hello"; }
};
```

---

## 12. 面向对象

### 类 (Class)
```javascript
class Person {
  // 构造函数
  constructor(name, age) {
    this.name = name;
    this.age = age;
  }

  // 方法
  greet() {
    return `你好，我是${this.name}`;
  }

  // 静态方法
  static create(name, age) {
    return new Person(name, age);
  }

  // getter
  get info() {
    return `${this.name}, ${this.age}岁`;
  }

  // setter
  set age(value) {
    if (value < 0) throw new Error("年龄不能为负");
    this._age = value;
  }
}

// 继承
class Student extends Person {
  constructor(name, age, grade) {
    super(name, age);  // 调用父类构造函数
    this.grade = grade;
  }

  study() {
    return `${this.name}在学习`;
  }
}
```

### 原型链
```
实例 → 构造函数.prototype → Object.prototype → null
```

```javascript
// 原型继承
function Animal(name) {
  this.name = name;
}
Animal.prototype.speak = function() {
  return `${this.name}发出声音`;
};

function Dog(name) {
  Animal.call(this, name);
}
Dog.prototype = Object.create(Animal.prototype);
Dog.prototype.constructor = Dog;
```

---

## 13. 错误处理

### try...catch
```javascript
try {
  // 可能出错的代码
  const result = riskyOperation();
} catch (error) {
  // 处理错误
  console.error(error.message);
} finally {
  // 无论如何都会执行
  cleanup();
}
```

### 常见错误类型
| 错误类型 | 说明 |
|----------|------|
| `Error` | 通用错误 |
| `TypeError` | 类型错误 |
| `ReferenceError` | 引用错误 |
| `SyntaxError` | 语法错误 |
| `RangeError` | 范围错误 |
| `URIError` | URI 错误 |

### 自定义错误
```javascript
class ValidationError extends Error {
  constructor(message) {
    super(message);
    this.name = "ValidationError";
  }
}

throw new ValidationError("数据无效");
```

### 异步错误处理
```javascript
// Promise
promise.catch(err => console.error(err));

// async/await
async function fetchData() {
  try {
    const data = await fetch(url);
  } catch (err) {
    console.error(err);
  }
}
```

---

## 14. 本地存储

### localStorage
```javascript
// 存储（字符串）
localStorage.setItem("key", "value");
localStorage.setItem("user", JSON.stringify({name: "张三"}));

// 读取
const value = localStorage.getItem("key");
const user = JSON.parse(localStorage.getItem("user"));

// 删除
localStorage.removeItem("key");
localStorage.clear();

// 特点：永久存储，同源共享，约5MB
```

### sessionStorage
```javascript
// 用法与 localStorage 相同
sessionStorage.setItem("key", "value");
sessionStorage.getItem("key");

// 特点：会话结束自动清除，标签页独立
```

### 对比
| 特性 | localStorage | sessionStorage |
|------|--------------|----------------|
| 生命周期 | 永久 | 会话 |
| 作用域 | 同源 | 同源同标签页 |
| 容量 | ~5MB | ~5MB |
| 服务器 | 不发送 | 不发送 |

### Cookie
```javascript
// 设置
document.cookie = "name=张三; expires=Fri, 31 Dec 2025 23:59:59 GMT; path=/";

// 读取
const cookies = document.cookie;

// 特点：自动发送到服务器，约4KB，可设置过期时间
```

---

## 15. 正则表达式

### 创建正则
```javascript
// 字面量
const regex = /pattern/flags;

// 构造函数
const regex = new RegExp("pattern", "flags");
```

### 常用模式
| 模式 | 说明 | 示例 |
|------|------|------|
| `\d` | 数字 | `\d+` 匹配一个或多个数字 |
| `\w` | 单词字符 | `\w+` 匹配单词 |
| `\s` | 空白字符 | `\s+` 匹配空白 |
| `.` | 任意字符 | `.` 匹配单个字符 |
| `^` | 开头 | `^hello` 以hello开头 |
| `$` | 结尾 | `world$` 以world结尾 |
| `*` | 零次或多次 | `ab*c` 匹配 ac, abc, abbc |
| `+` | 一次或多次 | `ab+c` 匹配 abc, abbc |
| `?` | 零次或一次 | `ab?c` 匹配 ac, abc |
| `{n}` | 恰好n次 | `\d{3}` 匹配3位数字 |
| `{n,m}` | n到m次 | `\d{2,4}` 匹配2-4位数字 |
| `[]` | 字符集 | `[abc]` 匹配 a, b, 或 c |
| `|` | 或 | `cat|dog` 匹配 cat 或 dog |
| `()` | 分组 | `(ab)+` 匹配 ab, abab |

### 常用方法
```javascript
// 测试是否匹配
/abc/.test("abcdef");    // true

// 查找匹配
"hello".match(/\w+/);    // ["hello"]

// 查找所有匹配
"hello".match(/\w/g);    // ["h", "e", "l", "l", "o"]

// 替换
"hello".replace(/l/g, "L");  // "heLLo"

// 分割
"a,b,c".split(/,/);    // ["a", "b", "c"]
```

### 常用正则
```javascript
// 手机号
/^1[3-9]\d{9}$/

// 邮箱
/^[\w.-]+@[\w.-]+\.\w+$/

// URL
/^https?:\/\/[\w.-]+(:\d+)?(\/\w*)*$/

// 中文
/[\u4e00-\u9fa5]/

// 身份证号
/^\d{17}[\dX]$/
```

### 标志
| 标志 | 说明 |
|------|------|
| `g` | 全局匹配 |
| `i` | 忽略大小写 |
| `m` | 多行模式 |
| `s` | 点号匹配换行 |

---

## 常用代码片段

### 数组去重
```javascript
// 方法1: Set
const unique = [...new Set(array)];

// 方法2: filter
const unique = array.filter((item, index) => array.indexOf(item) === index);
```

### 数组扁平化
```javascript
// 方法1: flat
const flat = array.flat(Infinity);

// 方法2: 递归
function flatten(arr) {
  return arr.reduce((acc, item) =>
    Array.isArray(item) ? [...acc, ...flatten(item)] : [...acc, item], []);
}
```

### 深拷贝
```javascript
// 方法1: JSON
const copy = JSON.parse(JSON.stringify(obj));

// 方法2: 递归
function deepClone(obj) {
  if (obj === null || typeof obj !== 'object') return obj;
  const copy = Array.isArray(obj) ? [] : {};
  for (let key in obj) {
    if (obj.hasOwnProperty(key)) {
      copy[key] = deepClone(obj[key]);
    }
  }
  return copy;
}
```

### 防抖
```javascript
function debounce(fn, delay) {
  let timer;
  return function(...args) {
    clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}
```

### 节流
```javascript
function throttle(fn, delay) {
  let last = 0;
  return function(...args) {
    const now = Date.now();
    if (now - last >= delay) {
      last = now;
      fn.apply(this, args);
    }
  };
}
```

---

*最后更新: 2026-05-18*
