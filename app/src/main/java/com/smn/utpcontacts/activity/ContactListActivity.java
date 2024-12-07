package com.smn.utpcontacts.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.content.Intent;
import androidx.appcompat.widget.Toolbar;
import android.widget.TextView;
import android.widget.Toast;
import com.smn.utpcontacts.R;
import com.smn.utpcontacts.adapter.ContactAdapter;
import com.smn.utpcontacts.database.AppDatabase;
import com.smn.utpcontacts.model.Contact;
import com.smn.utpcontacts.util.SessionManager;
import java.util.ArrayList;
import java.util.List;

public class ContactListActivity extends AppCompatActivity implements
        ContactAdapter.OnContactClickListener,
        ContactAdapter.OnFavoriteClickListener {

    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private TextView textViewEmpty;
    private AppDatabase db;
    private SessionManager sessionManager;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        initComponents();
        setupRecyclerView();
        loadContacts();
    }

    private void initComponents() {
        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserEmail();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerViewContacts);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        FloatingActionButton fab = findViewById(R.id.fabAddContact);
        fab.setOnClickListener(v -> addNewContact());

        setupSearchView();
    }

    private void setupSearchView() {
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    loadContacts();
                } else {
                    filterContacts(newText);
                }
                return true;
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ContactAdapter(new ArrayList<>());
        adapter.setOnContactClickListener(this);
        adapter.setOnFavoriteClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onContactClick(Contact contact) {
        Toast.makeText(this, "Editar contacto: " + contact.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFavoriteClick(Contact contact, int position) {
        contact.setFavorite(!contact.isFavorite());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.contactDao().update(contact);
        });
    }

    private void loadContacts() {
        db.contactDao().getAllContacts(currentUserId).observe(this, contacts -> {
            adapter.setContacts(contacts);
            updateEmptyView(contacts.isEmpty());
        });
    }

    private void filterContacts(String query) {
        db.contactDao().searchContacts(currentUserId, query).observe(this, contacts -> {
            adapter.setContacts(contacts);
            updateEmptyView(contacts.isEmpty());
        });
    }

    private void updateEmptyView(boolean isEmpty) {
        textViewEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void addNewContact() {
        // TODO: Implementar creación de contacto
        Toast.makeText(this, "Agregar nuevo contacto", Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}