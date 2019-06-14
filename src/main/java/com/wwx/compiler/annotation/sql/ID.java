package com.wwx.compiler.annotation.sql;

import java.lang.annotation.*;

/**
 * 指定bean中的序列自增长ID字段
 * 如果字段是ID但不需要序列的，不需要改注解
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ID {
    /**
     * 数据库序列
     * @return 序列名
     */
    String seq();
}
