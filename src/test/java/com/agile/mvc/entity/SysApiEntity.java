package com.agile.mvc.entity;

import cloud.agileframework.dictionary.annotation.Dictionary;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.Hibernate;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * 描述：[系统管理]目标任务表
 *
 * @author agile gennerator
 */
@ToString
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sys_api")
public class SysApiEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    private String name;
    private Boolean type;
    @Dictionary(fieldName = "type", isFull = true, dicCode = "type")
    private String typeText;
    private String businessName;
    private String businessCode;
    private String remarks;
    private Date now;

    @Column(name = "name", length = 65535)
    @Basic
    public String getName() {
        return name;
    }

    @Column(name = "type", length = 1)
    @Basic
    public Boolean getType() {
        return type;
    }

    @Column(name = "business_name", length = 40)
    @Basic
    public String getBusinessName() {
        return businessName;
    }

    @Basic
    @Column(name = "business_code", length = 20)
    public String getBusinessCode() {
        return businessCode;
    }

    @Basic
    @Column(name = "remarks", length = 255)
    public String getRemarks() {
        return remarks;
    }

    @Basic
    @Column(name = "now")
    public Date getNow() {
        return now;
    }

    @Transient
    public String getTypeText() {
        return typeText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        SysApiEntity that = (SysApiEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
