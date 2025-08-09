package com.baria.bot.repository;

import com.baria.bot.model.Product;
import com.baria.bot.model.Product.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Page<Product> findByCategoryAndActiveTrue(ProductCategory category, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND :phase = ANY(p.allowedPhases)")
    List<Product> findByPhase(@Param("phase") String phase);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:phase IS NULL OR :phase = ANY(p.allowedPhases)) AND " +
           "(:allergen IS NULL OR :allergen != ALL(p.allergens))")
    Page<Product> findFiltered(@Param("category") ProductCategory category,
                               @Param("phase") String phase,
                               @Param("allergen") String allergen,
                               Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);
}
