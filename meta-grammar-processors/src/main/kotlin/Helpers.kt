package com.zigocracy.sdk.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Collects all enum entries from an `@GrammarRoot`-annotated enum class.
 *
 * Unlike the previous sealed-class design (which traversed `getSealedSubclasses()`),
 * enum entries are direct children of the enum class. Only entries whose
 * [ClassKind] is [ClassKind.ENUM_ENTRY] are included.
 */
internal fun resolveEnumEntries(root: KSClassDeclaration): List<KSClassDeclaration> {
	return root.declarations
		.filterIsInstance<KSClassDeclaration>()
		.filter { it.classKind == ClassKind.ENUM_ENTRY }
		.toList()
}

@OptIn(KspExperimental::class)
internal inline fun <reified T : Annotation> KSClassDeclaration.getAnnotationsByType(): List<T> =
	this.getAnnotationsByType(T::class).toList()
