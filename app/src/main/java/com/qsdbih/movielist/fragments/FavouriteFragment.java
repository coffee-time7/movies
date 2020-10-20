package com.qsdbih.movielist.fragments;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.qsdbih.movielist.adapters.MovieIoListener;
import com.qsdbih.movielist.database.FavouriteMovie;
import com.qsdbih.movielist.database.FavouriteMovie_;
import com.qsdbih.movielist.database.Movie;
import com.qsdbih.movielist.database.ObjectBox;
import com.qsdbih.movielist.R;
import com.qsdbih.movielist.adapters.MoviesRecyclerViewAdapter;
import com.qsdbih.movielist.api.MovieApi;
import com.qsdbih.movielist.api.MovieService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.objectbox.Box;
import io.objectbox.query.Query;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavouriteFragment extends Fragment {

    private Box<FavouriteMovie> box = ObjectBox.get().boxFor(FavouriteMovie.class);
    private MoviesRecyclerViewAdapter myAdapter;
    private List<Movie> movies = new ArrayList<>();
    private PopularMoviesFragment.Screen mScreenSize = PopularMoviesFragment.Screen.REGULAR;
    private int mSpanCount;
    private MovieService movieService;
    private boolean mLoadedAll;
    private Map<Integer, Boolean> myMap = new HashMap<>();

    public FavouriteFragment() {
        // Required empty public constructor
    }

    public static FavouriteFragment newInstance(String param1, String param2) {
        return new FavouriteFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void checkForScreenSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        Objects.requireNonNull(getActivity()).getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        float scaleFactor = metrics.density;

        float widthDp = widthPixels / scaleFactor;
        float heightDp = heightPixels / scaleFactor;

        float smallestWidth = Math.min(widthDp, heightDp);

        if (smallestWidth > 600) {
            mScreenSize = PopularMoviesFragment.Screen.TABLET;
        } else if (smallestWidth < 300)
            mScreenSize = PopularMoviesFragment.Screen.SMALL;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favourite, container, false);
        Button mBtnBack = view.findViewById(R.id.btnBack);
        checkForScreenSize();
        movieService = MovieApi.getClient().create(MovieService.class);

        for (FavouriteMovie favmov : box.getAll()) {
            loadFavMovies(favmov.movieid + "");
            myMap.put((int) favmov.movieid, false);
        }

        movieService = MovieApi.getClient().create(MovieService.class);

        RecyclerView mRecyclerView = view.findViewById(R.id.favrecyclerview);
        myAdapter = new MoviesRecyclerViewAdapter(getContext(), movies, PopularMoviesFragment.SortType.TITLE_ASC);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mSpanCount = 3;
            if (mScreenSize == PopularMoviesFragment.Screen.TABLET) mSpanCount++;
            else if (mScreenSize == PopularMoviesFragment.Screen.SMALL) mSpanCount--;
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mSpanCount = 2;
            if (mScreenSize == PopularMoviesFragment.Screen.TABLET) mSpanCount++;
            else if (mScreenSize == PopularMoviesFragment.Screen.SMALL) mSpanCount--;
        }
        GridLayoutManager mGridLayoutManager = new GridLayoutManager(getContext(), mSpanCount);
        mRecyclerView.setLayoutManager(mGridLayoutManager);
        mRecyclerView.setAdapter(myAdapter);

        myAdapter.setListener(new MovieIoListener() {
            @Override
            public void onMovieInsert(int id) {
                FavouriteMovie movie = new FavouriteMovie();
                movie.movieid = id;
                DbManager.get().getBox().put(movie);
                DbManager.get().getDatabase().child("users").child(
                        FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child("" + id)
                        .setValue("");
            }

            @Override
            public void onMovieRemove(int id) {
                Query<FavouriteMovie> query = ObjectBox.get().boxFor(FavouriteMovie.class).query().equal(FavouriteMovie_.movieid, id).build();
                FavouriteMovie favmovie = query.findFirst(); //it started showing an error today(22/06/2020), but compiles and works normally nevertheless
                DbManager.get().getDatabase().child("users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).child("" + id).setValue(null); //favmovie.movieid
                if (favmovie != null) {
                    ObjectBox.get().boxFor(FavouriteMovie.class).remove(favmovie.id);
                }
            }
        });
        return view;
    }

    private Call<Movie> getMovie(String id) { //2
        return movieService.getMovie(id, getString(R.string.my_api_key), "en_US");
    }

    private Movie fetchResultsMovie(Response<Movie> response) { //3
        Movie movie = response.body();
        return movie;
    }

    private void loadFavMovies(String id) {
        getMovie(id).enqueue(new Callback<Movie>() { //6
            @Override
            public void onResponse(Call<Movie> call, Response<Movie> response) {
                if (movies == null)
                    movies = new ArrayList<>();

                Movie result = fetchResultsMovie(response);
                result.checked = true;
                myMap.put(Integer.parseInt(id), true);
                mLoadedAll = true;
                for (FavouriteMovie favmov : box.getAll()) {
                    if (!myMap.get((int) favmov.movieid)) {
                        mLoadedAll = false;
                    }
                }
                myAdapter.add(result);
                if (mLoadedAll) {
                    myAdapter.sort(PopularMoviesFragment.SortType.TITLE_ASC);
                    myAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<Movie> call, Throwable t) {
            }
        });
    }
}