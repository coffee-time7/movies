package com.qsdbih.movielist.adapters;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.qsdbih.movielist.database.FavouriteMovie;
import com.qsdbih.movielist.database.FavouriteMovie_;
import com.qsdbih.movielist.database.Movie;
import com.qsdbih.movielist.database.ObjectBox;
import com.qsdbih.movielist.R;
import com.qsdbih.movielist.fragments.DetailFragment;
import com.qsdbih.movielist.fragments.PopularMoviesFragment;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class MoviesRecyclerViewAdapter extends RecyclerView.Adapter<MoviesRecyclerViewAdapter.MyViewHolder> {

    private static final int ITEM = 0;
    private static final int LOADING = 1;
    private static String BASE_URL_IMG;

    private Context mContext;
    private List<Movie> movies;
    private PopularMoviesFragment.SortType sort;
    private DatabaseReference mDatabase;

    private Dialog myDialog;
    private PopularMoviesFragment.Screen mScreenSize = PopularMoviesFragment.Screen.REGULAR;

    private boolean mTwoPane = false;

    private MovieIoListener mListener;

    public MoviesRecyclerViewAdapter(Context mContext, List<Movie> movies, PopularMoviesFragment.SortType sort, PopularMoviesFragment.Screen screenSize) {
        this.mContext = mContext;
        this.sort = sort;
        this.movies = movies;
        mScreenSize = screenSize;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        sort(sort);
        setBaseUrlImg();
    }

    public MoviesRecyclerViewAdapter(Context mContext, List<Movie> movies, PopularMoviesFragment.SortType sort) {
        this.mContext = mContext;
        this.sort = sort;
        this.movies = movies;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        sort(sort);
        setBaseUrlImg();
    }

    private void setBaseUrlImg(){
        if (mScreenSize == PopularMoviesFragment.Screen.TABLET) {
            BASE_URL_IMG = mContext.getString(R.string.base_url_img_tablets);
        }else{
            BASE_URL_IMG = mContext.getString(R.string.base_url_image_phones);
        }
    }

    public void setListener(MovieIoListener listener) {
        mListener = listener;
    }

    public void sort(PopularMoviesFragment.SortType sort) {
        switch (sort) {
            case TITLE_ASC:
                Collections.sort(movies, new Movie.TitleComparator());
                break;
            case DATE_ASC:
                Collections.sort(movies, new Movie.DateComparator());
                break;
            case LIKE_ASC:
                Collections.sort(movies, new Movie.LikesComparator());
                break;
            case TITLE_DESC:
                sort(PopularMoviesFragment.SortType.TITLE_ASC);
                Collections.reverse(movies);
                break;
            case DATE_DESC:
                sort(PopularMoviesFragment.SortType.DATE_ASC);
                Collections.reverse(movies);
                break;
            case LIKE_DESC:
                sort(PopularMoviesFragment.SortType.LIKE_ASC);
                Collections.reverse(movies);
                break;
        }
        this.sort = sort;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;
        LayoutInflater mInflater = LayoutInflater.from(mContext);
        view = mInflater.inflate(R.layout.cardview_item_movie, parent, false);

        return new MyViewHolder(view);
    }

    @SuppressLint({"SetTextI18n", "SimpleDateFormat"})
    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {

        // ANIMATIONS
        /*
         holder.img_movie_thumbnail.setAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_transition_animation));
         holder.llcardview.setAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_scale_animation));
         */
        // - END ANIMATIONS
        Movie movie_ = movies.get(position);

        switch (getItemViewType(position)) {
            case ITEM:
                holder.cbCardview.setOnCheckedChangeListener(null);

                holder.cbCardview.setChecked(movie_.checked);

                int pos;
                if (sort == PopularMoviesFragment.SortType.TITLE_DESC || sort == PopularMoviesFragment.SortType.LIKE_DESC || sort== PopularMoviesFragment.SortType.DATE_DESC) {
                    pos = getItemCount() - position;
                } else {
                    pos = position + 1;
                }
                holder.tv_movie_title.setText(pos + "." + movie_.getTitle());

                Picasso.get()
                        .load(BASE_URL_IMG + movie_.getPosterPath())
                        //.centerCrop()
                        .placeholder(R.drawable.progress_animation)
                        .into(holder.img_movie_thumbnail);


                holder.cbCardview.setText(movie_.getVoteCount() + "");
                if (movie_.getReleaseDate() != null) {
                    holder.movie_release_date.setText(new SimpleDateFormat("MMM-dd-yyyy").format(movie_.getReleaseDate()));
                } else {
                    holder.movie_release_date.setText("N/A");
                }
                myDialog = new Dialog(mContext);
                myDialog.setContentView(R.layout.fragment_details);
                Objects.requireNonNull(myDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                holder.cardView.setOnClickListener((View v) -> {
                    if (!mTwoPane) {
                        WindowManager.LayoutParams lp = myDialog.getWindow().getAttributes();
                        lp.dimAmount = 0.8f;
                        TextView tvFragmentTitle = myDialog.findViewById(R.id.tvFragmentTitle);
                        TextView tvFragmentDate = myDialog.findViewById(R.id.tvFragmentDate);
                        TextView tvFragmentOverview = myDialog.findViewById(R.id.tvFragmentOverview);
                        ImageView ivFragmentPoster = myDialog.findViewById(R.id.ivFragmentPoster);
                        tvFragmentTitle.setText(movie_.getTitle());
                        tvFragmentDate.setText(new SimpleDateFormat("MMM-dd-yyyy").format(movie_.getReleaseDate()));
                        tvFragmentOverview.setText("Overview:" + movie_.getOverview());
                        Picasso.get()
                                .load(BASE_URL_IMG + movie_.getPosterPath())
                                //.centerCrop()
                                .placeholder(R.drawable.progress_animation)
                                .into(ivFragmentPoster);

                        myDialog.show();
                    } else {
                        Bundle arguments = new Bundle();
                        arguments.putString("MOVIE_ID", "" + movie_.getId());
                        DetailFragment fragment = new DetailFragment();
                        fragment.setArguments(arguments);
                        /*f_manager.beginTransaction()
                                .replace(R.id.frameDetails, fragment)
                                .commit();*/
                    }
                });
                holder.cbCardview.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        holder.cbCardview.setText(movie_.getVoteCount() + 1 + "");
                        movie_.checked = true;
                        mListener.onMovieInsert(movie_.getId());
                    } else {
                        movie_.checked = false;
                        holder.cbCardview.setText(movie_.getVoteCount() + "");
                        mListener.onMovieRemove(movie_.getId());
                    }
                });
                break;

            case LOADING:
                //                Do nothing
                break;
        }
    }

    public void add(Movie mc) {
        if (mc != null && mc.getId() != 0) {
            movies.add(mc);
            notifyItemInserted(movies.size() - 1);
        }
    }

    public void addAll(List<Movie> mcList) {
        if (movies == null)
            movies = mcList;
        else {
            for (Movie m : mcList)
                add(m);
        }
    }

    public void remove(Movie city) {
        int position = movies.indexOf(city);
        if (position > -1) {
            movies.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clear() {
        while (getItemCount() > 0) {
            remove(getItem(0));
        }
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public Movie getItem(int position) {
        return movies.get(position);
    }

    @Override
    public int getItemCount() {
        return movies == null ? 0 : movies.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout movie_detail;
        TextView tv_movie_title, movie_release_date;
        ImageView img_movie_thumbnail;
        CardView cardView;
        CheckBox cbCardview;
        LinearLayout llcardview;

        public MyViewHolder(View itemView) {

            super(itemView);
            movie_detail = itemView.findViewById(R.id.movie_detail);
            llcardview = itemView.findViewById(R.id.llcardview);
            movie_release_date = itemView.findViewById(R.id.movie_release_date);
            tv_movie_title = itemView.findViewById(R.id.movie_title_id);
            img_movie_thumbnail = itemView.findViewById(R.id.movie_img_id);
            cardView = itemView.findViewById(R.id.cardview_id);
            cbCardview = itemView.findViewById(R.id.cbCardview);
        }
    }
}