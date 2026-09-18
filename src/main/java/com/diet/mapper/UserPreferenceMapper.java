package com.diet.mapper;

import com.diet.model.UserPreferenceRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserPreferenceMapper {
    List<UserPreferenceRow> findActiveByUserId(@Param("userId") Long userId);

    int upsert(UserPreferenceRow row);

    UserPreferenceRow findByUnique(
            @Param("userId") Long userId,
            @Param("slotName") String slotName,
            @Param("optionValue") String optionValue,
            @Param("preferenceType") String preferenceType
    );

    int deleteById(@Param("id") Long id, @Param("userId") Long userId);
}
