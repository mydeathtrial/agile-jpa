# agile-jpa ： spring-data-jpa扩展

[![spring-data-jpa](https://img.shields.io/badge/Spring--data--jpa-LATEST-green)](https://img.shields.io/badge/Spring--data--jpa-LATEST-green)
[![maven](https://img.shields.io/badge/build-maven-green)](https://img.shields.io/badge/build-maven-green)

## 它有什么作用

* **持久层工具合并**
  使用该组件后，全局仅需要注入唯一的Dao工具，无需再声明其他Dao/Repository，一个工具搞定所有Dao，极大简化持久层代码量。

* **原生动态sql解析支持**
  依靠agile-sql（动态sql解析器）实现jpa中对sql语法段的动态解析，弥补其在动态sql解析方面的短板。面对复杂查询语句将不再是难点。

* **内置slq分页与jpa分页**
  JPA风格的Do类对象操作形式分页与原生SQL形式分页均返回统一的分页数据格式，且提供total总条数结果

* **内置字典翻译**
  依靠agile-dictionary组件，查询过程中会识别`@Dictionary`注解，如果不需要字典翻译时可以将该组件于pom中直接移除

-------

## 快速入门

开始你的第一个项目是非常容易的。

#### 步骤 1: 下载包

您可以从[最新稳定版本]下载包(https://github.com/mydeathtrial/agile-jpa/releases). 该包已上传至maven中央仓库，可在pom中直接声明引用

以版本agile-jpa-2.1.0.M5.jar为例。

#### 步骤 2: 添加maven依赖

```xml
<!--声明中央仓库-->
<repositories>
    <repository>
        <id>cent</id>
        <url>https://repo1.maven.org/maven2/</url>
    </repository>
</repositories>
        <!--声明依赖-->
<dependency>
<groupId>cloud.agileframework</groupId>
<artifactId>agile-jpa</artifactId>
<version>2.1.0.M5</version>
</dependency>
```

#### 步骤 3: 开箱即用，由于API众多，本文只介绍常用方法，更多方法请查看javadoc

- <span id="id1">新增</span>

```
/**
 * 功能：延迟保存
 * 特点：无回参，flush后产生ID，事务提交后保存
 */
dao.save(new SysUsersEntity());
```

```
/**
 * 功能：立即保存
 * 特点：有回参，立即产生ID，事务提交后保存，不立即调用flush
 */
SysUsersEntity entity = dao.saveAndReturn(new SysUsersEntity());
```

```
/**
 * 功能：立即保存
 * 特点：有回参，立即产生ID，事务提交后保存，立即调用flush
 */
SysUsersEntity entity = dao.saveAndReturn(new SysUsersEntity()，true);
```

```
/**
 * 功能：批量立即保存
 * 特点：有回参，立即产生ID，事务提交后保存，不立即调用flush
 */
List<SysUsersEntity> list = new ArrayList<>(2);
list.add(new SysUsersEntity());
list.add(new SysUsersEntity());
boolean isSuccess = dao.save(list);
```

------------

- <span id="id2">删除</span>

```
/**
 * 功能：批量模糊删除数据
 * 特点：无回参，先根据入参对象进行查询，再批量删除结果集
 * 例子：该例子中会删除SysUsersEntity对应表中Name字段为Tom的所有数据
 */
SysUsersEntity entity = new SysUsersEntity()；
entity.setName("Tom");
dao.delete(entity);
```

```
/**
 * 功能：删除对应表指定ID的数据
 * 特点：无回参，删除单条数据
 * 例子：该例子中会删除SysUsersEntity对应表中主键字段为123的有数据
 */
dao.deleteById(SysUsersEntity.class,"123");
```

```
/**
 * 功能：清空全表
 * 特点：无回参，一条一条删除
 * 例子：该例子中会删除SysUsersEntity对应表所有数据
 */
dao.deleteAll(SysUsersEntity.class);
```

```
/**
 * 功能：清空全表
 * 特点：无回参，一次性清空
 * 例子：该例子中会删除SysUsersEntity对应表所有数据
 */
dao.deleteAllInBatch(SysUsersEntity.class);
```

```
/**
 * 功能：批量删除指定表指定主键的数据
 * 特点：无回参，一次性清空
 * 例子：该例子中会删除SysUsersEntity对应表主键为1111、2222的数据
 */
dao.deleteInBatch(SysUsersEntity.class,new Object[]{"1111","2222"});
```

```
/**
 * 功能：批量删除指定表指定的数据集
 * 特点：无回参，一次性清空
 * 例子：该例子中会删除SysUsersEntity对应表指定两条数据
 */
List<SysUsersEntity> list = new ArrayList<>(2);
list.add(new SysUsersEntity());
list.add(new SysUsersEntity());
dao.deleteInBatch(list);
```

```
/**
 * 功能：sql方式批量删除
 * 特点：有回参，一次性清空
 * 例子：该例子中会删除SysUsersEntity对应表主键为1111、2222的数据
 */
String sql = "delete from sys_suers where name  in {names}";
Map<String,Object> params = new HashMap<>(1);
params.put("names",new String[]{"1111","2222"});
int total = dao.updateBySQL(sql, params);
```

------------

- <span id="id3">修改</span>

```
/**
 * 功能：更新数据
 * 特点：有回参,null字段会被清空
 */
SysUsersEntity newEntity = ...
dao.update(newEntity);
```

```
/**
 * 功能：更新非空字段数据
 * 特点：有回参，null字段不更新
 */
SysUsersEntity newEntity = ...
dao.updateOfNotNull(newEntity);
```

```
/**
 * 功能：sql方式更新数据
 * 特点：有回参，null字段不更新
 * 例子：该例子中会更新SysUsersEntity对应表主键为1111的数据，name为Tom
 */
String sql = "update sys_suers set name = ? where id = ? ";
int total = dao.updateBySQL(sql,"Tom","1111");
```

------------

- <span id="id4">查询</span>
  <span style="color:red">查询方法多而杂，本文只例举常用例子，具体用发参照java doc</span>
  只有select支持 {id} 方式传参。如果该key值没有传参数，则会去掉该条件

> **1、只有select支持 {id} 方式传参。如果该key值没有传参数，则会去掉该条件**
**2、 当 ? 参数没有值时候条件不会去掉。例如：selec * from person where name=? 没有传参时则条件不会去掉**

```
/**
 * 功能：根据ID查询单条
 * 特点：无
 * 例子：该例子中会查询SysUsersEntity对应表主键为1111的数据
 */
SysUsersEntity entity = dao.findOne(SysUsersEntity.class,"1111");
```

```
/**
 * 功能：根据例子查询单条
 * 特点：如果查询结果超过一条，则抛出异常
 * 例子：该例子中会查询SysUsersEntity对应表中一条name为Tom的数据
 */
SysUsersEntity entity = new SysUsersEntity()；
entity.setName("Tom");
SysUsersEntity entity = dao.findOne(entity);
```

```
/**
 * 功能：根据sql语句查询单条
 * 特点：查询结果会映射为你提供的参数类型（第二个参数），如果查询结果超过一条，则抛出异常
 * 例子：该例子中会查询SysUsersEntity对应表中一条id为1111的数据
 */
String sql = "select * from sys_users where id = ?";
SysUsersEntity entity = dao.findOne(sql,SysUsersEntity.class,"1111");
```

```
/**
 * 功能：根据sql语句查询单条
 * 特点：查询结果会映射为你提供的参数类型（第二个参数）；提供动态sql语句服务，根据参数有无动态踢除sql语法段；如果查询结果超过一条，则抛出异常
 * 例子：该例子中会查询SysUsersEntity对应表中一条id为1111的数据
 */
String sql = "select * from sys_users where id = {id1} or id = {id2}";
Map<String,Object> params = new HashMap<>(1);
params.put("id1","1111");
SysUsersEntity entity = dao.findOne(sql,SysUsersEntity.class,params);
```

```
/**
 * 功能：根据例子查询数据集
 * 特点：无
 * 例子：该例子中会查询SysUsersEntity对应表name字段值为Tom的所有数据
 */
SysUsersEntity entity = new SysUsersEntity()；
entity.setName("Tom");
SysUsersEntity entity = dao.findAll(entity);
```

```
/**
 * 功能：根据例子与排序信息Sort查询数据集并排序
 * 特点：提供排序
 * 例子：该例子中会查询SysUsersEntity对应表name字段值为Tom的所有数据，且根据name与code字段做降序排序
 */
Sort sort = new Sort(Sort.Direction.DESC,"name","code");
SysUsersEntity entity = new SysUsersEntity()；
entity.setName("Tom");
SysUsersEntity entity = dao.findAll(entity，sort);
```

```
/**
 * 功能：根据sql查询数据集
 * 特点：提供动态sql服务,结果为List<T>
 * 例子：该例子中会查询SysUsersEntity对应表id字段值为1111的所有数据
 */
String sql = "select * from sys_users where id = {id1} or id = {id2}";
Map<String,Object> params = new HashMap<>(1);
params.put("id1","1111");
List<SysUsersEntity> entity = dao.findAll(sql，SysUsersEntity.class,params);
```

```
/**
 * 功能：根据实体类型查询全表数据
 * 特点：无
 * 例子：该例子中会查询SysUsersEntity对应表全部数据
 */
List<SysUsersEntity> list = dao.findAll(SysUsersEntity.class);
```

```
/**
 * 功能：根据sql查询一条数据的一个字段值
 * 特点：提供动态sql服务,结果为Object
 * 例子：该例子中会查询SysUsersEntity对应表id字段值为1111的name字段数据
 * 参数：第二个参数可以为对象数组或Iterable类型
 */
List<SysUsersEntity> list = dao.findAllById(SysUsersEntity.class，new String[]{"1111","2222"});
```

```
/**
 * 功能：根据sql查询数据集
 * 特点：提供动态sql服务,结果为List<Map>
 * 例子：该例子中会查询SysUsersEntity对应表id字段值为1111的所有数据
 */
String sql = "select * from sys_users where id = {id1} or id = {id2}";
Map<String,Object> params = new HashMap<>(1);
params.put("id1","1111");
List<Map<String, Object>> entity = dao.findAllBySQL(sql，params);
```

```
/**
 * 功能：根据sql查询一条数据的一个字段值
 * 特点：提供动态sql服务,结果为Object
 * 例子：该例子中会查询SysUsersEntity对应表id字段值为1111的name字段数据
 */
String sql = "select name from sys_users where id = {id1}";
Map<String,Object> params = new HashMap<>(1);
params.put("id1","1111");
Object entity = dao.findParameter(sql，params);
```

------------

- <span id="id5">分页</span>
  <span style="color:red">查询方法多而杂，本文只例举常用例子，具体用发参照java doc</span>

```
/**
 * 功能：按照例子查询 + 分页
 * 特点：首页从1计算
 * 例子：该例子中会查询SysUsersEntity对应表首页，10条数据
 */
 int pageSize = 10;
 int pageNum = 1;
SysUsersEntity entity = new SysUsersEntity()；
entity.setName("Tom");
Page page = dao.findAll(entity，pageNum，pageSize);
```

```
/**
 * 功能：按照例子查询 + 分页 + 排序
 * 特点：首页从1计算
 * 例子：该例子中会查询SysUsersEntity对应表首页，10条数据，且排序
 */
 int pageSize = 10;
 int pageNum = 1;
Sort sort = new Sort(Sort.Direction.DESC,"code");
SysUsersEntity entity = new SysUsersEntity()；
entity.setName("Tom");
Page page = dao.findAll(entity，pageNum，pageSize，sort);
```

```
/**
 * 功能：按照sql查询 + 分页
 * 特点：首页从1计算
 * 例子：该例子中会查询SysUsersEntity对应表首页，10条数据，且排序
 * 提示：复杂sql建议使用MyBatis进行查询操作
 */
 int pageSize = 10;
 int pageNum = 1;
String sql = "select name from sys_users where id = {id1}";
Map<String,Object> params = new HashMap<>(1);
params.put("id1","1111");
Page page = dao.findPageBySQL(sql，pageNum,pageSize，params);
```

- <span id="id11">like查询</span>

```
String sql= "select * from sys_role where name like '%{name}%'";
Page<ParamRoleEntity> page = dao.findPageBySQL(sql, getInParam("pageNum", Integer.class), getInParam("pageSize", Integer.class), ParamRoleEntity.class, getInParam());

```

- <span id="id12">批量删除或者更新</span>

```
// 主键集合，支持Collection类型及数组类型
List<String> ids = getInParamOfArray("ids");
dao.updateBySQL("update sys_users set dele_flag = 1 where sys_users_id in ({ids})", getInParam());
```
