package com.qsdbih.movielist.database;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class FavouriteMovie {
    @Id
    public long id;
    public long movieid;
}
