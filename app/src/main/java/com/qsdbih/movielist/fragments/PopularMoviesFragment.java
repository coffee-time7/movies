package com.qsdbih.movielist.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.qsdbih.movielist.adapters.MovieIoListener;
import com.qsdbih.movielist.database.FavouriteMovie;
import com.qsdbih.movielist.database.FavouriteMovie_;
import com.qsdbih.movielist.database.Movie;
import com.qsdbih.movielist.database.ObjectBox;
import com.qsdbih.movielist.PaginationScrollListener;
import com.qsdbih.movielist.R;
import com.qsdbih.movielist.adapters.MoviesRecyclerViewAdapter;
import com.qsdbih.movielist.activities.LoginActivity;
import com.qsdbih.movielist.api.MovieApi;
import com.qsdbih.movielist.api.MovieService;
import com.qsdbih.movielist.database.Movie_;
import com.qsdbih.movielist.objects.PopularMovies;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.objectbox.Box;
import io.objectbox.query.Query;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PopularMoviesFragment extends Fragment {

    private static final String KEY_RECYCLER_STATE = "RVPosition";
    private static final String ARGS_POSITION = "position";
    private static final String ARGS_CURRENT_PAGE = "currentpage";
    private static final String ARGS_READ_NAME = "readname";
    private static final String ARGS_SORT = "sort";
    private static final String UPDATE_TIME = "updatetime";

    private MaterialButton mBtnLogout;
    private Chip mBtnTitle, mBtnDate, mBtnLikes, mBtnFav;

    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private GoogleApiClient mGoogleApiClient;
    private MovieService movieService;

    private int spanCount, savedPage;
    private List<Movie> movies;
    private RecyclerView mMoviesRecyclerView;
    private MoviesRecyclerViewAdapter mMoviesAdapter;
    private SortType mSort = SortType.NO_SORT;
    private StaggeredGridLayoutManager mGridLayoutManager;
    private List<Movie> mUpdateArrayList = new ArrayList<>();
    private Screen mScreenSize = Screen.REGULAR; // 0 - regular screen size, 1 tablets, 2 small devices

    private SharedPreferences mSharedPreferences;

    private static final int PAGE_START = 1;
    private boolean isLoading;
    private boolean isLastPage;
    private int TOTAL_PAGES = 5;
    private int currentPage = PAGE_START;
    private boolean readName = false;
    private int scrollPosition = 0;

    public PopularMoviesFragment() {
    }

    public static PopularMoviesFragment newInstance() {
        return new PopularMoviesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_popular_movies, container, false);

        initViews(view);
        initScreenSize();
        ObjectBox.init(getContext());
        initSharedPrefs();
        setupRecyclerView();

        updateFilterButtonColors(SortType.NO_SORT);

        mGridLayoutManager = new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL);
        mMoviesRecyclerView.setLayoutManager(mGridLayoutManager);
        mMoviesRecyclerView.setAdapter(mMoviesAdapter);

        //init service
        movieService = MovieApi.getClient().create(MovieService.class);

        addScrollListener();

        if (savedInstanceState != null) {
            readName = savedInstanceState.getBoolean(ARGS_READ_NAME);
            mSort = (SortType) savedInstanceState.getSerializable(ARGS_SORT);
            savedPage = savedInstanceState.getInt(ARGS_CURRENT_PAGE);

            if (savedPage > TOTAL_PAGES) {
                savedPage = TOTAL_PAGES;
            }

            int i = 1;
            loadNextPage();
            while (i < savedPage) {
                currentPage += 1;
                loadNextPage();
                i++;
            }
            scrollPosition = savedInstanceState.getInt(ARGS_POSITION);
            updateFilterButtonColors(mSort);

            if (savedPage == TOTAL_PAGES) {
                isLastPage = true;
            }
        } else if (currentPage == 1) {
            updateFilterButtonColors(mSort);
            mMoviesAdapter.clear();
            if (mUpdateArrayList.size() != 0) {
                mMoviesAdapter.addAll(mUpdateArrayList);
                clearUpAdapter();
                mMoviesAdapter.notifyDataSetChanged();
            } else {
                loadNextPage();
            }

        } else if (currentPage > 1) {

            updateFilterButtonColors(mSort);

            savedPage = currentPage;
            if (savedPage > TOTAL_PAGES) savedPage = TOTAL_PAGES;
            if (currentPage > TOTAL_PAGES) currentPage = TOTAL_PAGES;
            if (mMoviesAdapter.getItemCount() != 0) mMoviesAdapter.clear();
            mMoviesAdapter.addAll(mUpdateArrayList);
            clearUpAdapter();
            mMoviesAdapter.notifyDataSetChanged();
        }

        setupButtons();

        if (!readName) {  //Check if Welcome:user toast has already been shown, on screen rotation/etc
            if (user.getDisplayName() != null) {
                user.getDisplayName();
                Toast.makeText(getContext(), getString(R.string.welcome_user, user.getDisplayName()), Toast.LENGTH_SHORT).show();
                readName = true;
            } else {
                Toast.makeText(getContext(), getString(R.string.welcome_user, user.getEmail()), Toast.LENGTH_SHORT).show();
            }
        }
        return view;
    }

    private void setupButtons() {
        mBtnFav.setOnClickListener(v -> {
            if (mUpdateArrayList != null) {
                mUpdateArrayList.clear();
            } else {
                mUpdateArrayList = new ArrayList<>();
            }
            for (int i = 0; i < mMoviesAdapter.getItemCount(); i++) {
                mUpdateArrayList.add(mMoviesAdapter.getItem(i));
            }
            FavouriteFragment nextFrag = new FavouriteFragment();
            Objects.requireNonNull(getActivity()).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, nextFrag, "favouriteFragment")
                    .addToBackStack(null)
                    .commit();
        });
        mBtnTitle.setOnClickListener(v -> {
            if (mSort == SortType.TITLE_ASC) {
                Toast.makeText(getContext(), SortType.TITLE_DESC.getDescription(), Toast.LENGTH_SHORT).show();
                mSort = SortType.TITLE_DESC;
            } else {
                Toast.makeText(getContext(), SortType.TITLE_ASC.getDescription(), Toast.LENGTH_SHORT).show();
                mSort = SortType.TITLE_ASC;
                updateFilterButtonColors(mSort);
            }
            mMoviesAdapter.sort(mSort);
            mMoviesAdapter.notifyDataSetChanged();
        });
        mBtnDate.setOnClickListener(v -> {
            if (mSort == SortType.DATE_ASC) {
                Toast.makeText(getContext(), SortType.DATE_DESC.getDescription(), Toast.LENGTH_SHORT).show();
                mSort = SortType.DATE_DESC;
            } else {
                Toast.makeText(getContext(), SortType.DATE_ASC.getDescription(), Toast.LENGTH_SHORT).show();
                mSort = SortType.DATE_ASC;
                updateFilterButtonColors(mSort);
            }
            mMoviesAdapter.sort(mSort);
            mMoviesAdapter.notifyDataSetChanged();
        });
        mBtnLikes.setOnClickListener(v -> {
            if (mSort == SortType.LIKE_ASC) {
                Toast.makeText(getContext(), SortType.LIKE_DESC.getDescription(), Toast.LENGTH_SHORT).show();
                mSort = SortType.LIKE_DESC;
            } else {
                Toast.makeText(getContext(), SortType.LIKE_ASC.getDescription(), Toast.LENGTH_SHORT).show();
                mSort = SortType.LIKE_ASC;
                updateFilterButtonColors(mSort);
            }
            mMoviesAdapter.sort(mSort);
            mMoviesAdapter.notifyDataSetChanged();
        });

        mBtnLogout.setOnClickListener((View v) -> new AlertDialog.Builder(Objects.requireNonNull(getContext()))
                .setTitle("Log out")
                .setMessage("Are you sure?")
                .setPositiveButton("Log Out", (dialog, whichButton) -> {
                    Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(getContext(), LoginActivity.class);
                    FirebaseAuth.getInstance().signOut();
                    Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient);
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                    LoginManager.getInstance().logOut();
                    startActivity(i);
                    Objects.requireNonNull(getActivity()).getSupportFragmentManager().popBackStack();
                    getActivity().finish();
                    Objects.requireNonNull(getActivity()).finish();
                })
                .setNegativeButton(android.R.string.no, null).show());
    }

    private void addScrollListener() {
        mMoviesRecyclerView.addOnScrollListener(new PaginationScrollListener(mGridLayoutManager) {
            @Override
            protected void loadMoreItems() {
                isLoading = true;
                currentPage += 1;
                loadNextPage();
            }

            @Override
            public int getTotalPageCount() {
                return TOTAL_PAGES;
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });
    }

    private void initViews(View view) {
        mBtnTitle = view.findViewById(R.id.btnTitle);
        mBtnDate = view.findViewById(R.id.btnDate);
        mBtnLikes = view.findViewById(R.id.btnLikes);
        mBtnFav = view.findViewById(R.id.btnFav);
        mBtnLogout = view.findViewById(R.id.btnLogout);
        mMoviesRecyclerView = view.findViewById(R.id.recyclerview);
    }

    private void setupRecyclerView() {
        mMoviesRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mMoviesAdapter = new MoviesRecyclerViewAdapter(getContext(), movies, mSort, mScreenSize);

        mMoviesAdapter.setListener(new MovieIoListener() {
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
    }

    private void initSharedPrefs() {
        if (getActivity() != null) {
            mSharedPreferences = Objects.requireNonNull(this.getActivity()).getSharedPreferences("pref", Context.MODE_PRIVATE);
        }
    }

    public void initScreenSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        Objects.requireNonNull(getActivity()).getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        float scaleFactor = metrics.density;

        float widthDp = widthPixels / scaleFactor;
        float heightDp = heightPixels / scaleFactor;

        float smallestWidth = Math.min(widthDp, heightDp);
        if (smallestWidth > 600) {
            mScreenSize = Screen.SMALL;
        } else if (smallestWidth < 300) {
            mScreenSize = Screen.TABLET;
        }
        initSpanCount();
    }

    private void initSpanCount() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            spanCount = 3;
            if (mScreenSize == Screen.SMALL) {
                spanCount++;
            } else if (mScreenSize == Screen.TABLET) spanCount--;
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            spanCount = 2;
            if (mScreenSize == Screen.SMALL) {
                spanCount++;
            } else if (mScreenSize == Screen.TABLET) spanCount--;
        }
    }

    private Call<PopularMovies> callPopularMoviesApi() { //2
        return movieService.getPopularMovies(
                getString(R.string.my_api_key),
                "en_US",
                currentPage
        );
    }

    private List<Movie> fetchResults(Response<PopularMovies> response) { //3
        PopularMovies popularMovies = response.body();
        return popularMovies != null ? popularMovies.getResults() : null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        int lastItem = RecyclerView.NO_POSITION; // NO_POSITION == -1
        int[] array = new int[spanCount];
        mGridLayoutManager.findFirstVisibleItemPositions(array);
        for (int value : array) {
            if (lastItem < value)
                lastItem = value;
        }
        if (mGridLayoutManager != null) {
            outState.putParcelable(KEY_RECYCLER_STATE, mGridLayoutManager.onSaveInstanceState());
            outState.putInt(ARGS_POSITION, lastItem);
            outState.putInt(ARGS_CURRENT_PAGE, currentPage);
            outState.putBoolean(ARGS_READ_NAME, readName);
            outState.putSerializable(ARGS_SORT, mSort);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        clearUpAdapter();
    }

    private void updateFilterButtonColors(SortType sortType) {
        mBtnDate.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#e6e6e6")));
        mBtnTitle.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#e6e6e6")));
        mBtnFav.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#e6e6e6")));
        mBtnLikes.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#e6e6e6")));

        switch (sortType) {
            case TITLE_ASC:
            case TITLE_DESC:
                mBtnTitle.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#cdd1ea")));
                break;
            case DATE_ASC:
            case DATE_DESC:
                mBtnDate.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#cdd1ea")));
                break;
            case LIKE_ASC:
            case LIKE_DESC:
                mBtnLikes.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#cdd1ea")));
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(Objects.requireNonNull(getContext()))
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        mGoogleApiClient.connect();
    }

    private void loadNextPage() {
        long currentTime = System.currentTimeMillis();
        long lastKnownTime = mSharedPreferences.getLong(UPDATE_TIME, 1);

        boolean shouldRefreshData = (currentTime - lastKnownTime) > TimeUnit.HOURS.toMillis(8);
        long obCount = ObjectBox.get().boxFor(Movie.class).count();
        if (shouldRefreshData || obCount < 95) {
            callPopularMoviesApi().enqueue(new Callback<PopularMovies>() { //6
                @Override
                public void onResponse(@NonNull Call<PopularMovies> call, @NonNull Response<PopularMovies> response) {
                    if (shouldRefreshData) {
                        ObjectBox.get().boxFor(Movie.class).removeAll();
                    }
                    if (movies == null) {
                        movies = new ArrayList<>();
                    }
                    isLoading = false;

                    List<Movie> results = fetchResults(response);
                    List<Movie> boxresults = null;
                    for (int i = 0; i < results.size(); i++)
                        if (boxresults == null) {
                            boxresults = ObjectBox.get().boxFor(Movie.class).query().equal(Movie_.id, results.get(i).getId()).build().find();
                        } else
                            boxresults.addAll(ObjectBox.get().boxFor(Movie.class).query().equal(Movie_.id, results.get(i).getId()).build().find());
                    if (boxresults.size() < 1) {
                        ObjectBox.get().boxFor(Movie.class).put(results);
                        Log.d("PopularMoviesFragment", "OBJECTBOX");
                        mSharedPreferences.edit().putLong(UPDATE_TIME, System.currentTimeMillis() / 86400 / 3).apply();
                    }

                    boolean changed;
                    for (int i = results.size() - 1; i >= 0; i--) {
                        changed = false;
                        if (results.get(i).getId() == null) {
                            results.remove(i);
                            Log.d("PopularMoviesFragment", "Removed a null element from the list");
                            continue;
                        } // ^^Debug mode shows an extra 'null' element, usually at the end. Removing it solves the problem
                        for (FavouriteMovie favMov : ObjectBox.get().boxFor(FavouriteMovie.class).getAll()) {
                            if (favMov.movieid == results.get(i).getId()) {
                                results.get(i).checked = true;
                                changed = true;
                            }
                            if (!changed) {
                                results.get(i).checked = false;
                            }
                        }
                    }

                    mMoviesAdapter.addAll(results);

                    if (currentPage >= TOTAL_PAGES) isLastPage = true;

                    if (savedPage == currentPage) {
                        mMoviesAdapter.sort(mSort);
                        mMoviesRecyclerView.scrollToPosition(scrollPosition);
                    }
                    if (scrollPosition > 0 && currentPage == 1) {
                        mMoviesRecyclerView.scrollToPosition(scrollPosition);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<PopularMovies> call, @NonNull Throwable t) {
                    // handle failure
                }
            });
        } else {
            if (movies == null) {
                movies = new ArrayList<>();
            }
            isLoading = false;

            List<Movie> results = ObjectBox.get().boxFor(Movie.class).query().order(Movie_.movieid).build().find((currentPage - 1) * 20, 20);

            boolean changed;
            for (int i = results.size() - 1; i >= 0; i--) {
                changed = false;
                if (results.get(i).getId() == null) {
                    results.remove(i);
                    continue;
                } // ^^Debug mode shows an extra 'null' element, usually at the end. Removing it solves the problem
                for (FavouriteMovie favMov : ObjectBox.get().boxFor(FavouriteMovie.class).getAll()) {
                    if (favMov.movieid == results.get(i).getId()) {
                        results.get(i).checked = true;
                        changed = true;
                    }
                    if (!changed) {
                        results.get(i).checked = false;
                    }
                }
            }

            mMoviesAdapter.addAll(results);

            if (currentPage >= TOTAL_PAGES) {
                isLastPage = true;
            }

            if (savedPage == currentPage) {
                mMoviesAdapter.sort(mSort);
                mMoviesRecyclerView.scrollToPosition(scrollPosition);
            }
            if (scrollPosition > 0 && currentPage == 1)
                mMoviesRecyclerView.scrollToPosition(scrollPosition);
        }
    }

    public void clearUpAdapter() {
        List<Movie> mUpdateArrayList;

        mUpdateArrayList = new ArrayList<>();
        for (int i = 0; i < mMoviesAdapter.getItemCount(); i++) {
            if (!mUpdateArrayList.contains(mMoviesAdapter.getItem(i))) {
                mUpdateArrayList.add(mMoviesAdapter.getItem(i));
            }
        }
        for (int i = mUpdateArrayList.size() - 1; i >= 0; i--) {
            mUpdateArrayList.get(i).checked = false; //Set the movie to false, if it's found in favourites, then set it to true and continue checking the rest of the list

            for (FavouriteMovie favMov : ObjectBox.get().boxFor(FavouriteMovie.class).getAll()) {
                if (favMov.movieid == mUpdateArrayList.get(i).getId()) {
                    mUpdateArrayList.get(i).checked = true;
                    break;
                }
            }
        }
        mMoviesAdapter.clear();
        mMoviesAdapter.addAll(mUpdateArrayList);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public enum SortType {
        NO_SORT(0, "Default sort"), // defaultni poredak, kakav je dan u originalnom netaknutom responseu
        TITLE_ASC(1, "Ascending - Title"),
        DATE_ASC(2, "Ascending - Date"),
        LIKE_ASC(3, "Ascending - Likes"),
        TITLE_DESC(4, "Descending - Title"),
        DATE_DESC(5, "Descending - Date"),
        LIKE_DESC(6, "Descending - Likes");

        private int key;
        private String description;

        SortType(int key, String description) {
            this.key = key;
            this.description = description;
        }

        // zbog legacy razloga da ne refaktoriram i adapter sada
        public int getKey() {
            return key;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum Screen {
        REGULAR(0),
        TABLET(1),
        SMALL(2);

        private int mKey;

        Screen(int key) {
            this.mKey = key;
        }

        // zbog legacy razloga
        public int getKey() {
            return mKey;
        }
    }
}

class DbManager {
    private Box mBox;
    private DatabaseReference mDatabase;

    private static DbManager sInstance;

    private DbManager() {
        this.mBox = ObjectBox.get().boxFor(FavouriteMovie.class);
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public static DbManager get() {
        if (sInstance == null) {
            sInstance = new DbManager();
        }
        return sInstance;
    }

    public Box getBox() {
        return mBox;
    }

    public DatabaseReference getDatabase() {
        return mDatabase;
    }
}