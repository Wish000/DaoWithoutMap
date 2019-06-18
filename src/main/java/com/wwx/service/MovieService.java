package com.wwx.service;

import com.wwx.dao.MovieDao;
import com.wwx.entity.Movie;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class MovieService {

    @Resource
    private MovieDao movieDao;

    /**
     * 查询符合条件的所有电影
     *
     * @param movie 封装查询条件
     * @return 电影结果集
     */
    public List<Movie> queryMovies(Movie movie) {
        List<Movie> movies = movieDao.SELECT(movie);
        return movies;
    }

    public int addMovie(Movie movie) {
        return movieDao.INSERT(movie);
    }

    public int changeMovie(Movie set, Movie cons) {
        return movieDao.updateByName(set, cons);
    }

    public int removeMovie(Movie movie) {
        return movieDao.DELETE(movie);
    }
}
