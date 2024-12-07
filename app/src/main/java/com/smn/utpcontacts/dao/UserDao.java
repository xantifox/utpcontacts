package com.smn.utpcontacts.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;
import com.smn.utpcontacts.model.User;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    User authenticate(String email, String password);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(User user);

    @Query("SELECT * FROM users WHERE email = :email")
    User findByEmail(String email);
}