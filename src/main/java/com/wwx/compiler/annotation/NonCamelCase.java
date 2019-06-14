package com.wwx.compiler.annotation;

import java.lang.annotation.*;

/**
 * Created by wuweixi on 2018/8/22.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface NonCamelCase {
}
