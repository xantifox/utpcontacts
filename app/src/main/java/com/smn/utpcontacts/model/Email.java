package com.smn.utpcontacts.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(
        tableName = "emails",
        foreignKeys = @ForeignKey(
                entity = Contact.class,
                parentColumns = "id",
                childColumns = "contactId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("contactId"),
                @Index(value = {"contactId", "address"}, unique = true)
        }
)
public class Email {
    public static class Type {
        public static final String PERSONAL = "PERSONAL";
        public static final String WORK = "WORK";
        public static final String OTHER = "OTHER";

        public static boolean isValid(String type) {
            return type != null && (
                    type.equals(PERSONAL) ||
                            type.equals(WORK) ||
                            type.equals(OTHER)
            );
        }
    }

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String address;

    @NonNull
    private String type;

    private long contactId;

    private boolean isPrimary;
    private String label;
    private long createdAt;
    private long updatedAt;

    public Email(@NonNull String address, @NonNull String type, long contactId) {
        if (!Type.isValid(type)) {
            throw new IllegalArgumentException("Invalid email type");
        }

        this.address = address.toLowerCase();
        this.type = type;
        this.contactId = contactId;
        this.isPrimary = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    // Getters y setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    public void setAddress(@NonNull String address) {
        this.address = address.toLowerCase();
        this.updatedAt = System.currentTimeMillis();
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        if (!Type.isValid(type)) {
            throw new IllegalArgumentException("Invalid email type");
        }
        this.type = type;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        this.isPrimary = primary;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}