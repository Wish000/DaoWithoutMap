package com.wwx.dao;

import com.wwx.compiler.annotation.AutoSelect;
import com.wwx.compiler.annotation.AutoUpdate;
import com.wwx.compiler.annotation.DaoProxy;
import com.wwx.config.MySQLDataSourceConfig;
import com.wwx.entity.Movie;

import java.util.List;

@DaoProxy(output = MySQLDataSourceConfig.PACKAGE)
public interface MovieDao extends Dao<Movie> {

    /**
     * 通过注解自定义其他条件，会覆盖对象封装的条件
     *
     * @param movie 封装查询条件的字段
     * @return 结果集List
     */
    @AutoSelect(other = {"LASTS->LASTS = 90 OR LASTS = 80"})
    List<Movie> selectByLastsLimits(Movie movie);

    @AutoUpdate(compare = "NAME:LIKE")
    int updateByName(Movie movieS, Movie movieC);
}
