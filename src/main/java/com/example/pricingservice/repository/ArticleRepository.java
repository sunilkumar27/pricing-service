package com.example.pricingservice.repository;

import com.example.pricingservice.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    @Query("SELECT a FROM Article a WHERE a.storeId = :storeId AND a.articleId = :articleId")
    Optional<Article> findByStoreIdAndArticleId(String storeId, String articleId);
}