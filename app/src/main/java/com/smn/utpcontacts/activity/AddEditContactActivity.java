package com.smn.utpcontacts.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smn.utpcontacts.R;
import com.smn.utpcontacts.database.AppDatabase;
import com.smn.utpcontacts.model.Contact;
import com.smn.utpcontacts.util.SessionManager;

public class AddEditContactActivity extends AppCompatActivity {
    private TextInputEditText etName, etPhone, etAddress;
    private AppDatabase db;
    private SessionManager sessionManager;
    private Contact contact;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_contact);

        initComponents();
        setupToolbar();
        checkMode();
    }

    private void initComponents() {
        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);

        FloatingActionButton fabSave = findViewById(R.id.fabSave);
        fabSave.setOnClickListener(v -> saveContact());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void checkMode() {
        long contactId = getIntent().getLongExtra("contact_id", -1);
        isEditMode = contactId != -1;

        if (isEditMode) {
            getSupportActionBar().setTitle("Editar Contacto");
            loadContact(contactId);
        } else {
            getSupportActionBar().setTitle("Nuevo Contacto");
        }
    }

    private void loadContact(long contactId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Contact loadedContact = db.contactDao().getContactById(contactId);
            runOnUiThread(() -> {
                if (loadedContact != null) {
                    contact = loadedContact;
                    populateFields();
                }
            });
        });
    }

    private void populateFields() {
        etName.setText(contact.getName());
        etPhone.setText(contact.getMainPhone());
        etAddress.setText(contact.getAddress());
    }

    private void saveContact() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (validateInput(name)) {
            if (isEditMode) {
                updateExistingContact(name, phone, address);
            } else {
                createNewContact(name, phone, address);
            }
        }
    }

    private boolean validateInput(String name) {
        if (name.isEmpty()) {
            etName.setError("El nombre es requerido");
            return false;
        }
        return true;
    }

    private void createNewContact(String name, String phone, String address) {
        Contact newContact = new Contact(name);
        newContact.setMainPhone(phone);
        newContact.setAddress(address);
        newContact.setUserId(sessionManager.getUserEmail());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.contactDao().insert(newContact);
            runOnUiThread(this::finish);
        });
    }

    private void updateExistingContact(String name, String phone, String address) {
        contact.setName(name);
        contact.setMainPhone(phone);
        contact.setAddress(address);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.contactDao().update(contact);
            runOnUiThread(this::finish);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}