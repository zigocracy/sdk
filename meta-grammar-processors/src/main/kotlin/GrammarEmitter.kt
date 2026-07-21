package net.landless_city.zigocracy.processor

/**
 * A code-generation module that transforms a [net.landless_city.zigocracy.processor.ResolvedGrammar] into a build artifact.
 *
 * Every emitter is a pure function of the grammar model: it reads the resolved metadata
 * and writes one or more files through the KSP [CodeGenerator][com.google.devtools.ksp.processing.CodeGenerator].
 * Emitters never interact with KSP's symbol-resolution API and can therefore be tested
 * against hand-constructed [net.landless_city.zigocracy.processor.ResolvedGrammar] instances without a running compiler.
 *
 * ## Adding a new emitter
 *
 * 1. Implement this interface.
 * 2. Register the emitter in [net.landless_city.zigocracy.processor.GrammarResolver.emitters].
 * 3. The resolver will call [emit] automatically after successful validation.
 *
 * ## Conventions
 *
 * - Each emitter should generate exactly **one** top-level file (object, class, or resource).
 * - Generated files should be `internal` unless there is an explicit reason to expose them.
 * - Generated files should carry a KDoc banner stating they are auto-generated.
 *
 * @see net.landless_city.zigocracy.processor.GrammarResolver
 * @see net.landless_city.zigocracy.processor.ResolvedGrammar
 */
public interface GrammarEmitter {

	/**
	 * A short human-readable name for logging and diagnostics (e.g. `"LexerRegistry"`, `"DfaScanner"`).
	 */
	public val name: String

	/**
	 * Generates one or more build artifacts from the resolved grammar.
	 *
	 * Called by the [net.landless_city.zigocracy.processor.GrammarResolver] after the model has been validated.
	 * Implementations should not throw; instead, report issues through the
	 * KSP logger passed at construction time.
	 */
	public fun emit(grammar: ResolvedGrammar)
}