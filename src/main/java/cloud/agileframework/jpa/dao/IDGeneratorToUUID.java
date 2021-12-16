package cloud.agileframework.jpa.dao;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentityGenerator;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author 佟盟
 * 日期 2020-12-28 18:07
 * 描述 主键生成器
 * @version 1.0
 * @since 1.0
 */
public class IDGeneratorToUUID extends IdentityGenerator {
    @Override
    public Serializable generate(SharedSessionContractImplementor s, Object obj) {
        return UUID.randomUUID().toString();
    }
}
