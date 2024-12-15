package com.smn.utpcontacts.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smn.utpcontacts.R;
import com.smn.utpcontacts.database.AppDatabase;
import com.smn.utpcontacts.model.Contact;
import com.smn.utpcontacts.util.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imageViewPhoto;
    private Uri selectedImageUri;

    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private static final int CAMERA_REQUEST = 2;
    private Uri photoURI;

    private MaterialButton btnCall;
    private static final int CALL_PHONE_PERMISSION_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_contact);

        initComponents();
        setupToolbar();
        setupTextWatchers();
        setupCallButton();
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

        imageViewPhoto = findViewById(R.id.imageViewPhoto);
        findViewById(R.id.buttonChangePhoto).setOnClickListener(v -> selectImage());
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

        // Inicializar el botón de llamada
        btnCall = findViewById(R.id.btnCall);
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

        if (contact.getPhotoUrl() != null && !contact.getPhotoUrl().isEmpty()) {
            File photoFile = new File(contact.getPhotoUrl());
            if (photoFile.exists()) {
                selectedImageUri = Uri.fromFile(photoFile);
                Glide.with(this)
                        .load(photoFile)
                        .circleCrop()
                        .into(imageViewPhoto);
            }
        }

        contact.setPhotoChanged(false);
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
            // Insertar el contacto y obtener el ID generado
            long id = db.contactDao().insert(newContact);
            newContact.setId(id);

            // Si hay una imagen seleccionada, guardarla
            if (selectedImageUri != null) {
                String fileName = "contact_" + id + ".jpg";
                File destFile = new File(getFilesDir(), fileName);

                try {
                    // Copiar la imagen al almacenamiento interno
                    InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                    FileOutputStream outputStream = new FileOutputStream(destFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    outputStream.close();
                    inputStream.close();

                    // Actualizar el contacto con la ruta de la imagen
                    newContact.setPhotoUrl(destFile.getAbsolutePath());
                    db.contactDao().update(newContact);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(() -> {
                showSuccess(R.string.contact_created);
                finish();
            });
        });
    }

    private void updateExistingContact(String name, String phone, String address) {
        showLoading();
        contact.setName(name);
        contact.setMainPhone(phone);
        contact.setAddress(address);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Solo procesar la imagen si ha cambiado
                if (selectedImageUri != null && contact.isPhotoChanged()) {
                    // Crear nombre de archivo único
                    String fileName = "contact_" + contact.getId() + "_" +
                            System.currentTimeMillis() + ".jpg";
                    File destFile = new File(getFilesDir(), fileName);

                    // Eliminar foto anterior si existe
                    if (contact.getPhotoUrl() != null) {
                        File oldFile = new File(contact.getPhotoUrl());
                        if (oldFile.exists()) {
                            oldFile.delete();
                        }
                    }

                    // Copiar nueva imagen
                    try (InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                         FileOutputStream outputStream = new FileOutputStream(destFile)) {

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }

                    // Actualizar la ruta en el contacto
                    contact.setPhotoUrl(destFile.getAbsolutePath());
                }

                // Actualizar en base de datos
                db.contactDao().update(contact);

                runOnUiThread(() -> {
                    showSuccess(R.string.contact_updated);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(AddEditContactActivity.this,
                            "Error al actualizar el contacto: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    showForm();
                });
            }
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

    private void selectImage() {
        String[] options = {"Tomar foto", "Elegir de galería", "Cancelar"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Añadir foto")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Tomar foto con la cámara
                        if (checkCameraPermission()) {
                            openCamera();
                        }
                    } else if (which == 1) {
                        // Elegir de galería
                        openGallery();
                    }
                })
                .show();
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CALL_PHONE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String phoneNumber = isEditMode && contact != null ?
                        contact.getMainPhone() : etPhone.getText().toString().trim();
                if (!phoneNumber.isEmpty()) {
                    makePhoneCall(phoneNumber);
                }
            } else {
                Toast.makeText(this, "Permiso de llamada denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Seleccionar imagen"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                selectedImageUri = data.getData();
                if (contact != null) {
                    contact.setPhotoChanged(true);
                }
                loadImage(selectedImageUri);
                hasUnsavedChanges = true;
            } else if (requestCode == CAMERA_REQUEST) {
                if (photoURI != null) {
                    selectedImageUri = photoURI;
                    if (contact != null) {
                        contact.setPhotoChanged(true);
                    }
                    loadImage(selectedImageUri);
                    hasUnsavedChanges = true;
                }
            }
        }
    }

    private void loadImage(Uri uri) {
        Log.d("AddEditContact", "Cargando imagen desde: " + uri.toString());
        if (uri.getScheme() != null && uri.getScheme().equals("file")) {
            // Es un archivo local
            File file = new File(uri.getPath());
            Glide.with(this)
                    .load(file)
                    .circleCrop()
                    .into(imageViewPhoto);
        } else {
            // Es una URI de contenido (como cuando se selecciona de la galería)
            Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(imageViewPhoto);
        }
    }

    private void saveImage(Uri uri, Contact contact) {
        // Crea un nombre único para la imagen
        String fileName = "contact_" + contact.getId() + ".jpg";
        File destFile = new File(getFilesDir(), fileName);

        try {
            // Copia la imagen al almacenamiento interno
            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            // Guarda la ruta en el contacto
            contact.setPhotoUrl(destFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creando el archivo", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.smn.utpcontacts.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Crear un nombre de archivo único usando timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // Obtener el directorio para guardar las imágenes
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Crear el archivo temporal
        return File.createTempFile(
                imageFileName,  /* prefijo */
                ".jpg",        /* sufijo */
                storageDir     /* directorio */
        );
    }

    private void setupCallButton() {
        btnCall.setOnClickListener(v -> {
            if (isEditMode && contact != null && contact.getMainPhone() != null && !contact.getMainPhone().isEmpty()) {
                requestCall(contact.getMainPhone());
            } else {
                String phoneNumber = etPhone.getText().toString().trim();
                if (!phoneNumber.isEmpty()) {
                    requestCall(phoneNumber);
                } else {
                    Toast.makeText(this, "No hay número de teléfono disponible", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void requestCall(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    CALL_PHONE_PERMISSION_REQUEST);
        } else {
            makePhoneCall(phoneNumber);
        }
    }

    private void makePhoneCall(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (SecurityException e) {
            Toast.makeText(this, "Error al realizar la llamada", Toast.LENGTH_SHORT).show();
            Log.e("AddEditContact", "Error al realizar llamada: " + e.getMessage());
        }
    }
}