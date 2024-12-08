package com.smn.utpcontacts.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.content.Intent;
import androidx.appcompat.widget.Toolbar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.LiveData;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
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
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;
    private MenuItem favoriteMenuItem;
    private boolean showingFavorites = false;
    private LiveData<List<Contact>> currentContactsLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        initComponents();
        setupRecyclerView();
        setupSwipeRefresh();
        loadContacts();
    }

    private void initComponents() {
        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserEmail();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.contacts_title);
        }

        recyclerView = findViewById(R.id.recyclerViewContacts);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        FloatingActionButton fab = findViewById(R.id.fabAddContact);
        fab.setOnClickListener(v -> addNewContact());

        setupSearchView();
    }

    private void setupSearchView() {
        searchView = findViewById(R.id.searchView);
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (showingFavorites) {
                    filterFavoriteContacts(newText);
                } else {
                    filterContacts(newText);
                }
                return true;
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.purple_500);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshContacts();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void setupRecyclerView() {
        adapter = new ContactAdapter();
        adapter.setOnContactClickListener(this);
        adapter.setOnFavoriteClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        // Añadir scroll listener para cargar más contactos
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // Scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadMoreContacts();
                    }
                }
            }
        });
    }

    @Override
    public void onContactClick(Contact contact) {
        Intent intent = new Intent(this, AddEditContactActivity.class);
        intent.putExtra("contact_id", contact.getId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Contact contact, int position) {
        contact.setFavorite(!contact.isFavorite());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.contactDao().update(contact);
            runOnUiThread(() -> {
                adapter.notifyItemChanged(position);
                showToast(contact.isFavorite() ?
                        R.string.contact_added_to_favorites :
                        R.string.contact_removed_from_favorites);
            });
        });
    }

    private void loadContacts() {
        if (currentContactsLiveData != null) {
            currentContactsLiveData.removeObservers(this);
        }
        String orderBy = "name";
        currentContactsLiveData = db.contactDao().getAllContacts(currentUserId, orderBy);
        currentContactsLiveData.observe(this, this::updateContactsList);
    }

    private void filterContacts(String query) {
        if (currentContactsLiveData != null) {
            currentContactsLiveData.removeObservers(this);
        }

        currentContactsLiveData = db.contactDao().searchContacts(currentUserId, query, "name");
        currentContactsLiveData.observe(this, this::updateContactsList);
    }

    private void filterFavoriteContacts(String query) {
        if (currentContactsLiveData != null) {
            currentContactsLiveData.removeObservers(this);
        }
        currentContactsLiveData = db.contactDao().getFavoriteContacts(currentUserId, query);
        currentContactsLiveData.observe(this, this::updateContactsList);
    }

    private void updateContactsList(List<Contact> contacts) {
        adapter.submitList(contacts);
        updateEmptyView(contacts.isEmpty());
    }

    private void updateEmptyView(boolean isEmpty) {
        textViewEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            textViewEmpty.setText(showingFavorites ?
                    R.string.no_favorite_contacts :
                    R.string.no_contacts);
        }
    }

    private void refreshContacts() {
        String query = searchView.getQuery().toString();
        if (showingFavorites) {
            filterFavoriteContacts(query);
        } else {
            filterContacts(query);
        }
    }

    private void loadMoreContacts() {
        // Implementar paginación si es necesario
    }

    private void addNewContact() {
        Intent intent = new Intent(this, AddEditContactActivity.class);
        startActivity(intent);
    }

    private void logout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        favoriteMenuItem = menu.findItem(R.string.action_favorites);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_logout) {
            logout();
            return true;
        } else if (itemId == R.string.action_favorites) {
            toggleFavorites();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleFavorites() {
        showingFavorites = !showingFavorites;
        favoriteMenuItem.setIcon(showingFavorites ?
                R.drawable.ic_favorite :
                R.drawable.ic_favorite_border);
        searchView.setQuery("", false);
        refreshContacts();
    }
}