package com.example.pricingservice.repository;

import com.example.pricingservice.model.Price;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {

    @Query("SELECT p FROM Price p WHERE p.article.storeId = :storeId AND p.article.articleId = :articleId")
    Page<Price> findByStoreIdAndArticleId(String storeId, String articleId, Pageable pageable);

    @Query("SELECT p FROM Price p WHERE p.article.id = :articleId")
    List<Price> findAllByArticleId(Long articleId);
}