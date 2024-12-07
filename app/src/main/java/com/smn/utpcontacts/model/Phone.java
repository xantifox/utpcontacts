package com.smn.utpcontacts.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import java.util.UUID;

@Entity(
        tableName = "phones",
        foreignKeys = @ForeignKey(
                entity = Contact.class,
                parentColumns = "id",
                childColumns = "contactId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("contactId")}
)
public class Phone {
    @PrimaryKey
    @NonNull
    private String id;
    private String number;
    private String type;
    private String contactId;  // Foreign key

    public Phone(String number, String type, String contactId) {
        this.id = UUID.randomUUID().toString();
        this.number = number;
        this.type = type;
        this.contactId = contactId;
    }

    // Getters y setters
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContactId() { return contactId; }
    public void setContactId(String contactId) { this.contactId = contactId; }
}