package com.baria.bot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "product")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductCategory category;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private BigDecimal price;
    
    private String url;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_phases", columnDefinition = "text[]")
    private String[] allowedPhases;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allergens", columnDefinition = "text[]")
    private String[] allergens;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum ProductCategory {
        SETS("sets", "Наборы еды для диет"),
        MASKS("masks", "Маски"),
        SHAMPOOS("shampoos", "Шампуни"),
        CREAMS("creams", "Кремы"),
        SUPPS("supps", "Витамины и добавки");
        
        private final String code;
        private final String displayName;
        
        ProductCategory(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
