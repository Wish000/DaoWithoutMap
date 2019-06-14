package com.wwx.entity;

import com.wwx.compiler.annotation.SQLDatePattern;
import com.wwx.compiler.annotation.Table;
import com.wwx.compiler.annotation.sql.ID;

import java.util.Date;

/**
 * 实体类，对应数据库里的MOVIE表
 */
@Table("MOVIE")
public class Movie {
    @ID(seq = "MOVIE_SEQ")
    private Integer id;
    private String name;
    private Long lasts;
//    @SQLDatePattern
    private Date startTime;

    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lasts=" + lasts +
                ", startTime=" + startTime +
                '}';
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getLasts() {
        return lasts;
    }

    public void setLasts(Long lasts) {
        this.lasts = lasts;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
}
