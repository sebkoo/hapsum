package io.github.sebkoo.hapsum.core.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Categories are archived, never hard-deleted, once an expense references them (ADR-0003). */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isArchived: Boolean = false,
)
