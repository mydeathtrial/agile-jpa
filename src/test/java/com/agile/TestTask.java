package com.agile;

import cloud.agileframework.dictionary.DictionaryDataBase;
import cloud.agileframework.dictionary.DictionaryDataManager;
import cloud.agileframework.dictionary.util.TranslateException;
import cloud.agileframework.jpa.dao.Dao;
import cloud.agileframework.spring.util.IdUtil;
import com.agile.mvc.entity.SysApiEntity;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author 佟盟
 * 日期 2020/7/14 18:03
 * 描述 TODO
 * @version 1.0
 * @since 1.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
public class TestTask {
    private final Logger logger = LoggerFactory.getLogger(TestTask.class);

    @Autowired
    private DictionaryDataManager manager;


    @Before
    public void init() {
        manager.add(new DictionaryDataBase("1", null, "状态", "type"));
        manager.add(new DictionaryDataBase("2", "1", "对", "1"));
        manager.add(new DictionaryDataBase("3", "1", "错", "0"));
    }

    @Autowired
    private Dao dao;

    @Test
    @Transactional
    public void add() {
        dao.save(create());
    }

    @Test
    @Transactional
    public void delete() {
        add();
        dao.delete(SysApiEntity.builder().name("asd").build());
        List<?> all = dao.findAll(SysApiEntity.builder().name("asd").build());
        Assert.assertTrue(all.isEmpty());
    }

    @Test
    @Transactional
    public void update() {
        dao.update(create());
    }

    @Test
    public void query() {
        List<SysApiEntity> list = dao.findBySQL("select * from sys_api", SysApiEntity.class);
        logger.info(JSON.toJSONString(list, true));
    }

    @Test
    public void page() {
        Page<SysApiEntity> list = dao.pageBySQL("select * from sys_api", 1, 10, SysApiEntity.class);
        logger.info(JSON.toJSONString(list, true));
    }

    @Test
    public void findOne() {
//        SysApiEntity s = dao.findOne("select 1 as name,1 as type", SysApiEntity.class);
//        System.out.println(s);

        List<String> s = dao.findBySQL("select '{\"nickname\": \"{p}\", \"avatar\": \"avatar_url\", \"tags\": [\"python\", \"golang\", \"db\"]}'::jsonb->>'nickname' as nickname", String.class, new HashMap<String, Object>() {{
            put("p", "sd");
        }});
        System.out.println(s);
    }

    private static final Map<String, Object> param;

    static {
        param = Maps.newHashMap();
        param.put("column", new String[]{"a", "b"});
        param.put("a", "abc");
        param.put("b", "b");
        param.put("c", new String[]{"c1", "c2"});
        param.put("d", "d");
        param.put("e", "e");
        param.put("f", new String[]{"f1", "f2"});
        param.put("g", "g");
        param.put("h", new String[]{"h1", "h2"});
        param.put("j", new String[]{"j1", "j2"});
        param.put("ga", "ga'''");
        param.put("gb", "gb");
        param.put("order", "ad desc");

        param.put("format", "%Y/%m/%d");
        param.put("typeId", "typeId,typeId2,");
        param.put("time", "123123123");

        param.put("startTime", "11111111");
        param.put("endTime", "22222222");
        param.put("businessName", new String[]{"j1", "j2"});
        param.put("businessCode", "33");
        param.put("deptName", new String[]{"deptName1", "deptName2"});
    }

    @SneakyThrows
    @Test
    public void query2() {
        String sql = "select a.business_code AS 'businessCode',${typeId2:business_code,} " +
                "sd.depart_name AS 'deptName', " +
                "a.business_name AS 'businessName', a.foura_flag AS 'fouraFlag', ad.datasource AS 'dataSource', " +
                "ad.describes AS 'describe' from asset_base a " +
                "left join asset_data_source ad on ad.asset_id = a.asset_id " +
                "left join sys_department sd on sd.sys_depart_id = a.dept_id  " +
                "where a.del_flag = 0 and ad.del_flag = 0 and a.dept_id in ({deptName}) " +
                "and a.asset_id in ({businessName:'assss'}) and a.business_code LIKE {h.0} and a.foura_flag = '{fouraFlag}' order by a.update_time desc ";

        Method creatQuery = Dao.class.getDeclaredMethod("creatQuery", boolean.class, String.class, Object[].class);
        creatQuery.setAccessible(true);
        creatQuery.invoke(dao, false, sql, new Object[]{param});
    }

    @Test
    public void query3() {
        dao.findAllByClass(SysApiEntity.class);
    }

    private SysApiEntity create() {
        SysApiEntity entity = new SysApiEntity();
        entity.setBusinessCode("asd");
        entity.setName("asd");
        entity.setId(IdUtil.generatorIdToString());
        entity.setType(false);
        entity.setNow(new Date());
        return entity;
    }

//    @Test
//    @Transactional
//    public void query4() {
//        IntStream.range(0, 100).forEach(a -> {
//            SysApiEntity entity = new SysApiEntity();
//            entity.setBusinessCode("asd" + a);
//            entity.setName("asd" + a);
//            entity.setId(IdUtil.generatorIdToString());
//            entity.setType(false);
//            dao.save(entity);
//        });
////        QBaseEntity baseEntity = QBaseEntity.baseEntity;
////        QSysApiEntity sysApiEntity = QSysApiEntity.sysApiEntity;
//        JPAQueryFactory queryFactory = new JPAQueryFactory(dao.getEntityManager());
////        queryFactory.selectFrom(sysApiEntity)
////                .where(
////                        sysApiEntity.id.like("%1")
////                ).fetch();
//
//        MyEntityPathBase<SysApiEntity> e = new MyEntityPathBase<>(SysApiEntity.class, "a");
//        List<Object> one = queryFactory.select(new PathBuilder<>(Object.class, "a").get("name")).from(e).where(e.createString("id").eq("1")).fetch();
//
//        final SQLQuery<Object> sqlQuery = new SQLQuery<>(MySQLTemplates.DEFAULT);
//        PathBuilder<Object> pathBuilder = new PathBuilder<>(Object.class, "person");
//        sqlQuery.select(pathBuilder.get("name"))
//                .from(pathBuilder.getRoot())
//                .where(pathBuilder.get("idnumber")
//                        .in("a", "b", "c")).offset(5).limit(10);
//        final SQLBindings bindings = sqlQuery.getSQL();
//        System.out.println(bindings.getSQL());
//        System.out.println(bindings.getNullFriendlyBindings());
//    }

    //    @Before
    public void before() {
        dao.updateBySQL("drop table if exists SYS_API;" + "create table SYS_API\n" +
                "(\n" +
                "    SYS_API_ID    VARCHAR2 not null,\n" +
                "    NAME          VARCHAR2,\n" +
                "    BUSINESS_NAME VARCHAR2,\n" +
                "    BUSINESS_CODE VARCHAR2,\n" +
                "    REMARKS       TEXT,\n" +
                "    TYPE          VARCHAR,\n" +
                "    NOW          DATETIME,\n" +
                "    constraint SYS_API_PK\n" +
                "        primary key (SYS_API_ID)\n" +
                ")");
    }

    @Test
    public void query5() {
        dao.findAll(SysApiEntity.builder().name("asd").build());
    }
}
