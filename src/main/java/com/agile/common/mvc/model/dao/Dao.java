package com.agile.common.mvc.model.dao;

import com.agile.common.dictionary.DataExtendManager;
import com.agile.common.exception.NoSuchIDException;
import com.agile.common.util.SqlUtil;
import com.agile.common.util.clazz.ClassUtil;
import com.agile.common.util.clazz.TypeReference;
import com.agile.common.util.object.ObjectUtil;
import com.agile.common.util.string.StringUtil;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author 佟盟 on 2017/11/15
 */
public class Dao {
    private static final Map<Class<?>, SimpleJpaRepository> REPOSITORY_CACHE = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(Dao.class);
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private DataExtendManager dictionaryManager;

    /**
     * 根据java类型获取对应的数据库表的JpaRepository对象
     *
     * @param tableClass 表对应的实体类型
     * @param <T>        表对应的实体类型
     * @param <ID>       主键类型
     * @return 对应的数据库表的JpaRepository对象
     */
    @SuppressWarnings("unchecked")
    public <T, ID> SimpleJpaRepository<T, ID> getRepository(Class<T> tableClass) {
        SimpleJpaRepository<T, ID> repository = REPOSITORY_CACHE.get(tableClass);
        if (ObjectUtils.isEmpty(repository)) {
            repository = new SimpleJpaRepository<>(tableClass, getEntityManager());
            REPOSITORY_CACHE.put(tableClass, repository);
        }
        return repository;
    }

    /**
     * 获取EntityManager，操作jpa api的入口
     *
     * @return EntityManager
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * 保存
     *
     * @param o ORM对象
     */
    public void save(Object o) {
        getEntityManager().persist(o);
    }

    /**
     * 批量保存
     *
     * @param list 表对应的实体类型的对象列表
     * @param <T>  表对应的实体类型
     * @return 是否保存成功
     */
    @SuppressWarnings("unchecked")
    public <T> boolean save(Iterable<T> list) {
        boolean isTrue = false;
        Iterator<T> iterator = list.iterator();
        if (iterator.hasNext()) {
            T obj = iterator.next();
            Class<T> tClass = (Class<T>) obj.getClass();
            getRepository(tClass).saveAll(list);
            isTrue = true;

        }
        return isTrue;
    }

    /**
     * 获取数据库连接
     *
     * @return Connection
     */
    public Connection getConnection() {
        return getEntityManager().unwrap(SessionImplementor.class).connection();
    }

    public boolean contains(Object o) {
        return getEntityManager().contains(o);
    }

    public <T> T saveOrUpdate(T o) {
        if (getEntityManager().contains(o)) {
            return saveAndReturn(o);
        } else {
            return update(o);
        }
    }

    /**
     * 保存并刷新
     *
     * @param o       表对应的实体类型的对象
     * @param isFlush 是否刷新
     * @param <T>     泛型
     * @return 保存后的对象
     */
    @SuppressWarnings("unchecked")
    public <T> T saveAndReturn(T o, boolean isFlush) {
        T e;
        Class<T> clazz = (Class<T>) o.getClass();
        if (isFlush) {
            e = getRepository(clazz).saveAndFlush(o);
        } else {
            e = getRepository(clazz).save(o);
        }
        dictionaryManager.cover(e);
        return e;
    }

    /**
     * 保存
     *
     * @param o   要保存的对象
     * @param <T> 泛型
     * @return 保存后的对象
     */
    public <T> T saveAndReturn(T o) {
        return saveAndReturn(o, Boolean.FALSE);
    }

    /**
     * 批量保存
     *
     * @param list 要保存的对象列表
     * @param <T>  表对应的实体类型
     * @return 保存后的数据集
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> saveAndReturn(Iterable<T> list) {
        Iterator<T> iterator = list.iterator();
        if (iterator.hasNext()) {
            T obj = iterator.next();
            Class<T> clazz = (Class<T>) obj.getClass();
            return getRepository(clazz).saveAll(list);
        }
        return new ArrayList<>(0);
    }

    /**
     * 根据表实体类型与主键值，判断数据是否存在
     *
     * @param tableClass 表对应的实体类型
     * @param id         数据主键
     * @return 是否存在
     */
    public boolean existsById(Class<?> tableClass, Object id) {
        return getRepository(tableClass).existsById(id);
    }

    /**
     * 刷新数据库中指定tableClass类型实体对应的表
     *
     * @param tableClass 表对应的实体类型
     */
    public void flush(Class<?> tableClass) {
        getRepository(tableClass).flush();
    }

    /**
     * 刷新数据库中全部表
     */
    public void flush() {
        getEntityManager().flush();
    }


    /**
     * 刷新数据库数据到实体类当中
     *
     * @param o 表对应的实体类型的对象
     */
    public void refresh(Object o) {
        getEntityManager().refresh(o);
    }

    /**
     * 更新或新增
     *
     * @param o   ORM对象
     * @param <T> 表对应的实体类型
     * @return 返回更新后的数据
     */
    public <T> T update(T o) {
        T e = getEntityManager().merge(o);
        dictionaryManager.cover(e);
        return e;
    }

    /**
     * 更新或新增非空字段，空字段不进行更新
     *
     * @param o   表映射实体类型的对象
     * @param <T> 表映射实体类型的对象
     * @return 返回更新后的数据
     * @throws NoSuchIDException      异常
     * @throws IllegalAccessException 异常
     */
    @SuppressWarnings("unchecked")
    public <T> T updateOfNotNull(T o) throws NoSuchIDException, IllegalAccessException {
        Class<T> clazz = (Class<T>) o.getClass();
        Field idField = getIdField(clazz);
        idField.setAccessible(true);
        T old = findOne(clazz, idField.get(o));
        ObjectUtil.copyProperties(o, old, ObjectUtil.Compare.DIFF_SOURCE_NOT_NULL);
        T e = getEntityManager().merge(old);
        dictionaryManager.cover(e);
        return e;
    }

    /**
     * 根据提供的对象参数，作为例子，查询出结果并删除
     *
     * @param o 表实体对象
     */
    public <T> void delete(T o) {
        List<T> list = findAll(o);
        deleteInBatch(list);
    }

    /**
     * 删除
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param id         删除的主键标识
     * @param <T>        查询的目标表对应实体类型
     */
    public <T> boolean deleteById(Class<T> tableClass, Object id) {
        try {
            getRepository(tableClass).deleteById(id);
        } catch (EmptyResultDataAccessException ignored) {
            return false;
        }
        return true;

    }

    /**
     * 删除全部(逐一删除)
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param <T>        查询的目标表对应实体类型
     */
    public <T> void deleteAll(Class<T> tableClass) {
        getRepository(tableClass).deleteAll();
    }

    /**
     * 删除全部(一次性删除)
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param <T>        查询的目标表对应实体类型
     */
    public <T> void deleteAllInBatch(Class<T> tableClass) {
        getRepository(tableClass).deleteAllInBatch();
    }

    /**
     * 根据表映射的实体类型与主键值集合，创建一个只包含主键值的空的对象集合
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param ids        主键数组
     * @param <T>        查询的目标表对应实体类型
     * @param <ID>       查询的目标表对应实体主键类型
     * @return 结果集
     * @throws NoSuchIDException tableClass实体类型中没有找到@ID的注解，识别成主键字段
     */
    private <T, ID> List<T> createObjectList(Class<T> tableClass, ID[] ids) throws NoSuchIDException {
        ArrayList<T> list = new ArrayList<>();
        Field idField = getIdField(tableClass);
        for (ID id : ids) {
            try {
                T instance = tableClass.newInstance();
                idField.setAccessible(true);
                idField.set(instance, ObjectUtil.to(id, new TypeReference<>(idField.getType())));
                list.add(instance);
            } catch (IllegalAccessException | InstantiationException e) {
                logger.error("主键数组转换ORM对象列表失败", e);
            }
        }
        return list;
    }

    private Object getId(Object o) throws NoSuchIDException, IllegalAccessException {
        return getIdField(o.getClass()).get(o);
    }

    /**
     * 获取ORM中的主键字段
     *
     * @param clazz 查询的目标表对应实体类型，Entity
     * @return 主键属性
     * @throws NoSuchIDException tableClass实体类型中没有找到@ID的注解，识别成主键字段
     */
    public Field getIdField(Class<?> clazz) throws NoSuchIDException {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            method.setAccessible(true);
            Id id = method.getAnnotation(Id.class);
            if (!ObjectUtils.isEmpty(id)) {
                try {
                    Field field = clazz.getDeclaredField(StringUtil.toLowerName(method.getName().replaceFirst("get", "")));
                    field.setAccessible(true);
                    return field;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    throw new NoSuchIDException();
                }
            }
        }
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Id id = field.getAnnotation(Id.class);
            if (!ObjectUtils.isEmpty(id)) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchIDException();
    }

    /**
     * 根据主键与实体类型，部分删除，删除对象集(一次性删除)
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param <T>        查询的目标表对应实体类型
     * @param ids        主键数组
     */
    public <T> void deleteInBatch(Class<T> tableClass, Object[] ids) {
        if (ArrayUtils.isEmpty(ids) || ids.length < 1) {
            return;
        }
        SimpleJpaRepository<T, Object> repository = getRepository(tableClass);
        for (Object id : ids) {
            repository.deleteById(id);
        }
    }

    public <T> void deleteInBatch(Class<T> tableClass, Iterable<?> ids) {
        if (ids == null) {
            return;
        }
        SimpleJpaRepository<T, Object> repository = getRepository(tableClass);
        for (Object id : ids) {
            repository.deleteById(id);
        }
    }

    /**
     * 根据表映射类型的对象集合，部分删除，删除对象集(一次性删除)，无返回值
     *
     * @param list 需要删除的对象列表
     * @param <T>  删除对象集合的对象类型，用于生成sql语句时与对应的表进行绑定
     */
    public <T> void deleteInBatch(Iterable<T> list) {
        for (T obj : list) {
            try {
                getRepository(obj.getClass()).deleteById(getId(obj));
            } catch (NoSuchIDException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据主键，查询单条
     *
     * @param clazz 查询的目标表对应实体类型，Entity
     * @param id    主键
     * @param <T>   查询的目标表对应实体类型
     * @return clazz类型对象
     */
    public <T> T findOne(Class<T> clazz, Object id) {
        T e = getEntityManager().find(clazz, id);
        dictionaryManager.cover(e);
        return e;
    }

    /**
     * 按照例子查询单条
     *
     * @param <T>    查询的表的映射实体类型
     * @param object 查询一句的例子对象
     * @return 返回查询结果
     */
    @SuppressWarnings("unchecked")
    public <T> T findOne(T object) {
        Example<T> example = Example.of(object);
        Class<T> clazz = (Class<T>) object.getClass();
        T e = (T) this.getRepository(clazz).findOne(example).orElse(null);
        dictionaryManager.cover(e);
        return e;
    }

    /**
     * 根据sql查询出单条数据，并映射成指定clazz类型
     *
     * @param <T>        查询的表的映射实体类型
     * @param sql        sql
     * @param clazz      查询的目标表对应实体类型，Entity
     * @param parameters 对象数组格式的sql语句中的参数集合，使用?方式占位
     * @return 查询的结果
     */
    @SuppressWarnings("unchecked")
    public <T> T findOne(String sql, Class<T> clazz, Object... parameters) {
        Query query = creatQuery(sql, parameters);
        queryCoverMap(query);
        List<?> result = query.getResultList();

        if (result.size() == 0) {
            return null;
        }
        if (result.size() != 1) {
            throw new NonUniqueResultException(String.format("Call to stored procedure [%s] returned multiple results", sql));
        }
        Map<String, Object> o = (Map<String, Object>) result.get(0);
        T e = ObjectUtil.to(o, new TypeReference<>(clazz));
        dictionaryManager.cover(e);
        return e;
    }

    /**
     * 根据sql查询出单条数据，并映射成指定clazz类型
     *
     * @param <T>        查询的表的映射实体类型
     * @param sql        原生sql，使用{Map的key值}形式占位
     * @param clazz      查询的目标表对应实体类型，Entity
     * @param parameters Map形式参数集合
     * @return 查询的结果
     */
    public <T> T findOne(String sql, Class<T> clazz, Object parameters) {
        if (parameters != null && (canCastClass(parameters.getClass()) || parameters.getClass().isArray() || parameters instanceof Collection)) {
            return findOne(sql, clazz, new Object[]{parameters});
        }
        T e = findOne(SqlUtil.parserSQL(sql, parameters), clazz);
        dictionaryManager.cover(e);
        return e;
    }

    public static boolean canCastClass(Class<?> clazz) {
        if (ClassUtil.isWrapOrPrimitive(clazz)) {
            return true;
        }
        return String.class == clazz || BigDecimal.class == clazz || Date.class == clazz;
    }

    /**
     * 按照例子查询多条
     *
     * @param <T>    查询的表的映射实体类型
     * @param object 例子对象
     * @return 查询结果数据集合
     */
    public <T> List<T> findAll(T object) {
        List<T> list = findAll(object, Sort.unsorted());
        dictionaryManager.cover(list);
        return list;
    }

    /**
     * 按照例子查询多条/排序
     *
     * @param <T>    查询的表的映射实体类型
     * @param object 例子对象
     * @param sort   排序对象
     * @return 查询结果数据集合
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(T object, Sort sort) {
        Example<T> example = Example.of(object);
        Class<T> clazz = (Class<T>) object.getClass();
        List<T> result = this.getRepository(clazz).findAll(example, sort);
        dictionaryManager.cover(result);
        return result;
    }

    /**
     * 根据例子查询列表
     *
     * @param example 例子
     * @param <T>     泛型
     * @return 列表
     */
    public <T> List<T> findAllByExample(Example<T> example) {
        return findAllByExample(example, Sort.unsorted());
    }

    /**
     * 根据例子查询列表
     *
     * @param example 例子
     * @param sort    排序
     * @param <T>     泛型
     * @return 列表
     */
    public <T> List<T> findAllByExample(Example<T> example, Sort sort) {
        List<T> result = this.getRepository(example.getProbeType()).findAll(example, sort);
        dictionaryManager.cover(result);
        return result;
    }

    /**
     * 按照例子查询多条分页
     *
     * @param <T>    查询的表的映射实体类型
     * @param object 例子对象
     * @param page   第几页
     * @param size   每页条数
     * @return 分页对象
     */
    public <T> Page<T> findAll(T object, int page, int size) {
        return findAll(object, page, size, Sort.unsorted());
    }

    /**
     * 按照例子对象查询多条分页
     *
     * @param <T>    查询的表的映射实体类型
     * @param object 例子对象
     * @param page   第几页
     * @param size   每页条数
     * @param sort   排序对象
     * @return 分页信息
     */
    public <T> Page<T> findAll(T object, int page, int size, Sort sort) {
        validatePageInfo(page, size);
        return findAll(object, PageRequest.of(page - 1, size, sort));
    }

    @SuppressWarnings("unchecked")
    public <T> Page<T> findAll(T object, PageRequest pageRequest) {
        if (object instanceof Class) {
            return this.getRepository((Class<T>) object).findAll(pageRequest);
        }
        Example<T> example = Example.of(object);
        Class<T> clazz = (Class<T>) object.getClass();
        Page<T> page = this.getRepository(clazz).findAll(example, pageRequest);
        dictionaryManager.cover(page.getContent());
        return page;
    }

    private void queryCoverMap(Query query) {
        if (query instanceof NativeQueryImpl) {
            ((NativeQueryImpl<?>) query).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        } else if (Proxy.isProxyClass(query.getClass())) {
            try {
                String setResultTransformer = "setResultTransformer";
                Method method = NativeQueryImpl.class.getDeclaredMethod(setResultTransformer, ResultTransformer.class);
                Proxy.getInvocationHandler(query).invoke(query, method, new Object[]{Transformers.ALIAS_TO_ENTITY_MAP});
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据sql语句查询指定类型clazz列表
     *
     * @param sql         查询的sql语句，参数使用？占位
     * @param clazz       希望查询结果映射成的实体类型
     * @param <T>         指定返回类型
     * @param firstResult 第一条数据
     * @param maxResults  最大条数据
     * @param parameters  对象数组类型的参数集合
     * @return 结果集
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(String sql, Class<T> clazz, Integer firstResult, Integer maxResults, Object... parameters) {
        Query query = creatQuery(sql, parameters);
        queryCoverMap(query);
        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        List<Map<String, Object>> list = query.getResultList();

        if (list != null && list.size() > 0) {
            List<T> result = new ArrayList<>();
            if (canCastClass(clazz)) {
                for (Map<String, Object> entity : list) {
                    T node = ObjectUtil.to(entity.values().toArray()[entity.values().size() - 1], new TypeReference<>(clazz));
                    if (node != null) {
                        result.add(node);
                    }
                }
            } else {
                for (Map<String, Object> entity : list) {
                    T node = ObjectUtil.to(entity, new TypeReference<>(clazz));
                    if (node != null) {
                        result.add(node);
                    }
                }
            }
            dictionaryManager.cover(result);
            return result;
        }
        return new ArrayList<>(0);
    }

    /**
     * 根据sql语句查询指定类型clazz列表
     *
     * @param sql        查询的sql语句，参数使用？占位
     * @param clazz      希望查询结果映射成的实体类型
     * @param <T>        指定返回类型
     * @param parameters 对象数组类型的参数集合
     * @return 结果集
     */
    public <T> List<T> findAll(String sql, Class<T> clazz, Object... parameters) {
        return findAll(sql, clazz, null, null, parameters);
    }

    /**
     * 根据sql查询结果为List<clazz>类型的结果集
     *
     * @param sql        查询sql语句，使用{Map的key值}形式占位
     * @param clazz      查询结果需要映射的实体
     * @param parameters Map类型参数集合
     * @param <T>        查询结果需要映射的实体类型
     * @return 类型的结果集
     */
    public <T> List<T> findAll(String sql, Class<T> clazz, Object parameters) {
        if (parameters != null && (canCastClass(parameters.getClass()) || parameters.getClass().isArray() || parameters instanceof Collection)) {
            return findAll(sql, clazz, null, null, parameters);
        }
        return findAll(SqlUtil.parserSQL(sql, parameters), clazz);
    }

    public static void validatePageInfo(int page, int size) throws IllegalArgumentException {
        if (size < 1) {
            throw new IllegalArgumentException("每页显示条数最少为数字 1");
        }
        if (page < 1) {
            throw new IllegalArgumentException("最小页为数字 1");
        }
    }

    /**
     * 分页查询
     *
     * @param sql        查询的sql语句
     * @param countSql   统计总数的sql语句
     * @param page       第几页
     * @param size       页大小
     * @param parameters 对象数组类型的参数集合
     * @return Page类型的查询结果
     */
    @SuppressWarnings("unchecked")
    public <T> Page<T> findPageBySQL(String sql, String countSql, int page, int size, Class<T> clazz, Object... parameters) {
        validatePageInfo(page, size);
        PageImpl<T> pageDate = null;
        PageRequest pageable;

        List<Sort.Order> sorts = Lists.newArrayList();

        List<SQLSelectOrderByItem> items = SqlUtil.getSort(sql);
        if (items != null) {
            for (SQLSelectOrderByItem item : items) {
                String column = item.getExpr().toString();
                if (item.getType() == null) {
                    sorts.add(Sort.Order.by(column));
                } else {
                    Sort.Direction des = Sort.Direction.fromString(item.getType().name_lcase);
                    switch (des) {
                        case ASC:
                            sorts.add(Sort.Order.asc(column));
                            break;
                        case DESC:
                            sorts.add(Sort.Order.desc(column));
                            break;
                        default:
                    }
                }
            }
        }

        if (sorts.size() > 0) {
            pageable = PageRequest.of(page - 1, size, Sort.by(sorts));
        } else {
            pageable = PageRequest.of(page - 1, size, Sort.unsorted());
        }

        Query countQuery = creatQuery(countSql, parameters);
        int count = Integer.parseInt(countQuery.getSingleResult().toString());

        //取查询结果集
        if (count >= 0) {
            List<T> content;
            if (clazz != null) {
                content = findAll(sql, clazz, (page - 1) * size, size, parameters);
            } else {
                Query query = creatQuery(sql, parameters);
                query.setFirstResult((page - 1) * size);
                query.setMaxResults(size);
                queryCoverMap(query);
                content = query.getResultList();
            }

            //字典转换
            dictionaryManager.cover(content);
            pageDate = new PageImpl<>(content, pageable, count);
        }

        return pageDate;
    }

    /**
     * 分页查询，自动生成条数汇总sql语句
     *
     * @param sql        原生sql，参数使用{Map的key值}形式占位
     * @param page       第几页
     * @param size       页大小
     * @param parameters Map类型参数集合
     * @return 分页Page类型结果
     */
    public Page<Map<String, Object>> findPageBySQL(String sql, int page, int size, Object parameters) {
        if (parameters != null && (canCastClass(parameters.getClass()) || parameters.getClass().isArray() || parameters instanceof Collection)) {
            return findPageBySQL(sql, SqlUtil.parserCountSQL(sql), page, size, null, parameters);
        }
        return findPageBySQL(SqlUtil.parserSQL(sql, parameters), SqlUtil.parserCountSQL(sql, parameters), page, size, null);
    }

    /**
     * 分页查询，自动生成条数汇总sql语句
     *
     * @param sql        原生sql，参数使用?形式占位
     * @param page       第几页
     * @param size       页大小
     * @param parameters 对象数组类型参数集合
     * @return 分页Page类型结果
     */
    public Page<Map<String, Object>> findPageBySQL(String sql, int page, int size, Object... parameters) {
        return findPageBySQL(sql, SqlUtil.parserCountSQL(sql), page, size, null, parameters);
    }

    /**
     * 分页查询，自动生成条数汇总sql语句
     *
     * @param sql        原生sql，参数使用{Map的key值}形式占位
     * @param page       第几页
     * @param size       页大小
     * @param parameters Map类型参数集合
     * @return 分页Page类型结果
     */
    public <T> Page<T> findPageBySQL(String sql, int page, int size, Class<T> clazz, Object... parameters) {
        return findPageBySQL(sql, SqlUtil.parserCountSQL(sql), page, size, clazz, parameters);
    }

    /**
     * 分页查询，自动生成条数汇总sql语句
     *
     * @param sql        原生sql，参数使用{Map的key值}形式占位
     * @param page       第几页
     * @param size       页大小
     * @param parameters Map类型参数集合
     * @return 分页Page类型结果
     */
    public <T> Page<T> findPageBySQL(String sql, int page, int size, Class<T> clazz, Object parameters) {
        if (parameters != null && (canCastClass(parameters.getClass()) || parameters.getClass().isArray() || parameters instanceof Collection)) {
            return findPageBySQL(sql, SqlUtil.parserCountSQL(sql), page, size, clazz, parameters);
        }
        return findPageBySQL(SqlUtil.parserSQL(sql, parameters), SqlUtil.parserCountSQL(sql, parameters), page, size, clazz);
    }

    /**
     * 创建普通查询的Query对象
     *
     * @param sql        sql语句
     * @param parameters 对象数组形式参数集合
     * @return 完成设置参数的Query对象
     */
    private Query creatQuery(String sql, Object... parameters) {
        Query query = getEntityManager().createNativeQuery(sql);
        if (parameters != null) {
            setParameter(query, parameters);
        }
        return query;
    }

    /**
     * 创建返回结果为clazz的Query对象
     *
     * @param sql        sql语句
     * @param clazz      返回结果类型
     * @param parameters 对象数组形式参数集合
     * @return 完成设置参数的Query对象
     */
    private Query creatClassQuery(String sql, Class<?> clazz, Object... parameters) {
        Query query = getEntityManager().createNativeQuery(sql, clazz);
        if (parameters != null) {
            setParameter(query, parameters);
        }
        return query;
    }

    /**
     * 设置查询参数
     *
     * @param query      query对象
     * @param parameters 参数集合
     */
    private void setParameter(Query query, Object... parameters) {
        if (parameters == null) {
            return;
        }
        for (int i = 0; i < parameters.length; i++) {
            query.setParameter(i + 1, parameters[i]);
        }
    }

    /**
     * 根据sql语句查询列表，结果类型为List<Map<String, Object>>
     *
     * @param sql        查询的sql语句，参数使用？占位
     * @param parameters 对象数组形式参数集合
     * @return 结果类型为List套Map的查询结果
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findAllBySQL(String sql, Object... parameters) {
        Query query = creatQuery(sql, parameters);
        queryCoverMap(query);
        List<Map<String, Object>> result = query.getResultList();
        if (result != null) {
            return result;
        }
        return new ArrayList<>(0);
    }

    /**
     * 根据sql语句查询列表，结果类型为List<Map<String, Object>>
     *
     * @param sql        查询sql语句，参数使用{Map的key值}形式占位
     * @param parameters Map类型参数集合
     * @return 结果类型为List套Map的查询结果
     */
    public List<Map<String, Object>> findAllBySQL(String sql, Object parameters) {
        if (parameters != null && (canCastClass(parameters.getClass()) || parameters.getClass().isArray() || parameters instanceof Collection)) {
            return findAllBySQL(sql, new Object[]{parameters});
        }
        return findAllBySQL(SqlUtil.parserSQL(sql, parameters));
    }

    public Map<String, Object> findOneToMap(String sql, Object parameters) {
        if (parameters != null && (canCastClass(parameters.getClass()) || parameters.getClass().isArray() || parameters instanceof Collection)) {
            return findOneToMap(sql, new Object[]{parameters});
        }
        return findOneToMap(SqlUtil.parserSQL(sql, parameters));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> findOneToMap(String sql, Object... parameters) {
        Query query = creatQuery(sql, parameters);
        queryCoverMap(query);
        return (Map<String, Object>) getSingleResult(query, sql);
    }

    private Object getSingleResult(Query query, String sql) {
        List<?> list = query.getResultList();
        if (list.size() == 0) {
            return null;
        } else if (list.size() > 1) {
            throw new NonUniqueResultException(String.format("Call to stored procedure [%s] returned multiple results", sql));
        } else {
            return list.get(0);
        }
    }

    /**
     * 查询结果预判为一个字段值
     *
     * @param sql        查询的sql语句，参数使用？占位
     * @param parameters 对象数组形式参数集合
     * @return 结果为一个查询字段值
     */
    public Object findParameter(String sql, Object... parameters) {
        Query query = creatQuery(sql, parameters);
        return getSingleResult(query, sql);
    }

    /**
     * 查询结果预判为一个字段值
     *
     * @param sql        查询sql语句，参数使用{Map的key值}形式占位
     * @param parameters Map类型参数集合
     * @return 结果为一个查询字段值
     */
    public Object findParameter(String sql, Object parameters) {
        if (parameters != null && (canCastClass(parameters.getClass()) || parameters.getClass().isArray() || parameters instanceof Collection)) {
            return findParameter(sql, new Object[]{parameters});
        }
        return findParameter(SqlUtil.parserSQL(sql, parameters));
    }

    /**
     * sql形式写操作
     *
     * @param sql        查询的sql语句，参数使用？占位
     * @param parameters 对象数组形式参数集合
     * @return 影响条数
     */
    public int updateBySQL(String sql, Object... parameters) {
        Query query = creatQuery(sql, parameters);
        return query.executeUpdate();
    }

    /**
     * sql形式写操作
     *
     * @param sql        查询sql语句，参数使用{Map的key值}形式占位
     * @param parameters Map类型参数集合
     * @return 影响条数
     */
    public int updateBySQL(String sql, Object parameters) {
        if (parameters != null && (canCastClass(parameters.getClass()) || parameters.getClass().isArray() || parameters instanceof Collection)) {
            return updateBySQL(sql, new Object[]{parameters});
        }
        return updateBySQL(SqlUtil.parserSQL(sql, parameters));
    }

    /**
     * 根据实体类型tableClass与主键值集合ids，查询实体列表
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param ids        主键值集合
     * @param <T>        目标表对应实体类型
     * @return 返回查询出的实体列表
     */
    public <T> List<T> findAllById(Class<T> tableClass, Iterable<?> ids) {
        List<T> result = getRepository(tableClass).findAllById((Iterable<Object>) ids);
        dictionaryManager.cover(result);
        return result;
    }

    /**
     * 根据实体类型tableClass与主键值集合ids，查询实体列表
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param ids        主键值集合，数组类型
     * @param <T>        目标表对应实体类型
     * @return 返回查询出的实体列表
     */
    public <T> List<T> findAllByArrayId(Class<T> tableClass, Object... ids) {
        List<T> result = getRepository(tableClass).findAllById(Arrays.asList(ids));
        dictionaryManager.cover(result);
        return result;
    }

    /**
     * 查询指定tableClass对应表的全表分页
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param page       第几页
     * @param size       页大小
     * @param <T>        目标表对应实体类型
     * @return 内容为实体的Page类型分页结果
     */
    public <T> Page<T> findAll(Class<T> tableClass, int page, int size) {
        validatePageInfo(page, size);
        Page<T> pageInfo = getRepository(tableClass).findAll(PageRequest.of(page - 1, size));
        dictionaryManager.cover(pageInfo.getContent());
        return pageInfo;
    }

    /**
     * 指定tableClass对应表的全表查询
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param <T>        目标表对应实体类型
     * @return 内容为实体的List类型结果集
     */
    public <T> List<T> findAll(Class<T> tableClass) {
        List<T> result = getRepository(tableClass).findAll();
        dictionaryManager.cover(result);
        return result;
    }

    /**
     * 指定tableClass对应表的全表查询,并排序
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param sort       排序信息
     * @param <T>        目标表对应实体类型
     * @return 内容为实体的List类型结果集
     */
    public <T> List<T> findAll(Class<T> tableClass, Sort sort) {
        List<T> result = getRepository(tableClass).findAll(sort);
        dictionaryManager.cover(result);
        return result;
    }

    /**
     * 查询指定tableClass对应表的总数
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @return 查询条数
     */
    public long count(Class<?> tableClass) {
        return getRepository(tableClass).count();
    }
}
