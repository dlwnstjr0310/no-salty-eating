package com.study.order.domain.model

import au.com.console.kassava.kotlinToString
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.io.Serializable
import java.time.LocalDateTime

open class BaseEntity(
    var isDeleted: Boolean = false,
    var isPublic: Boolean = true,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
) : Serializable {
    override fun toString(): String {
        return kotlinToString(arrayOf(
            BaseEntity::isDeleted,
            BaseEntity::isPublic,
            BaseEntity::createdAt,
            BaseEntity::updatedAt
        ))
    }
}