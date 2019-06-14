package com.wwx.compiler.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoInsert {
    /**
     * 主键自增长
     * 如果使用自增长ID，则会将bean中带有@ID注解的字段变成自增长的方式新增
     * @return 是否使用自增长的ID
     */
    boolean idSeq() default false;
}
