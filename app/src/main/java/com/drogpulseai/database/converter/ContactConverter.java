package com.drogpulseai.database.converter;

import com.drogpulseai.database.entity.ContactEntity;
import com.drogpulseai.models.Contact;

import java.util.ArrayList;
import java.util.List;

public class ContactConverter {

    public static ContactEntity toEntity(Contact contact) {
        return new ContactEntity(
                contact.getId(),
                contact.getNom(),
                contact.getPrenom(),
                contact.getTelephone(),
                contact.getEmail(),
                contact.getNotes(),
                contact.getType(),
                contact.getLatitude(),
                contact.getLongitude(),
                contact.getUserId()
        );
    }

    public static Contact toModel(ContactEntity entity) {
        Contact contact = new Contact(
                entity.getNom(),
                entity.getPrenom(),
                entity.getTelephone(),
                entity.getEmail(),
                entity.getNotes(),
                entity.getType(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getUserId()
        );
        contact.setId(entity.getId());
        return contact;
    }

    public static List<Contact> toModelList(List<ContactEntity> entities) {
        List<Contact> contacts = new ArrayList<>();
        for (ContactEntity entity : entities) {
            if (!entity.isDeleted()) {
                contacts.add(toModel(entity));
            }
        }
        return contacts;
    }
}