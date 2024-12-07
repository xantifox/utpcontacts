package com.smn.utpcontacts.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.smn.utpcontacts.dao.ContactDao;
import com.smn.utpcontacts.dao.EmailDao;
import com.smn.utpcontacts.dao.PhoneDao;
import com.smn.utpcontacts.database.AppDatabase;
import com.smn.utpcontacts.model.Contact;
import com.smn.utpcontacts.model.Email;
import com.smn.utpcontacts.model.Phone;

import java.util.List;

public class ContactRepository {
    private ContactDao contactDao;
    private PhoneDao phoneDao;
    private EmailDao emailDao;
    private String userId;

    public ContactRepository(Application application, String userId) {
        AppDatabase db = AppDatabase.getDatabase(application);
        contactDao = db.contactDao();
        phoneDao = db.phoneDao();
        emailDao = db.emailDao();
        this.userId = userId;
    }

    // Métodos para Contact
    public LiveData<List<Contact>> getAllContacts() {
        return contactDao.getAllContacts(userId);
    }

    public void insert(Contact contact) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            contact.setUserId(userId);
            contactDao.insert(contact);
        });
    }

    // Métodos para Phone
    public LiveData<List<Phone>> getPhonesForContact(String contactId) {
        return phoneDao.getPhonesForContact(contactId);
    }

    public void insert(Phone phone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            phoneDao.insert(phone);
        });
    }

    // Métodos para Email
    public LiveData<List<Email>> getEmailsForContact(String contactId) {
        return emailDao.getEmailsForContact(contactId);
    }

    public void insert(Email email) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            emailDao.insert(email);
        });
    }
}