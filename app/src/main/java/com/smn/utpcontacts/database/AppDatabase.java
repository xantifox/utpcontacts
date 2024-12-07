package com.smn.utpcontacts.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.smn.utpcontacts.dao.ContactDao;
import com.smn.utpcontacts.dao.EmailDao;
import com.smn.utpcontacts.dao.PhoneDao;
import com.smn.utpcontacts.dao.UserDao;
import com.smn.utpcontacts.model.Contact;
import com.smn.utpcontacts.model.Email;
import com.smn.utpcontacts.model.Phone;
import com.smn.utpcontacts.model.User;

@Database(entities = {Contact.class, Phone.class, Email.class, User.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE contacts ADD COLUMN mainPhone TEXT");
        }
    };

    public abstract ContactDao contactDao();
    public abstract PhoneDao phoneDao();
    public abstract EmailDao emailDao();
    public abstract UserDao userDao();

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context,
                                    AppDatabase.class,
                                    "contacts_database"
                            ).addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}