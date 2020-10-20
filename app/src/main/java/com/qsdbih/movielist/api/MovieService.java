package com.qsdbih.movielist.api;

import com.qsdbih.movielist.database.Movie;
import com.qsdbih.movielist.objects.PopularMovies;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MovieService {

    @GET("movie/popular")
    Call<PopularMovies> getPopularMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("page") int page
    );

    @GET("movie/{id}")
    Call<Movie> getMovie(
            @Path("id") String id,
            @Query("api_key") String apiKey,
            @Query("language") String language
    );
}