package com.smn.utpcontacts.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.UUID;

@Entity(tableName = "contacts")
public class Contact {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private long id;
    private String name;
    private String address;
    private String photoUrl;
    private String mainPhone;
    private boolean favorite;
    private String userId;
    private boolean photoChanged = false;

    public Contact(String name) {
        this.name = name;
        this.favorite = false;
    }

    public String getMainPhone() {
        return mainPhone;
    }

    public void setMainPhone(String mainPhone) {
        this.mainPhone = mainPhone;
    }

    // Getters y setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isPhotoChanged() {
        return photoChanged;
    }

    public void setPhotoChanged(boolean photoChanged) {
        this.photoChanged = photoChanged;
    }
}