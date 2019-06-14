package com.wwx.compiler.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoSelect {
    /**
     * SQL中的or、in等其他条件，每个元素以“->”连接
     * @return 如[COL_1->COL_1 = 1 OR COL_1 = 3, COL_2->COL_2 IN ('3', '4', '5')]
     */
    String[] other() default {};

    /**
     * SQL中的比较条件，每个元素以“:”连接
     * @return 如[COL_1:<, COL_2:>=]
     */
    String[] compare() default {};

    /**
     * SQL中的ORDER BY 语句
     * @return 需要排序的字段
     */
    String[] orderBy() default {};

    /**
     * SQL中的ORDER BY DESC语句
     * @return 需要排序的字段
     */
    String[] orderDescBy() default {};
}
