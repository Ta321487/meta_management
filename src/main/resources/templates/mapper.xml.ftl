<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="${packageName}.mapper.${className}Mapper">

    <resultMap id="BaseResultMap" type="${packageName}.entity.${className}">
<#list fields as field>
        <#if field.field.fieldName == "id">
        <id column="${field.field.fieldName}" property="${field.camelCaseName}"/>
        <#else>
        <result column="${field.field.fieldName}" property="${field.camelCaseName}"/>
        </#if>
</#list>
    </resultMap>

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

    <update id="update" parameterType="${packageName}.entity.${className}">
        UPDATE ${tableName}
        SET
<#list fields as field>
            <#if field.field.fieldName != "id">${field.field.fieldName} = ${"#{" + field.camelCaseName + "}"}<#if field_has_next>,</#if>
            </#if>
</#list>
        WHERE id = <#noparse>#{id}</#noparse>
    </update>

    <delete id="deleteById">
        DELETE FROM ${tableName} WHERE id = <#noparse>#{id}</#noparse>
    </delete>

    <select id="selectById" resultMap="BaseResultMap">
        SELECT * FROM ${tableName} WHERE id = <#noparse>#{id}</#noparse>
    </select>

    <select id="selectAll" resultMap="BaseResultMap">
        SELECT * FROM ${tableName} ORDER BY id DESC
    </select>

</mapper>

