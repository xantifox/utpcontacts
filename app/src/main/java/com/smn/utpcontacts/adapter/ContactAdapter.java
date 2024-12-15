package com.smn.utpcontacts.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.smn.utpcontacts.R;
import com.smn.utpcontacts.model.Contact;

public class ContactAdapter extends ListAdapter<Contact, ContactAdapter.ContactViewHolder> {
    private OnContactClickListener listener;
    private OnFavoriteClickListener favoriteListener;
    private OnPrivacyClickListener privacyListener;
    private OnPhoneClickListener phoneListener;
    private boolean showPrivateContacts = false;

    public interface OnContactClickListener {
        void onContactClick(Contact contact);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Contact contact, int position);
    }

    public interface OnPrivacyClickListener {
        void onPrivacyClick(Contact contact, int position);
    }

    private static final DiffUtil.ItemCallback<Contact> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Contact>() {
                @Override
                public boolean areItemsTheSame(@NonNull Contact oldItem, @NonNull Contact newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Contact oldItem, @NonNull Contact newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                            oldItem.getMainPhone().equals(newItem.getMainPhone()) &&
                            oldItem.isFavorite() == newItem.isFavorite() &&
                            oldItem.isPrivate() == newItem.isPrivate() &&
                            TextUtils.equals(oldItem.getPhotoUrl(), newItem.getPhotoUrl());
                }
            };

    public ContactAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnContactClickListener(OnContactClickListener listener) {
        this.listener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteListener = listener;
    }

    public void setOnPrivacyClickListener(OnPrivacyClickListener listener) {
        this.privacyListener = listener;
    }

    public void setShowPrivateContacts(boolean show) {
        this.showPrivateContacts = show;
        notifyDataSetChanged();
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
        Contact contact = getItem(position);
        holder.bind(contact, showPrivateContacts);
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ShapeableImageView imageViewContact;
        private final TextView textViewName;
        private final TextView textViewPhone;
        private final ImageView imageViewFavorite;
        private final ImageView imageViewPrivate;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            imageViewContact = itemView.findViewById(R.id.imageViewContact);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewPhone = itemView.findViewById(R.id.textViewPhone);
            imageViewFavorite = itemView.findViewById(R.id.imageViewFavorite);
            imageViewPrivate = itemView.findViewById(R.id.imageViewPrivate);
        }

        void bind(final Contact contact, boolean showPrivateContacts) {
            // Configurar textos
            textViewName.setText(contact.getName());
            textViewPhone.setText(formatPhoneNumber(contact.getMainPhone()));

            // Cargar imagen de contacto
            if (!TextUtils.isEmpty(contact.getPhotoUrl())) {
                Glide.with(itemView.getContext())
                        .load(contact.getPhotoUrl())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(imageViewContact);
            } else {
                imageViewContact.setImageResource(R.drawable.ic_person);
            }

            // Configurar estado de favorito
            updateFavoriteState(contact);

            // Configurar privacidad
            imageViewPrivate.setVisibility(showPrivateContacts ? View.VISIBLE : View.GONE);
            if (showPrivateContacts) {
                imageViewPrivate.setImageResource(
                        contact.isPrivate() ? R.drawable.ic_lock_closed : R.drawable.ic_lock_open
                );

                // Añadir animación al cambiar el estado de privacidad
                if (contact.isPrivate()) {
                    imageViewPrivate.animate()
                            .scaleX(1.2f)
                            .scaleY(1.2f)
                            .setDuration(150)
                            .withEndAction(() ->
                                    imageViewPrivate.animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(150)
                                            .start()
                            ).start();
                }
            }

            // Configurar clicks
            setupClickListeners(contact);

            // Configurar el click del teléfono
            textViewPhone.setOnClickListener(v -> {
                if (getBindingAdapter() instanceof ContactAdapter) {
                    ContactAdapter adapter = (ContactAdapter) getBindingAdapter();
                    if (adapter.phoneListener != null && contact.getMainPhone() != null
                            && !contact.getMainPhone().isEmpty()) {
                        adapter.phoneListener.onPhoneClick(contact.getMainPhone());
                    }
                }
            });
        }

        private void updateFavoriteState(Contact contact) {
            imageViewFavorite.setImageResource(
                    contact.isFavorite() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border
            );

            // Animación de favorito
            if (contact.isFavorite()) {
                imageViewFavorite.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(150)
                        .withEndAction(() ->
                                imageViewFavorite.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(150)
                                        .start()
                        ).start();
            }
        }

        private void setupClickListeners(final Contact contact) {
            cardView.setOnClickListener(v -> {
                if (getBindingAdapter() instanceof ContactAdapter) {
                    ContactAdapter adapter = (ContactAdapter) getBindingAdapter();
                    if (adapter.listener != null) {
                        adapter.listener.onContactClick(contact);
                    }
                }
            });

            imageViewFavorite.setOnClickListener(v -> {
                if (getBindingAdapter() instanceof ContactAdapter) {
                    ContactAdapter adapter = (ContactAdapter) getBindingAdapter();
                    if (adapter.favoriteListener != null) {
                        adapter.favoriteListener.onFavoriteClick(contact, getBindingAdapterPosition());
                    }
                }
            });

            imageViewPrivate.setOnClickListener(v -> {
                if (getBindingAdapter() instanceof ContactAdapter) {
                    ContactAdapter adapter = (ContactAdapter) getBindingAdapter();
                    if (adapter.privacyListener != null) {
                        adapter.privacyListener.onPrivacyClick(contact, getBindingAdapterPosition());
                    }
                }
            });
        }

        private String formatPhoneNumber(String phone) {
            if (TextUtils.isEmpty(phone)) return "";

            // Eliminar caracteres no numéricos
            String cleaned = phone.replaceAll("[^\\d]", "");

            // Formato para números peruanos
            if (cleaned.length() == 9) {
                return String.format("+51 %s %s %s",
                        cleaned.substring(0, 3),
                        cleaned.substring(3, 6),
                        cleaned.substring(6));
            }

            return phone;
        }
    }

    public interface OnPhoneClickListener {
        void onPhoneClick(String phoneNumber);
    }

    public void setOnPhoneClickListener(OnPhoneClickListener listener) {
        this.phoneListener = listener;
    }
}