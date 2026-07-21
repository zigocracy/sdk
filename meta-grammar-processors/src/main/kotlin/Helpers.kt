package net.landless_city.zigocracy.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * Recursively collects all singleton `object` leaves in a sealed hierarchy.
 *
 * Sealed classes and sealed interfaces are traversed recursively; only concrete
 * `object` declarations are included in the result. Non-sealed, non-object
 * subtypes (e.g. `data class`) are silently skipped.
 */
internal fun resolveObjectChildrenOfSealed(root: KSClassDeclaration): List<KSClassDeclaration> {
	val tokenKinds: List<KSClassDeclaration> = buildList {
		fun visit(node: KSClassDeclaration) {
			for (sub in node.getSealedSubclasses()) {
				when {
					sub.classKind == ClassKind.OBJECT ->
						add(sub)

					sub.modifiers.contains(Modifier.SEALED) ->
						visit(sub)

					else -> {}
				}
			}
		}
		visit(root)
	}

	return tokenKinds
}

@OptIn(KspExperimental::class)
internal inline fun <reified T : Annotation> KSClassDeclaration.getAnnotationsByType(): List<T> =
	this.getAnnotationsByType(T::class).toList()