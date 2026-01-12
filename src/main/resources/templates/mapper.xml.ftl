<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<!-- ${table.tableName} Mapper XML -->
<!-- 业务系统：${businessName} -->
<mapper namespace="${packageName}.mapper.${className}Mapper">

    <resultMap id="BaseResultMap" type="${packageName}.entity.${className}">
        <#list fields as field>
            <#if field.field.formComponent == 'primary_key'>
            <id column="<#if table.pkStrategy == 'UUID'>uuid<#else>${field.field.fieldName}</#if>" property="${field.camelCaseName}"/>
            </#if>
        </#list>
        <#list fields as field>
            <#if field.field.fieldName != 'id'>
            <result column="${field.field.fieldName}" property="${field.camelCaseName}"/>
            </#if>
        </#list>
        
        <#-- 添加关联实体映射 -->
        <#list fields as field>
        <#if field.isForeignKey!false>
            <association property="${field.relatedTableCamelCaseName}" javaType="${packageName}.entity.${field.relatedTableClassName}" fetchType="lazy">
                <id column="${field.relatedTableCamelCaseName}_id" property="id"/>
                <#-- 这里只映射id字段，需要在关联查询时添加其他字段的映射 -->
            </association>
        </#if>
        </#list>
    </resultMap>
    
    <#-- 关联查询结果映射 -->
    <resultMap id="AssociationResultMap" type="${packageName}.entity.${className}" extends="BaseResultMap">
        <#list fields as field>
        <#if field.isForeignKey!false>
            <association property="${field.relatedTableCamelCaseName}" javaType="${packageName}.entity.${field.relatedTableClassName}" fetchType="lazy">
                <id column="${field.relatedTableCamelCaseName}_id" property="id"/>
                <result column="${field.relatedTableCamelCaseName}_${field.relatedTableFieldName}" property="${field.relatedTableFieldName}"/>
                <#-- 可以根据需要添加更多关联实体字段映射 -->
            </association>
        </#if>
        </#list>
    </resultMap>

    <#if table.pkStrategy == 'UUID'>
    <insert id="insert" parameterType="${packageName}.entity.${className}">
        INSERT INTO ${tableName} (
            uuid
            <#list fields as field>
                <#if field.field.fieldName != 'id'>
                    ,${field.field.fieldName}
                </#if>
            </#list>
        ) VALUES (
            <#noparse>#{id}</#noparse>
            <#list fields as field>
                <#if field.field.fieldName != 'id'>
                    ,${"#{" + field.camelCaseName + "}"}
                </#if>
            </#list>
        )
    </insert>
    <#else>
    <insert id="insert" parameterType="${packageName}.entity.${className}" useGeneratedKeys="true" keyProperty="<#noparse>id</#noparse>">
        INSERT INTO ${tableName} (
            <#list fields as field>
                <#if field.field.fieldName != "id">
                    ${field.field.fieldName}<#sep>,</#sep>
                </#if>
            </#list>
        ) VALUES (
            <#list fields as field>
                <#if field.field.fieldName != "id">
                    ${"#{" + field.camelCaseName + "}"}<#sep>,</#sep>
                </#if>
            </#list>
        )
    </insert>
    </#if>

    <update id="update" parameterType="${packageName}.entity.${className}">
        UPDATE ${tableName}
        SET
            <#list fields as field>
                <#if field.field.fieldName != "id">
                    ${field.field.fieldName} = ${"#{" + field.camelCaseName + "}"}<#sep>,</#sep>
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