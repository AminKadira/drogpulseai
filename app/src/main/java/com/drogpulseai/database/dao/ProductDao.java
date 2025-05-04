package com.drogpulseai.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.drogpulseai.database.ProductEntity;

import java.util.List;

@Dao
public interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertProduct(ProductEntity product);

    @Update
    int updateProduct(ProductEntity product);

    @Delete
    void deleteProduct(ProductEntity product);

    @Query("SELECT * FROM products WHERE userId = :userId")
    LiveData<List<ProductEntity>> getProductsByUserId(int userId);

    @Query("SELECT * FROM products WHERE id = :id")
    LiveData<ProductEntity> getProductById(int id);

    @Query("SELECT * FROM products WHERE isDirty = 1")
    List<ProductEntity> getDirtyProducts();

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    ProductEntity getProductByBarcode(String barcode);

    @Query("UPDATE products SET isDirty = 0 WHERE id = :id")
    void markProductAsSynced(int id);

    @Query("DELETE FROM products WHERE id = :id")
    void deleteProductById(int id);

    @Query("SELECT * FROM products WHERE userId = :userId AND (name LIKE '%' || :query || '%' OR reference LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%')")
    LiveData<List<ProductEntity>> searchProducts(int userId, String query);
}