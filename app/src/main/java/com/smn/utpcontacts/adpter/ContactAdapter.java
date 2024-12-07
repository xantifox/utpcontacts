package com.smn.utpcontacts.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smn.utpcontacts.R;
import com.smn.utpcontacts.model.Contact;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {
    @NonNull
    private List<Contact> contacts;
    private OnContactClickListener listener;
    private OnFavoriteClickListener favoriteListener;

    public interface OnContactClickListener {
        void onContactClick(Contact contact);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Contact contact, int position);
    }

    public ContactAdapter(@NonNull List<Contact> contacts) {
        this.contacts = contacts;
    }

    public void setOnContactClickListener(OnContactClickListener listener) {
        this.listener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteListener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.textViewName.setText(contact.getName());
        holder.textViewPhone.setText(contact.getMainPhone());
        holder.imageViewFavorite.setImageResource(
                contact.isFavorite() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContactClick(contact);
            }
        });

        holder.imageViewFavorite.setOnClickListener(v -> {
            if (favoriteListener != null) {
                favoriteListener.onFavoriteClick(contact, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void setContacts(@NonNull List<Contact> contacts) {
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    public void updateContact(Contact contact, int position) {
        contacts.set(position, contact);
        notifyItemChanged(position);
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        @NonNull final TextView textViewName;
        @NonNull final TextView textViewPhone;
        @NonNull final ImageView imageViewContact;
        @NonNull final ImageView imageViewFavorite;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewPhone = itemView.findViewById(R.id.textViewPhone);
            imageViewContact = itemView.findViewById(R.id.imageViewContact);
            imageViewFavorite = itemView.findViewById(R.id.imageViewFavorite);
        }
    }
}