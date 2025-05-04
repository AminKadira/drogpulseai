package com.drogpulseai.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.drogpulseai.database.entity.ContactEntity;

import java.util.List;

@Dao
public interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ContactEntity contact);

    @Update
    void update(ContactEntity contact);

    @Delete
    void delete(ContactEntity contact);

    @Query("SELECT * FROM contacts WHERE userId = :userId AND isDeleted = 0 ORDER BY nom ASC")
    LiveData<List<ContactEntity>> getContactsByUserId(int userId);

    @Query("SELECT * FROM contacts WHERE id = :id AND isDeleted = 0")
    ContactEntity getContactById(int id);

    @Query("SELECT * FROM contacts WHERE roomId = :roomId AND isDeleted = 0")
    ContactEntity getContactByRoomId(long roomId);

    @Query("SELECT * FROM contacts WHERE isSynced = 0 AND isDeleted = 0")
    List<ContactEntity> getUnsyncedContacts();

    @Query("SELECT * FROM contacts WHERE isSynced = 0 AND isDeleted = 1")
    List<ContactEntity> getDeletedContacts();

    @Query("UPDATE contacts SET isSynced = 1 WHERE roomId = :roomId")
    void markContactAsSynced(long roomId);

    @Query("UPDATE contacts SET id = :serverId WHERE roomId = :roomId")
    void updateServerId(long roomId, int serverId);

    @Query("SELECT * FROM contacts WHERE userId = :userId AND isDeleted = 0 AND (nom LIKE '%' || :query || '%' OR prenom LIKE '%' || :query || '%' OR telephone LIKE '%' || :query || '%')")
    LiveData<List<ContactEntity>> searchContacts(int userId, String query);

    @Query("DELETE FROM contacts WHERE isDeleted = 1 AND isSynced = 1")
    void deleteAllSyncedAndDeletedContacts();
}