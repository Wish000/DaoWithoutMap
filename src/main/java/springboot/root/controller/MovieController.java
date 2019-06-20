package springboot.root.controller;

import springboot.root.entity.Movie;
import springboot.root.service.MovieService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * A sample entry
 */
@RestController
@RequestMapping("")
public class MovieController {
    @Resource
    private MovieService movieService;

    @RequestMapping("list/{lasts}")
    public String list(Movie movie) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = sdf.parse("2018-09-05 10:01:36");
        movie.setStartTime(date);
        List<Movie> movies = movieService.queryMovies(movie);
        StringBuilder sb = new StringBuilder();
        movies.forEach(sb::append);
        return sb.toString();
    }

    @RequestMapping("add")
    public int add() {
        Movie movie = new Movie();
        movie.setId(90);
        movie.setStartTime(new Date());
        movie.setLasts(100L);
        movie.setName("西红柿");
        return movieService.addMovie(movie);
    }

    @RequestMapping("change")
    public int change() throws ParseException {
        Movie movieSets = new Movie();
        movieSets.setStartTime(new Date());
        movieSets.setLasts(105L);
        Movie movieConditions = new Movie();
        movieConditions.setName("teen");
        return movieService.changeMovie(movieSets, movieConditions);
    }

    @RequestMapping("remove")
    public int remove() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Movie movie = new Movie();
        movie.setStartTime(sdf.parse("2018-09-03 17:31:03"));
        return movieService.removeMovie(movie);
    }
}
