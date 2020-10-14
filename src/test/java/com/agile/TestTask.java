package com.agile;

import cloud.agileframework.dictionary.DictionaryDataManagerProxy;
import cloud.agileframework.dictionary.MemoryDictionaryData;
import com.agile.mvc.entity.SysApiEntity;
import cloud.agileframework.jpa.dao.Dao;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
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

import java.util.List;
import java.util.Map;

import static cloud.agileframework.sql.SqlUtil.parserSQL;

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
    private DictionaryDataManagerProxy manager;


    @Before
    public void init() {
        manager.add(new MemoryDictionaryData("1", null, "状态", "type"));
        manager.add(new MemoryDictionaryData("2", "1", "对", "1"));
        manager.add(new MemoryDictionaryData("3", "1", "错", "0"));
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
        dao.delete(create());
    }

    @Test
    @Transactional
    public void update(){
        dao.update(create());
    }

    @Test
    public void query(){
        List<SysApiEntity> list = dao.findAll("select * from sys_api", SysApiEntity.class);
        logger.info(JSON.toJSONString(list,true));
    }

    @Test
    public void page(){
        Page<SysApiEntity> list = dao.findPageBySQL("select * from sys_api", 1, 10, SysApiEntity.class);
        logger.info(JSON.toJSONString(list,true));
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
    }

    @Test
    public void query2(){
        String sql = "select a.business_code AS 'businessCode',${typeId2:tudou,} " +
                "sd.depart_name AS 'deptName', " +
                "a.business_name AS 'businessName', a.foura_flag AS 'fouraFlag', ad.datasource AS 'dataSource', " +
                "ad.describes AS 'describe' from asset_base a " +
                "left join asset_data_source ad on ad.asset_id = a.asset_id " +
                "left join sys_department sd on sd.sys_depart_id = a.dept_id  " +
                "where a.del_flag = 0 and ad.del_flag = 0 and a.dept_id in ({deptName}) " +
                "and a.asset_id in ({businessName:'assss'}) and a.business_code LIKE concat('%',{h.0},'%') and a.foura_flag = '{fouraFlag}' order by a.update_time desc ";

        dao.findAllBySQL(sql,param);
    }

    @Test
    public void query3(){
        dao.findAll(SysApiEntity.class);
    }

    private SysApiEntity create(){
        SysApiEntity entity = new SysApiEntity();
        entity.setBusinessCode("asd");
        entity.setName("asd");
        entity.setSysApiId(1L);
        entity.setType(false);
        return entity;
    }
}
