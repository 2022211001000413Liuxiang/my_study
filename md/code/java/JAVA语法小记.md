---
tags:
  - JAVA
  - 语法
---
## JAVA语法小记
#### 求长度

```java
//求长度
//数组
int n = nums.length;//无小括号，是属性
//字符串
int n =s.length();//有小括号，是方法
//ArrayList
ArrayList<String> sites = new ArrayList<String>();
System.out.println(sites.size());
```
#### 字符串
###### 取字符串中的某个元素
```java
String s = "java"; 
for (int i = 0; i < s.length(); i++) { 
	char c = s.charAt(i); 
	System.out.println(c);
	}
```
##### 可变字符缓冲区
在原对象上修改，不新建对象，适合大量拼接：
- **StringBuilder**：**单线程**用，速度快，不安全
- **StringBuffer**：**多线程**用，安全，速度慢
- 两者都是**可变字符串**，用来替代频繁拼接的 `String`
```java
//常用语法
append("xxx") // 拼接 
reverse() // 反转 
delete(start,end)// 删除
insert(下标,值) // 插入 
toString() // 转String
```
#### 基本类型与包装类
int → Integer 
char → Character 
boolean → Boolean 
double → Double
在集合和泛型中，类全都要换成包装类。（基本类型->对象类型）
#### 排序
```java
//collections类排序
ArrayList<String> list = new ArrayList<String>();
Collections.sort(list);
//JDK8+支持语法，lamada表达式
//a-b >0 a比b大，交换位置，升序->返回的是int类型
//a-b<0 a比b小，不交换
list.sort((a,b) -> a-b);//升序->return正数交换位置
list.sort((a,b) -> b-a);//降序

```
对于普通数组，int类型不支持lamada,而Interger类支持lamada表达式排序

## 面向对象

### 重写与重载

``` java
//重写：子类对父类同一方法的改造，（接收的参数类型相同）作出不同的响应
//重载：同类中不同方法的方法名相同，参数类型不同，用于不同的调用
```

![image-20260504114300558](D:\笔记\markdown\图片\image-20260504114300558.png)

![image-20260504114344533](C:\Users\aa\AppData\Roaming\Typora\typora-user-images\image-20260504114344533.png)

### 多态

同一个行为 ，不同子类有不同的实现；父类引用调用子类对象，运行时自动调用子类方法

前提条件：

1.有继承关系

2.子类重写了父类方法

3.父类引用指向子类对象

4.程序运行时动态绑定

``` java
//父类 引用名 = new 子类（）；
Animal a1 = new Dog();
Animal a2 = new Cat();
a1.shout();//子类狗方法
a2.shout();//子类猫方法
```

### 抽象类与抽象方法

![image-20260505165101864](D:\笔记\markdown\图片\image-20260505165101864.png)

特点：

- 1. 抽象类不能被实例化(初学者很容易犯的错)，如果被实例化，就会报错，编译无法通过。只有抽象类的非抽象子类可以创建对象。
- 2. 抽象类中不一定包含抽象方法，但是有抽象方法的类必定是抽象类。
- 3. 抽象类中的抽象方法只是声明，不包含方法体，就是不给出方法的具体实现也就是方法的具体功能。
- 4. 构造方法，类方法（用 static 修饰的方法）不能声明为抽象方法。
- 5. 抽象类的子类必须给出抽象类中的抽象方法的具体实现，除非该子类也是抽象类。

```java
//抽象方法格式
public abstract 返回类型 方法名(参数列表){
    
}
//抽象类格式
public abstract class 类名(){
    
}
```

### 反射

> 在运行状态中，动态获取类的信息并操作对象的机制

核心类：`Class`（类的类）、`Field`（成员变量）、`Method`（成员方法）、`Constructor`（构造方法）

```java
// 获取 Class 对象
Class<?> c = Student.class;  // 方式一
Class<?> c = new Student().getClass();  // 方式二
Class<?> c = Class.forName("com.example.Student");  // 方式三，常用

// 创建对象
Object obj = c.getConstructor().newInstance();

// 获取成员方法并调用
Method m = c.getMethod("speak");
m.invoke(obj);

// 操作私有成员（需设置访问权限）
Field f = c.getDeclaredField("name");
f.setAccessible(true);
f.set(obj, "李四");
```

应用场景：JDBC注册驱动、Spring创建Bean、注解处理、JSON序列化

### 泛型

> 将类型参数化，在编译时检查类型安全，避免强制类型转换
> T是类型占位符，可理解为形参代替的是类型
> - **T** — Type（通用类型）
- **E** — Element（集合元素）
- **K** — Key / **V** — Value（键值对）
- **N** — Number
[[SpringBoot#2.2 统一响应封装]]
#### 泛型类
```java
public class Box<T> {
    private T content;
    public void set(T content) { this.content = content; }
    public T get() { return content; }
}
// 使用
Box<String> box = new Box<>();
box.set("hello");
String s = box.get(); // 无需强转
```

#### 泛型方法
```java
// <T> 声明在返回值之前
public static <T> void print(T[] arr) {
    for (T item : arr) System.out.println(item);
}
// 调用时类型自动推断
print(new Integer[]{1, 2, 3});
```

#### 泛型接口
```java
public interface Comparator<T> {
    int compare(T a, T b);
}
// 实现时指定类型
public class NameComparator implements Comparator<String> {
    public int compare(String a, String b) { return a.compareTo(b); }
}
```

#### 类型通配符
```java
// <?> 无界通配符，只能读不能写
public void printList(List<?> list) { ... }

// <? extends T> 上界，传入 T 或 T 的子类，只读
public double sum(List<? extends Number> list) { ... }

// <? super T> 下界，传入 T 或 T 的父类，可写入 T 类型
public void addInt(List<? super Integer> list) { list.add(1); }
```

#### 类型擦除
泛型只在**编译期**生效，运行时会被擦除为原始类型（Object 或上界类型）。所以：
- 不能 `new T()`、`new T[]`
- 不能用基本类型作泛型参数（`List<int>` ✗，`List<Integer>` ✓）
- 不能用 `instanceof` 判断泛型类型






[[SpringBoot]]