package com.smn.utpcontacts.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smn.utpcontacts.R;
import com.smn.utpcontacts.database.AppDatabase;
import com.smn.utpcontacts.model.Contact;
import com.smn.utpcontacts.util.SessionManager;

public class AddEditContactActivity extends AppCompatActivity {
    private TextInputLayout tilName;
    private TextInputLayout tilPhone;
    private TextInputLayout tilAddress;
    private TextInputEditText etName;
    private TextInputEditText etPhone;
    private TextInputEditText etAddress;
    private FloatingActionButton fabSave;
    private View progressBar;

    private AppDatabase db;
    private SessionManager sessionManager;
    private Contact contact;
    private boolean isEditMode;
    private boolean hasUnsavedChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_contact);

        initComponents();
        setupToolbar();
        setupTextWatchers();
        checkMode();

        // Manejo del botón de retroceso
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (hasUnsavedChanges) {
                    showUnsavedChangesDialog();
                } else {
                    finish(); // Acción predeterminada para cerrar la actividad
                }
            }
        });
    }

    private void initComponents() {
        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);

        // TextInputLayouts
        tilName = findViewById(R.id.tilName);
        tilPhone = findViewById(R.id.tilPhone);
        tilAddress = findViewById(R.id.tilAddress);

        // EditTexts
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);

        // Progress Bar
        progressBar = findViewById(R.id.progressBar);

        // FAB
        fabSave = findViewById(R.id.fabSave);
        fabSave.setOnClickListener(v -> saveContact());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No necesitamos implementar nada aquí
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasUnsavedChanges = true;
                clearErrors();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No necesitamos implementar nada aquí
            }
        };

        etName.addTextChangedListener(textWatcher);
        etPhone.addTextChangedListener(textWatcher);
        etAddress.addTextChangedListener(textWatcher);
    }

    private void checkMode() {
        long contactId = getIntent().getLongExtra("contact_id", -1);
        isEditMode = contactId != -1;

        if (isEditMode) {
            setTitle(R.string.edit_contact);
            loadContact(contactId);
        } else {
            setTitle(R.string.new_contact);
            showForm();
        }
    }

    private void loadContact(long contactId) {
        showLoading();
        db.contactDao().getContactById(contactId).observe(this, loadedContact -> {
            if (loadedContact != null) {
                contact = loadedContact;
                populateFields();
                showForm();
            } else {
                showError(R.string.contact_not_found);
                finish();
            }
        });
    }

    private void populateFields() {
        etName.setText(contact.getName());
        etPhone.setText(contact.getMainPhone());
        etAddress.setText(contact.getAddress());
        hasUnsavedChanges = false;
    }

    private void saveContact() {
        if (!validateInput()) {
            return;
        }

        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        showLoading();

        if (isEditMode) {
            updateExistingContact(name, phone, address);
        } else {
            createNewContact(name, phone, address);
        }
    }

    private boolean validateInput() {
        boolean isValid = true;

        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            tilName.setError(getString(R.string.name_required));
            isValid = false;
        }

        String phone = etPhone.getText().toString().trim();
        if (!phone.isEmpty() && !isValidPhoneNumber(phone)) {
            tilPhone.setError(getString(R.string.invalid_phone));
            isValid = false;
        }

        return isValid;
    }

    private boolean isValidPhoneNumber(String phone) {
        // Implementar validación de número telefónico según tus requisitos
        return phone.matches("\\+?\\d{9,15}");
    }

    private void createNewContact(String name, String phone, String address) {
        Contact newContact = new Contact(name);
        newContact.setMainPhone(phone);
        newContact.setAddress(address);
        newContact.setUserId(sessionManager.getUserEmail());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.contactDao().insert(newContact);
            runOnUiThread(() -> {
                showSuccess(R.string.contact_created);
                finish();
            });
        });
    }

    private void updateExistingContact(String name, String phone, String address) {
        contact.setName(name);
        contact.setMainPhone(phone);
        contact.setAddress(address);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.contactDao().update(contact);
            runOnUiThread(() -> {
                showSuccess(R.string.contact_updated);
                finish();
            });
        });
    }

    private void clearErrors() {
        tilName.setError(null);
        tilPhone.setError(null);
        tilAddress.setError(null);
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        fabSave.setEnabled(false);
    }

    private void showForm() {
        progressBar.setVisibility(View.GONE);
        fabSave.setEnabled(true);
    }

    private void showError(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (hasUnsavedChanges) {
                showUnsavedChangesDialog(); // Mostrar el diálogo de confirmación
            } else {
                finish(); // Cerrar la actividad
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showUnsavedChangesDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.unsaved_changes_title)
                .setMessage(R.string.unsaved_changes_message)
                .setPositiveButton(R.string.discard, (dialog, which) -> finish())
                .setNegativeButton(R.string.keep_editing, null)
                .show();
    }
}