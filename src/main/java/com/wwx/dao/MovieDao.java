package com.wwx.dao;

import com.wwx.compiler.annotation.AutoSelect;
import com.wwx.compiler.annotation.AutoUpdate;
import com.wwx.compiler.annotation.DaoProxy;
import com.wwx.config.MySQLDataSourceConfig;
import com.wwx.entity.Movie;

import java.util.List;

@DaoProxy(output = MySQLDataSourceConfig.PACKAGE)
public interface MovieDao extends Dao<Movie> {

    @AutoSelect(other = {"LASTS->LASTS = 90 OR LASTS = 80"})
    List<Movie> selectByLastsLimits(Movie movie);

    @AutoUpdate(compare = "NAME:LIKE")
    int updateByName(Movie movieS, Movie movieC);
}
