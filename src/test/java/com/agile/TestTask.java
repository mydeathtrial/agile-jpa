package com.agile;

import com.agile.mvc.entity.SysApiEntity;
import com.agile.common.mvc.model.dao.Dao;
import com.alibaba.fastjson.JSON;
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
        Page<SysApiEntity> list = dao.findPageBySQL("select * from sys_api", 1, 10, SysApiEntity.class, null);
        logger.info(JSON.toJSONString(list,true));
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
