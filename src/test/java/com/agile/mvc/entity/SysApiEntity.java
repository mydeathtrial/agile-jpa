package com.agile.mvc.entity;

import cloud.agileframework.dictionary.annotation.Dictionary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Persistent;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * 描述：[系统管理]目标任务表
 *
 * @author agile gennerator
 */
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Entity
@Table(name = "sys_api")
public class SysApiEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long sysApiId;
    private String name;
    private Boolean type;
    @Dictionary(fieldName = "type",isFull = true,dicCode = "type")
    private String typeText;
    private String businessName;
    private String businessCode;
    private String remarks;

    @Column(name = "sys_api_id", nullable = false, length = 19)
    @Id
    public Long getSysApiId() {
        return sysApiId;
    }

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

    @Transient
    public String getTypeText() {
        return typeText;
    }
}
