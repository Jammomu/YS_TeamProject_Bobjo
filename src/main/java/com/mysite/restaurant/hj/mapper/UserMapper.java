package com.mysite.restaurant.hj.mapper;

import com.mysite.restaurant.hj.domain.entity.UserAuthEntity;
import com.mysite.restaurant.hj.domain.entity.UserEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO users (user_name, password, name, email, phone) " +
            "VALUES (#{userName}, #{password}, #{name}, #{email}, #{phone})")
    @Options(useGeneratedKeys = true, keyProperty = "userId", keyColumn = "user_id")
    long insertUser(UserEntity userEntity);

    @Insert("INSERT INTO user_auth (user_id, auth) values (#{userId}, #{auth})")
    void insertUserAuth(UserAuthEntity userAuthEntity);

    @Select("SELECT * FROM user_auth WHERE user_id = #{userId}")
    List<UserAuthEntity> findUserAuth(long userId);

    //아이디랑 비밀번호 유효성 검사
    @Select("SELECT * FROM users WHERE user_id = #{userId}")
    Optional<UserEntity> findUserByUserId(@Param("userId") int userId);

    //회원가입 완료시 나오는페이지 정보 가져오는
    @Select("SELECT * FROM users WHERE user_name = #{userName}")
    Optional<UserEntity> findUserByUserName(@Param("userName") String userName);

    //관리자페이지에서 회원조회
    @Select("SELECT * FROM users ORDER BY user_id DESC")
    List<Map<String, Object>> findAllUsers();

}