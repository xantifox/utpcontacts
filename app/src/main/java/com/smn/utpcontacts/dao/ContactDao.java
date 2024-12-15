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
    @Query("SELECT * FROM contacts WHERE userId = :userId AND isPrivate = 0 ORDER BY " +
            "CASE WHEN :orderBy = 'name' THEN name " +
            "WHEN :orderBy = 'address' THEN address " +
            "ELSE name END " +
            "COLLATE NOCASE " +
            "ASC")
    LiveData<List<Contact>> getAllContacts(String userId, String orderBy);


    @Query("SELECT * FROM contacts WHERE userId = :userId ORDER BY " +
            "CASE WHEN :orderBy = 'name' THEN name " +
            "WHEN :orderBy = 'address' THEN address " +
            "ELSE name END " +
            "COLLATE NOCASE " +
            "ASC")
    LiveData<List<Contact>> getAllContactsAuthenticated(String userId, String orderBy);


    @Query("SELECT * FROM contacts WHERE id = :contactId")
    LiveData<Contact> getContactById(long contactId);

    @Query("SELECT * FROM contacts " +
            "WHERE userId = :userId " +
            "AND (name LIKE '%' || :query || '%' " +
            "OR mainPhone LIKE '%' || :query || '%' " +
            "OR address LIKE '%' || :query || '%') " +
            "ORDER BY " +
            "CASE WHEN :orderBy = 'name' THEN name " +
            "WHEN :orderBy = 'address' THEN address " +
            "ELSE name END " +
            "COLLATE NOCASE " +
            "ASC")
    LiveData<List<Contact>> searchContacts(String userId, String query, String orderBy);

    @Query("SELECT * FROM contacts " +
            "WHERE userId = :userId AND favorite = 1 " +
            "ORDER BY " +
            "CASE WHEN :orderBy = 'name' THEN name " +
            "WHEN :orderBy = 'address' THEN address " +
            "ELSE name END " +
            "COLLATE NOCASE " +
            "ASC")
    LiveData<List<Contact>> getFavoriteContacts(String userId, String orderBy);

    @Insert
    long insert(Contact contact);

    @Update
    int update(Contact contact);

    @Delete
    int delete(Contact contact);

    @Query("SELECT COUNT(*) FROM contacts WHERE userId = :userId")
    LiveData<Integer> getContactCount(String userId);

    @Query("SELECT COUNT(*) FROM contacts WHERE userId = :userId AND favorite = 1")
    LiveData<Integer> getFavoriteCount(String userId);

    @Query("SELECT * FROM contacts " +
            "WHERE userId = :userId " +
            "AND id IN (" +
            "    SELECT id FROM contacts " +
            "    WHERE userId = :userId " +
            "    ORDER BY " +
            "    CASE WHEN :orderBy = 'name' THEN name " +
            "    WHEN :orderBy = 'address' THEN address " +
            "    ELSE name END " +
            "    COLLATE NOCASE ASC " +
            "    LIMIT :limit OFFSET :offset" +
            ")")
    LiveData<List<Contact>> getContactsPaged(String userId, String orderBy, int limit, int offset);
}