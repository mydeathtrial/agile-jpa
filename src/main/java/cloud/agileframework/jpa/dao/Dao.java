package cloud.agileframework.jpa.dao;

import cloud.agileframework.common.util.clazz.ClassInfo;
import cloud.agileframework.common.util.clazz.ClassUtil;
import cloud.agileframework.common.util.clazz.TypeReference;
import cloud.agileframework.common.util.collection.SortInfo;
import cloud.agileframework.common.util.object.ObjectUtil;
import cloud.agileframework.common.util.string.StringUtil;
import cloud.agileframework.data.common.dao.BaseDao;
import cloud.agileframework.data.common.dao.ColumnName;
import cloud.agileframework.data.common.dictionary.DataExtendManager;
import cloud.agileframework.sql.SqlUtil;
import com.alibaba.druid.DbType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Type;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author 佟盟 on 2017/11/15
 */
public class Dao extends HibernateDaoSupport implements BaseDao {
    private static final Map<Class<?>, SimpleJpaRepository> REPOSITORY_CACHE = new HashMap<>();
    private static final Map<Class<?>, JpaEntityInformation<?, ?>> ENTITYINFORMATION_CACHE = new HashMap<>();
    private final DbType dbType;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private DataExtendManager dictionaryManager;

    public Dao(DbType dbType) {
        this.dbType = dbType;
    }

    private static boolean canCastClass(Class<?> clazz) {
        if (ClassUtil.isWrapOrPrimitive(clazz)) {
            return true;
        }
        return String.class == clazz
                || BigDecimal.class == clazz
                || Date.class == clazz
                || BigInteger.class == clazz
                || StringBuilder.class == clazz
                || StringBuffer.class == clazz
                || Timestamp.class == clazz
                || Time.class == clazz;
    }

    private static <T> void sort(List<T> list, String property) {
        list.sort((o1, o2) -> {
            try {
                if (Map.class.isAssignableFrom(o1.getClass())) {
                    return String.valueOf(((Map) o1).get(property)).compareTo(String.valueOf(((Map) o2).get(property)));
                } else {
                    Class<?> clazz = o1.getClass();
                    Field field = ClassInfo.getCache(clazz).getField(property);
                    return String.valueOf(field.get(o1)).compareTo(String.valueOf(field.get(o2)));
                }
            } catch (IllegalAccessException var5) {
                var5.printStackTrace();
                return 0;
            }
        });
    }

    private static <T> void sort(List<T> list, SortInfo... sortInfos) {
        if (sortInfos == null || sortInfos.length == 0) {
            return;
        }
        list.sort((o1, o2) -> {
            try {
                for (SortInfo sort : sortInfos) {
                    final String property = sort.getProperty();
                    int v = compare(o1, o2, property, sort.isSort());
                    if (v != 0) {
                        break;
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return 0;
        });
    }

    private static <T> int compare(T o1, T o2, String property, boolean sort) throws IllegalAccessException {
        int result = 0;
        if (Map.class.isAssignableFrom(o1.getClass())) {
            result = String.valueOf(((Map) o1).get(property)).compareTo(String.valueOf(((Map) o2).get(property)));
        } else {
            Class<?> clazz = o1.getClass();
            Field field = ClassInfo.getCache(clazz).getField(property);
            result = String.valueOf(field.get(o1)).compareTo(String.valueOf(field.get(o2)));
        }
        return sort ? result : -result;
    }

    @Override
    public DataExtendManager dictionaryManager() {
        return dictionaryManager;
    }

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
            JpaEntityInformation<T, ?> entityInformation = JpaEntityInformationSupport.getEntityInformation(tableClass, getEntityManager());
            repository = new SimpleJpaRepository<>(entityInformation, getEntityManager());
            REPOSITORY_CACHE.put(tableClass, repository);
            ENTITYINFORMATION_CACHE.put(tableClass, entityInformation);
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
    @Override
    public <T> void save(T o) {
        getEntityManager().persist(o);
    }

    /**
     * 获取数据库连接
     *
     * @return Connection
     */
    public Connection getConnection() {
        return getEntityManager().unwrap(SessionImplementor.class).connection();
    }

    @Override
    public <T> boolean contains(T o) {
        return getEntityManager().contains(o);
    }

    /**
     * 保存或更新
     *
     * @param o   已经有的对象更新，不存在的保存
     * @param <T> 泛型
     * @return 被跟踪对象
     */
    @Override
    public <T> T saveOrUpdate(T o) {
        T e;
        boolean save = true;
        Object id = getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(o);
        if (id != null) {
            Object old = getEntityManager().find(o.getClass(), id);
            save = old == null;
        }

        if (save) {
            SimpleJpaRepository<T, Object> repository = (SimpleJpaRepository<T, Object>) getRepository(o.getClass());
            e = repository.save(o);
        } else {
            e = getEntityManager().merge(o);
        }

        dictionaryManager.cover(e);
        return e;
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
    @Override
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
     * @param o   ORM对象，瞬态对象时不会被跟踪
     * @param <T> 表对应的实体类型
     * @return 是否更新成功
     */
    @Override
    public <T> boolean update(T o) {
        Object id = getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(o);
        Object old = getEntityManager().find(o.getClass(), id);
        if (old == null) {
            return false;
        }
        T e = getEntityManager().merge(o);
        dictionaryManager.cover(e);
        return true;
    }

    /**
     * 更新或新增非空字段，空字段不进行更新
     *
     * @param o   表映射实体类型的对象
     * @param <T> 表映射实体类型的对象
     * @return 返回更新后的数据
     * @throws IdentifierGenerationException 异常
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T updateOfNotNull(T o) {
        Object id = getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(o);
        T old = (T) getEntityManager().find(o.getClass(), id);
        ObjectUtil.copyProperties(o, old, ObjectUtil.Compare.DIFF_SOURCE_NOT_NULL);
        T e = getEntityManager().merge(old);
        dictionaryManager.cover(e);
        return e;
    }

    /**
     * 删除全部(一次性删除)
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param <T>        查询的目标表对应实体类型
     */
    @Override
    public <T> void deleteAllInBatch(Class<T> tableClass) {
        getRepository(tableClass).deleteAllInBatch();
    }

    /**
     * 根据主键，查询单条
     *
     * @param clazz 查询的目标表对应实体类型，Entity
     * @param id    主键
     * @param <T>   查询的目标表对应实体类型
     * @return clazz类型对象
     */
    @Override
    public <T> T findOne(Class<T> clazz, Object id) {
        T e = getEntityManager().find(clazz, toIdType(clazz, id));
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
    @Override
    @SuppressWarnings("unchecked")
    public <T> T findOne(T object) {
        Example<T> example = Example.of(object);
        Class<T> clazz = (Class<T>) object.getClass();
        T e = this.getRepository(clazz).findOne(example).orElse(null);
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
    public <T> T findOne(T object, ExampleMatcher matcher) {
        Class<T> clazz = (Class<T>) object.getClass();
        T e = this.getRepository(clazz).findOne(Example.of(object, matcher)).orElse(null);
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
        Query query = creatQuery(false, sql, parameters);

        if (!canCastClass(clazz)) {
            queryCoverMap(query);
            Map<String, Object> o = (Map<String, Object>) getSingleResult(query, sql);
            if (Map.class.isAssignableFrom(clazz)) {
                return (T) o;
            }
            T e = ObjectUtil.to(o, new TypeReference<>(clazz));
            dictionaryManager.cover(e);
            return e;
        } else {
            Object o = getSingleResult(query, sql);
            return ObjectUtil.to(o, new TypeReference<>(clazz));
        }
    }

    /**
     * 按照例子查询多条
     *
     * @param <T>    查询的表的映射实体类型
     * @param object 例子对象
     * @return 查询结果数据集合
     */
    public <T> List<T> findAll(T object) {
        return findAll(object, Sort.unsorted());
    }

    public <T> List<T> findAll(T object, Sort sort) {
        return findAll(object, ExampleMatcher.matching(), sort);
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
    public <T> List<T> findAll(T object, ExampleMatcher matcher, Sort sort) {
        Example<T> example = Example.of(object, matcher);
        Class<T> clazz = (Class<T>) object.getClass();
        List<T> result = this.getRepository(clazz).findAll(example, sort);
        dictionaryManager.cover(result);
        return result;
    }

//    @SuppressWarnings("unchecked")
//    public <T> List<T> findAll(T object, ExampleMatcher matcher, Sort sort) {
//        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
//        CriteriaQuery<T> query = (CriteriaQuery<T>) criteriaBuilder.createQuery(object.getClass());
//        Root<T> root = (Root<T>) query.from(object.getClass());
//        query.select(criteriaBuilder.count(root.get("id")));
//        Predicate predicate = criteriaBuilder.equal(root.get("id"), 1);
//        query.where(predicate);
//        Long singleResult = entityManager.createQuery(query).getSingleResult();
//
//    }


    /**
     * 按照例子查询多条分页
     *
     * @param <T>    查询的表的映射实体类型
     * @param object 例子对象
     * @param page   第几页
     * @param size   每页条数
     * @return 分页对象
     */
    public <T> Page<T> page(T object, int page, int size) {
        return page(object, page, size, Sort.unsorted());
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
    public <T> Page<T> page(T object, ExampleMatcher matcher, int page, int size) {
        return page(object, matcher, PageRequest.of(page - 1, size, Sort.unsorted()));
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
    public <T> Page<T> page(T object, int page, int size, Sort sort) {
        return page(object, ExampleMatcher.matching(), PageRequest.of(page - 1, size, sort));
    }

    @SuppressWarnings("unchecked")
    public <T> Page<T> page(T object, ExampleMatcher matcher, PageRequest pageRequest) {
        if (object instanceof Class) {
            return this.getRepository((Class<T>) object).findAll(pageRequest);
        }
        parseDo(object);
        Example<T> example = Example.of(object, matcher);
        Class<T> clazz = (Class<T>) object.getClass();
        Page<T> page = this.getRepository(clazz).findAll(example, pageRequest);
        dictionaryManager.cover(page.getContent());
        return page;
    }

    public <T> Page<T> page(T object, PageRequest pageRequest) {
        return page(object, ExampleMatcher.matching(), pageRequest);
    }

    private <T> void parseDo(T object) {
        Class<?> clazz = object.getClass();
        ClassInfo<?> classInfo = ClassInfo.getCache(clazz);
        classInfo.getAllField().forEach(a -> {
            try {
                if (a.getType() == String.class && StringUtil.isBlank((String) (a.get(object)))) {
                    a.setAccessible(true);
                    a.set(object, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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
    public <T> Page<T> pageByClass(Class<T> tableClass, int page, int size) {
        return pageByClass(tableClass, PageRequest.of(page - 1, size));
    }

    /**
     * 查询指定tableClass对应表的全表分页
     *
     * @param tableClass  查询的目标表对应实体类型，Entity
     * @param pageRequest 分页信息
     * @param <T>         目标表对应实体类型
     * @return 内容为实体的Page类型分页结果
     */
    public <T> Page<T> pageByClass(Class<T> tableClass, PageRequest pageRequest) {
        Page<T> pageInfo = getRepository(tableClass).findAll(pageRequest);
        dictionaryManager.cover(pageInfo.getContent());
        return pageInfo;
    }

    public <T> Page<T> pageBySQL(String sql, int page, int size, Class<T> clazz, Object... parameters) {
        return pageBySQL(sql, PageRequest.of(page - 1, size, Sort.unsorted()), clazz, parameters);
    }

    /**
     * 分页查询
     *
     * @param sql        查询的sql语句
     * @param pageable   分页信息
     * @param parameters 对象数组类型的参数集合
     * @return Page类型的查询结果
     */
    @SuppressWarnings("unchecked")
    public <T> Page<T> pageBySQL(String sql, PageRequest pageable, Class<T> clazz, Object... parameters) {
        Page<T> pageDate = Page.empty();

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();

        Query countQuery = creatQuery(true, sql, parameters);
        int count = Integer.parseInt(countQuery.getSingleResult().toString());

        //取查询结果集
        if (count >= 0) {
            List<T> content;
            if (clazz != null) {
                content = findBySQL(sql, clazz, page * size, size, parameters);
            } else {
                Query query = creatQuery(false, sql, parameters);
                query.setFirstResult(page * size);
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
     * 指定tableClass对应表的全表查询
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param <T>        目标表对应实体类型
     * @return 内容为实体的List类型结果集
     */
    public <T> List<T> findAllByClass(Class<T> tableClass) {
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
    public <T> List<T> findAllByClass(Class<T> tableClass, Sort sort) {
        List<T> result = getRepository(tableClass).findAll(sort);
        dictionaryManager.cover(result);
        return result;
    }

    public <T> List<T> findBySQL(String sql, Class<T> clazz, Object... parameters) {
        return findBySQL(sql, clazz, null, null, parameters);
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
    public <T> List<T> findBySQL(String sql, Class<T> clazz, Integer firstResult, Integer maxResults, Object... parameters) {
        Query query = creatQuery(false, sql, parameters);
        queryCoverMap(query);
        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        List<Map<String, Object>> list = query.getResultList();

        if (list != null && !list.isEmpty()) {
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
     * 根据sql语句查询列表，结果类型为List<Map<String, Object>>
     *
     * @param sql        查询sql语句，参数使用{Map的key值}形式占位
     * @param parameters Map类型参数集合
     * @return 结果类型为List套Map的查询结果
     */
    public List<Map<String, Object>> findBySQL(String sql, Object... parameters) {
        Query query = creatQuery(false, sql, parameters);
        queryCoverMap(query);
        List<Map<String, Object>> result = query.getResultList();
        if (result != null) {
            return result;
        }
        return new ArrayList<>(0);
    }

    /**
     * 根据表映射的实体类型与主键值集合，创建一个只包含主键值的空的对象集合
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @param ids        主键数组
     * @param <T>        查询的目标表对应实体类型
     * @param <ID>       查询的目标表对应实体主键类型
     * @return 结果集
     * @throws IdentifierGenerationException tableClass实体类型中没有找到@ID的注解，识别成主键字段
     */
    private <T, ID> List<T> createObjectList(Class<T> tableClass, ID[] ids) throws IdentifierGenerationException {
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

    /**
     * 获取ORM中的主键字段
     *
     * @param clazz 查询的目标表对应实体类型，Entity
     * @return 主键属性
     * @throws IdentifierGenerationException tableClass实体类型中没有找到@ID的注解，识别成主键字段
     */
    public Field getIdField(Class<?> clazz) {
        final EntityType<?> entityInfo = getEntityManager().getMetamodel().entity(clazz);
        return ClassInfo.getCache(clazz).getField(entityInfo.getId(entityInfo.getIdType().getJavaType()).getName());
    }

    @SneakyThrows
    public Object getId(Object o) {
        return getIdField(o.getClass()).get(o);
    }

    @SneakyThrows
    public void setId(Object o, Object id) {
        final Field idField = getIdField(o.getClass());
        idField.setAccessible(true);
        idField.set(o, id);
    }

    /**
     * 根据ORM类型取主键类型
     *
     * @param clazz 主键java类型
     * @return 主键java类型
     */
    public Class<?> getIdType(Class<?> clazz) {
        return getEntityManager().getMetamodel().entity(clazz).getIdType().getJavaType();
    }

    /**
     * 把id转换为clazz实体的主键类型
     *
     * @param clazz 实体类型
     * @param id    主键
     * @return 转换后的主键
     */
    public Object toIdType(Class<?> clazz, Object id) {
        return ObjectUtil.to(id, new TypeReference<>(getIdType(clazz)));
    }

    @Override
    public <T> List<ColumnName> toColumnNames(Class<T> tableClass) {
        return ClassUtil.getAllEntityAnnotation(tableClass, Column.class).stream().map(f -> {
            ColumnName columnName = new ColumnName();

            Column annotation = f.getAnnotation();
            String name = annotation.name();

            if (!StringUtil.isEmpty(name)) {
                columnName.setName(name);
            }

            columnName.setMember(f.getMember());

            //设置主键
            Id id = ClassUtil.getFieldAnnotation(tableClass, columnName.getName(), Id.class);
            columnName.setPrimaryKey(id != null);
            return columnName;
        }).collect(Collectors.toList());
    }

    @Override
    public <T> String toTableName(Class<T> tableClass) {
        Table document = tableClass.getAnnotation(Table.class);
        return document.name();
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
     * 创建普通查询的Query对象
     *
     * @param sql        sql语句
     * @param parameters 对象数组形式参数集合
     * @return 完成设置参数的Query对象
     */
    private Query creatQuery(boolean isCount, String sql, Object... parameters) {
        Query query;
        if (parameters == null) {
            return getEntityManager().createNativeQuery(sql);
        }

        if (parameters.length == 1) {
            Object p = parameters[0];
            if (canCastClass(p.getClass()) || p instanceof Collection) {
                query = getEntityManager().createNativeQuery(sql);
                query.setParameter(0, p);
            } else {
                Map<String, Object> map = Maps.newHashMap();

                if (isCount) {
                    sql = SqlUtil.parserCountSQLByType(dbType, sql, p, map);
                } else {
                    sql = SqlUtil.parserSQLByType(dbType, sql, p, map);
                }
                query = getEntityManager().createNativeQuery(sql);
                try {
                    for (Map.Entry<String, Object> e : map.entrySet()) {
                        query.setParameter(Integer.parseInt(e.getKey()), e.getValue());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(sql, e);
                }


            }
        } else {
            if (isCount) {
                query = getEntityManager().createNativeQuery(SqlUtil.parserCountSQL(sql));
            } else {
                query = getEntityManager().createNativeQuery(sql);
            }

            for (int i = 0; i < parameters.length; i++) {
                query.setParameter(i, parameters[i]);
            }
        }
        return query;
    }

    private Object getSingleResult(Query query, String sql) {
        List<?> list = query.getResultList();
        if (list.isEmpty()) {
            return null;
        } else if (list.size() > 1) {
            throw new NonUniqueResultException(String.format("Call to stored procedure [%s] returned multiple results", sql));
        } else {
            return list.get(0);
        }
    }

    /**
     * sql形式写操作
     *
     * @param sql        查询的sql语句，参数使用？占位
     * @param parameters 对象数组形式参数集合
     * @return 影响条数
     */
    public int updateBySQL(String sql, Object... parameters) {
        Query query = creatQuery(false, sql, parameters);
        return query.executeUpdate();
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
     * 查询指定tableClass对应表的总数
     *
     * @param tableClass 查询的目标表对应实体类型，Entity
     * @return 查询条数
     */
    public long count(Class<?> tableClass) {
        return getRepository(tableClass).count();
    }

    /**
     * 内存分页
     *
     * @param list      集合
     * @param pageNum   页号
     * @param pageSize  页大小
     * @param sortInfos 排序字段
     * @param <T>       泛型
     * @return 分页
     */
    public <T> Page<T> memoryPage(List<T> list, int pageNum, int pageSize, SortInfo... sortInfos) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize);
        PageImpl<T> page = new PageImpl<>(Lists.newArrayList(), pageRequest, 0);

        sort(list, sortInfos);

        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = pageNum * pageSize;

        if (list.isEmpty() || fromIndex > list.size()) {
            return page;
        } else if (toIndex < list.size()) {
            page = new PageImpl<>(list.subList(fromIndex, toIndex), pageRequest, list.size());
        } else {
            page = new PageImpl<>(list.subList(fromIndex, list.size()), pageRequest, list.size());
        }
        return page;
    }

    /**
     * 批量插入
     *
     * @param list      要保存的数据集合
     * @param batchSize 多少条执行一次插入
     */
    public <T> void batchInsert(List<T> list, int batchSize) {
        try {
            if (batchSize <= 0) {
                for (Object o : list) {
                    getEntityManager().persist(o);
                }
                getEntityManager().flush();
                getEntityManager().clear();
            }
            for (int i = 0; i < list.size(); i++) {
                getEntityManager().persist(list.get(i));
                if (i % batchSize == 0) {//一次一百条插入
                    getEntityManager().flush();
                    getEntityManager().clear();
                }
            }
            logger.debug("save to DB success,list is " + list);
        } catch (Exception e) {
            logger.error("batch insert data fail.");
            e.printStackTrace();
        }
    }

    /**
     * 批量更新
     *
     * @param list      要更新的数据集合
     * @param batchSize 多少条执行一次更新
     */
    public <T> void batchUpdate(List<T> list, int batchSize) {
        try {
            if (batchSize <= 0) {
                for (Object o : list) {
                    getEntityManager().merge(o);
                }
                getEntityManager().flush();
                getEntityManager().clear();
            }
            for (int i = 0; i < list.size(); i++) {
                getEntityManager().merge(list.get(i));
                if (i % batchSize == 0) {
                    getEntityManager().flush();
                    getEntityManager().clear();
                }
            }
            logger.info("update data success,list is {}" + list);
        } catch (Exception e) {
            logger.error("batch update data fail.");
            e.printStackTrace();
        }
    }

    /**
     * 批量删除
     *
     * @param list      要删除的数据集合
     * @param batchSize 多少条执行一次删除
     */
    public <T> void batchDelete(List<T> list, int batchSize) {
        try {
            if (batchSize <= 0) {
                for (Object o : list) {
                    getEntityManager().remove(o);
                }
                getEntityManager().flush();
                getEntityManager().clear();
            }
            for (int i = 0; i < list.size(); i++) {
                getEntityManager().remove(list.get(i));
                if (i % batchSize == 0) {
                    getEntityManager().flush();
                    getEntityManager().clear();
                }
            }
            logger.info("delete data success,list is {}" + list);
        } catch (Exception e) {
            logger.error("batch delete data fail.");
            e.printStackTrace();
        }
    }

    public Class<?> getEntityType(String model) {
        Optional<EntityType<?>> entityType = getEntityManager().getEntityManagerFactory()
                .getMetamodel()
                .getEntities()
                .stream()
                .filter(n -> n.getName().equalsIgnoreCase(StringUtil.toUpperName(model)))
                .findFirst();

        return entityType.<Class<?>>map(Type::getJavaType).orElse(null);
    }

    @Override
    public <T> void delete(T entity) {
        updateBySQL(toDeleteSql(entity, dbType));
    }
}
