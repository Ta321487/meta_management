<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="${packageName}.mapper.${className}Mapper">

    <resultMap id="BaseResultMap" type="${packageName}.entity.${className}">
<#list fields as field>
        <#if field.field.fieldName == 'id'>
        <id column="<#if table.pkStrategy == 'UUID'>uuid<#else>${field.field.fieldName}</#if>" property="${field.camelCaseName}"/>
        <#else>
        <result column="${field.field.fieldName}" property="${field.camelCaseName}"/>
        </#if>
</#list>
    </resultMap>

    <#if table.pkStrategy == 'UUID'>
    <insert id="insert" parameterType="${packageName}.entity.${className}">
        INSERT INTO ${tableName} (
<#list fields as field>
            <#if field.field.fieldName == 'id'><#if table.pkStrategy == 'UUID'>uuid<#else>${field.field.fieldName}</#if><#else>${field.field.fieldName}</#if><#if field_has_next>,</#if>
</#list>
        ) VALUES (
<#list fields as field>
            ${"#{" + field.camelCaseName + "}"}<#if field_has_next>,</#if>
</#list>
        )
    </insert>
    <#else>
    <insert id="insert" parameterType="${packageName}.entity.${className}" useGeneratedKeys="true" keyProperty="<#noparse>id</#noparse>">
        INSERT INTO ${tableName} (
<#list fields as field>
            <#if field.field.fieldName != "id">${field.field.fieldName}<#if field_has_next>,</#if>
            </#if>
</#list>
        ) VALUES (
<#list fields as field>
            <#if field.field.fieldName != "id">${"#{" + field.camelCaseName + "}"}<#if field_has_next>,</#if>
            </#if>
</#list>
        )
    </insert>
    </#if>

    <update id="update" parameterType="${packageName}.entity.${className}">
        UPDATE ${tableName}
        SET
<#list fields as field>
            <#if field.field.fieldName != "id">${field.field.fieldName} = ${"#{" + field.camelCaseName + "}"}<#if field_has_next>,</#if>
            </#if>
</#list>
        WHERE <#if table.pkStrategy == 'UUID'>uuid<#else>id</#if> = <#noparse>#{id}</#noparse>
    </update>

    <delete id="deleteById">
        DELETE FROM ${tableName} WHERE <#if table.pkStrategy == 'UUID'>uuid<#else>id</#if> = <#noparse>#{id}</#noparse>
    </delete>

    <delete id="deleteByIds">
        DELETE FROM ${tableName} WHERE <#if table.pkStrategy == 'UUID'>uuid<#else>id</#if> IN
        <foreach collection="list" item="id" open="(" separator="," close=")">
            <#noparse>#{id}</#noparse>
        </foreach>
    </delete>

    <select id="selectById" resultMap="BaseResultMap">
        SELECT * FROM ${tableName} WHERE <#if table.pkStrategy == 'UUID'>uuid<#else>id</#if> = <#noparse>#{id}</#noparse>
    </select>

    <select id="selectAll" resultMap="BaseResultMap">
        SELECT * FROM ${tableName} ORDER BY <#if table.pkStrategy == 'UUID'>uuid<#else>id</#if> DESC
    </select>

    <select id="count" resultType="Long">
        SELECT COUNT(*) FROM ${tableName}
    </select>

    <!-- 通用WHERE条件片段 -->
    <sql id="whereCondition">
        <where>
            <#list fields as field>
                <#if field.field.fieldName != "id">
                    <if test="conditions != null and conditions['${field.camelCaseName}'] != null and conditions['${field.camelCaseName}'] != ''">
                        <#if field.field.fieldType?contains("varchar") || field.field.fieldType?contains("text")>
                        AND ${field.field.fieldName} LIKE CONCAT('%', <#noparse>#{conditions['</#noparse>${field.camelCaseName}<#noparse>']}</#noparse>, '%')
                        <#else>
                        AND ${field.field.fieldName} = <#noparse>#{conditions['</#noparse>${field.camelCaseName}<#noparse>']}</#noparse>
                        </#if>
                    </if>
                </#if>
            </#list>
        </where>
    </sql>

    <!-- 条件查询总数 -->
    <select id="countByCondition" resultType="Long">
        SELECT COUNT(*) FROM ${tableName}
        <include refid="whereCondition"/>
    </select>

    <!-- 分页查询（支持条件查询和排序） -->
    <select id="selectPage" resultMap="BaseResultMap">
        SELECT * FROM ${tableName}
        <include refid="whereCondition"/>
        <choose>
            <when test="orderBy != null and orderBy != ''">
                ORDER BY <#noparse>${orderBy}</#noparse> 
                <choose>
                    <when test="orderDirection != null and orderDirection == 'ASC'">ASC</when>
                    <otherwise>DESC</otherwise>
                </choose>
            </when>
            <otherwise>ORDER BY <#if table.pkStrategy == 'UUID'>uuid<#else>id</#if> DESC</otherwise>
        </choose>
        LIMIT <#noparse>#{offset}, #{size}</#noparse>
    </select>

    <!-- 条件查询（不分页，支持排序） -->
    <select id="selectByCondition" resultMap="BaseResultMap">
        SELECT * FROM ${tableName}
        <include refid="whereCondition"/>
        <choose>
            <when test="orderBy != null and orderBy != ''">
                ORDER BY <#noparse>${orderBy}</#noparse> 
                <choose>
                    <when test="orderDirection != null and orderDirection == 'ASC'">ASC</when>
                    <otherwise>DESC</otherwise>
                </choose>
            </when>
            <otherwise>ORDER BY <#if table.pkStrategy == 'UUID'>uuid<#else>id</#if> DESC</otherwise>
        </choose>
    </select>

</mapper>

