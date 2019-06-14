package com.wwx.compiler.annotation;

import java.lang.annotation.*;

/**
 * 日期转换注解，放在bean的字段上，用于转换sql的日期格式
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SQLDatePattern {
    /**
     * 数据库日期转换函数
     * 常用的，MYSQL是STR_TO_DATE，是ORACLE是TO_DATE
     * @return 数据库日期转换函数名
     */
    String func() default "STR_TO_DATE";

    /**
     * 数据库日期转换模板（年月日时分秒）
     * 常用的，MYSQL是%Y-%m-%d %H:%i:%s，ORACLE是yyyy-MM-dd HH24:mi:ss
     * @return 数据库日期模板字符串
     */
    String pattern() default "%Y-%m-%d %H:%i:%s";
}
