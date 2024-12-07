package com.smn.utpcontacts.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.smn.utpcontacts.model.Email;

import java.util.List;

@Dao
public interface EmailDao {
    @Query("SELECT * FROM emails WHERE contactId = :contactId")
    LiveData<List<Email>> getEmailsForContact(String contactId);

    @Insert
    void insert(Email email);

    @Update
    void update(Email email);

    @Delete
    void delete(Email email);
}