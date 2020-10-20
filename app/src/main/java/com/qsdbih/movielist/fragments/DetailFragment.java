package com.qsdbih.movielist.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.qsdbih.movielist.R;
import com.qsdbih.movielist.database.Movie;

import java.text.SimpleDateFormat;

public class DetailFragment extends Fragment {

    public static final String ARG_ITEM_ID = "item_id";
    private String movieId;
    Movie movie = new Movie();

    public DetailFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            movieId = getArguments().getString("MOVIE_ID");
        }
    }

    @SuppressLint("SimpleDateFormat")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.details, container, false);

        if (!movieId.equals("")) {
            ((TextView) rootView.findViewById(R.id.txtTitle)).setText(movie.getTitle());

            ((TextView) rootView.findViewById(R.id.tvDate)).setText(new SimpleDateFormat("MMM-dd-yyyy").format(movie.getReleaseDate()));
            ((TextView) rootView.findViewById(R.id.txtTitle)).setText(movie.getOverview());
        }
        return rootView;
    }
}
