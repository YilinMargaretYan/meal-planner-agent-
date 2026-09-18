package com.diet.mapper;

import com.diet.model.MealEmbeddingRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MealEmbeddingMapper {
    List<MealEmbeddingRow> findByMealIds(@Param("mealIds") List<Long> mealIds);

    int upsert(MealEmbeddingRow row);

    int deleteByMealId(@Param("mealId") Long mealId);
}
