package com.example.offlineqr.data.mapper

import com.example.offlineqr.data.entity.FolderEntity
import com.example.offlineqr.data.local.FolderWithCount
import com.example.offlineqr.domain.model.Folder

fun FolderEntity.toDomain(count: Int = 0): Folder {
    return Folder(
        id = id,
        name = name,
        createdAt = createdAt,
        itemCount = count
    )
}

fun FolderWithCount.toDomain(): Folder {
    return Folder(
        id = id,
        name = name,
        createdAt = createdAt,
        itemCount = itemCount
    )
}

fun Folder.toEntity(): FolderEntity {
    return FolderEntity(
        id = id,
        name = name,
        createdAt = createdAt
    )
}
