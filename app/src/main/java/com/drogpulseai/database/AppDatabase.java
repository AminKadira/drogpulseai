package com.drogpulseai.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.drogpulseai.database.dao.ContactDao;
import com.drogpulseai.database.dao.ProductDao;
import com.drogpulseai.database.entity.ContactEntity;

@Database(entities = {ContactEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "drogpulse_db";
    private static AppDatabase instance;

    public abstract ContactDao contactDao();
    public abstract ProductDao productDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}