package com.smn.utpcontacts.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.smn.utpcontacts.model.Phone;

import java.util.List;

@Dao
public interface PhoneDao {
    @Query("SELECT * FROM phones WHERE contactId = :contactId")
    LiveData<List<Phone>> getPhonesForContact(String contactId);

    @Insert
    void insert(Phone phone);

    @Update
    void update(Phone phone);

    @Delete
    void delete(Phone phone);
}
