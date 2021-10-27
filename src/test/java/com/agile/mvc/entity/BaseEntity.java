package com.agile.mvc.entity;

import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author 佟盟
 * 日期 2021-01-28 17:58
 * 描述 TODO
 * @version 1.0
 * @since 1.0
 */
@MappedSuperclass
@Setter
@SuperBuilder
public class BaseEntity {
    private String id;

    public BaseEntity() {

    }

    @Column(name = "sys_api_id", nullable = false, length = 19)
    @Id
    public String getId() {
        return id;
    }
}
