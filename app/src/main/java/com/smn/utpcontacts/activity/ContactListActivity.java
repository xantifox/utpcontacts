package com.smn.utpcontacts.activity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.content.Intent;
import android.widget.TextView;
import androidx.lifecycle.LiveData;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.smn.utpcontacts.R;
import com.smn.utpcontacts.adapter.ContactAdapter;
import com.smn.utpcontacts.database.AppDatabase;
import com.smn.utpcontacts.model.Contact;
import com.smn.utpcontacts.util.SessionManager;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import android.Manifest;
import android.net.Uri;
import androidx.core.app.ActivityCompat;

public class ContactListActivity extends AppCompatActivity implements
        ContactAdapter.OnContactClickListener,
        ContactAdapter.OnFavoriteClickListener,
        ContactAdapter.OnPrivacyClickListener,
        ContactAdapter.OnPhoneClickListener{

    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private TextView textViewEmpty;
    private AppDatabase db;
    private SessionManager sessionManager;
    private String currentUserId;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MenuItem favoriteMenuItem;
    private boolean showingFavorites = false;
    private LiveData<List<Contact>> currentContactsLiveData;
    private boolean showingPrivateContacts = false;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private Executor executor;
    private String currentOrderBy = "name";
    private MaterialToolbar toolbar;
    private static final int CALL_PHONE_PERMISSION_REQUEST = 102;
    private String pendingCallNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        initComponents();
        setupBiometricAuth();
        setupRecyclerView();
        setupSwipeRefresh();
        loadContacts();
    }

    private void initComponents() {
        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserEmail();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.contacts_title);
        }

        recyclerView = findViewById(R.id.recyclerViewContacts);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        FloatingActionButton fab = findViewById(R.id.fabAddContact);
        fab.setOnClickListener(v -> addNewContact());
    }

    private void setupBiometricAuth() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        runOnUiThread(() -> {
                            showingPrivateContacts = true;
                            updateVisibilityUI();
                            adapter.setShowPrivateContacts(true);
                            refreshContacts();
                            showToast(R.string.authentication_success);
                        });
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        runOnUiThread(() -> {
                            showToast(R.string.authentication_success);
                        });
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        runOnUiThread(() -> {
                            showToast(R.string.authentication_success);
                        });
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.biometric_prompt_negative))
                .build();
    }

    private void setupRecyclerView() {
        adapter = new ContactAdapter();
        adapter.setOnContactClickListener(this);
        adapter.setOnFavoriteClickListener(this);
        adapter.setOnPrivacyClickListener(this);
        adapter.setOnPhoneClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
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

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.purple_500);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshContacts();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadContacts() {
        if (currentContactsLiveData != null) {
            currentContactsLiveData.removeObservers(this);
        }

        if (showingPrivateContacts) {
            currentContactsLiveData = db.contactDao().getAllContactsAuthenticated(currentUserId, currentOrderBy);
        } else {
            currentContactsLiveData = db.contactDao().getAllContacts(currentUserId, currentOrderBy);
        }

        currentContactsLiveData.observe(this, this::updateContactsList);
    }

    private void filterContacts(String query) {
        if (currentContactsLiveData != null) {
            currentContactsLiveData.removeObservers(this);
        }

        if (showingPrivateContacts) {
            currentContactsLiveData = db.contactDao().getAllContactsAuthenticated(currentUserId, currentOrderBy);
        } else {
            currentContactsLiveData = db.contactDao().getAllContacts(currentUserId, currentOrderBy);
        }

        currentContactsLiveData.observe(this, contacts -> {
            List<Contact> filteredList;
            if (!query.isEmpty()) {
                filteredList = contacts.stream()
                        .filter(contact -> contact.getName().toLowerCase()
                                .contains(query.toLowerCase()))
                        .collect(Collectors.toList());
            } else {
                filteredList = contacts;
            }
            updateContactsList(filteredList);
        });
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
        MenuItem searchItem = toolbar.getMenu().findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        String query = searchView != null ? searchView.getQuery().toString() : "";

        if (!query.isEmpty()) {
            filterContacts(query);
        } else {
            loadContacts();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterContacts(newText);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_visibility) {
            togglePrivateContacts();
            return true;
        } else if (itemId == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void togglePrivateContacts() {
        if (!showingPrivateContacts) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            showingPrivateContacts = false;
            updateVisibilityUI();
            adapter.setShowPrivateContacts(false);
            refreshContacts();
        }
    }

    private void updateVisibilityUI() {
        MenuItem menuItem = toolbar.getMenu().findItem(R.id.action_visibility);
        if (menuItem != null) {
            menuItem.setIcon(showingPrivateContacts ?
                    R.drawable.ic_visibility_on :
                    R.drawable.ic_visibility_off);
        }
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

    @Override
    public void onPrivacyClick(Contact contact, int position) {
        if (showingPrivateContacts) {
            contact.setPrivate(!contact.isPrivate());
            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.contactDao().update(contact);
                runOnUiThread(() -> {
                    adapter.notifyItemChanged(position);
                    showToast(contact.isPrivate() ?
                            R.string.contact_made_private :
                            R.string.contact_made_public);
                });
            });
        }
    }

    private void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    public void setOrderBy(String orderBy) {
        this.currentOrderBy = orderBy;
        refreshContacts();
    }

    @Override
    public void onPhoneClick(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            pendingCallNumber = phoneNumber;
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
            showToast(R.string.call_permission_denied);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PHONE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingCallNumber != null) {
                    makePhoneCall(pendingCallNumber);
                    pendingCallNumber = null;
                }
            } else {
                showToast(R.string.call_permission_denied);
            }
        }
    }
}