package com.smn.utpcontacts.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.smn.utpcontacts.model.Contact;
import java.util.List;

@Dao
public interface ContactDao {
    @Query("SELECT * FROM contacts WHERE userId = :userId ORDER BY name ASC")
    LiveData<List<Contact>> getAllContacts(String userId);

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    Contact getContactById(long contactId);

    @Query("SELECT * FROM contacts WHERE userId = :userId AND name LIKE '%' || :query || '%'")
    LiveData<List<Contact>> searchContacts(String userId, String query);

    @Query("SELECT * FROM contacts WHERE userId = :userId AND favorite = 1 ORDER BY name ASC")
    LiveData<List<Contact>> getFavoriteContacts(String userId);

    @Insert
    void insert(Contact contact);

    @Update
    void update(Contact contact);

    @Delete
    void delete(Contact contact);

    @Query("SELECT COUNT(*) FROM contacts WHERE userId = :userId")
    LiveData<Integer> getContactCount(String userId);

    @Query("SELECT * FROM contacts WHERE userId = :userId AND mainPhone LIKE '%' || :query || '%'")
    LiveData<List<Contact>> searchContactsByPhone(String userId, String query);

    @Query("SELECT * FROM contacts WHERE userId = :userId AND address LIKE '%' || :query || '%'")
    LiveData<List<Contact>> searchContactsByAddress(String userId, String query);
}