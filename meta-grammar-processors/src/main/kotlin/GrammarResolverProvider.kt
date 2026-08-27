package com.zigocracy.sdk.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.zigocracy.sdk.processor.emitters.LexerRegistryEmitter
import com.zigocracy.sdk.processor.emitters.ParserRegistryEmitter

/**
 * KSP entry point that assembles the [GrammarResolver] with all enabled [GrammarEmitter]s.
 *
 * This replaces the previous `LexerProcessorProvider` and `ParserProcessorProvider`
 * with a single provider. The META-INF services file should reference only this class:
 *
 * ```
 * com.zigocracy.sdk.processor.GrammarResolverProvider
 * ```
 *
 * ## Emitter registration
 *
 * To add a new emitter, instantiate it in [create] and append it to the `emitters` list.
 * Each emitter receives the shared [CodeGenerator][com.google.devtools.ksp.processing.CodeGenerator]
 * and (optionally) the [KSPLogger][com.google.devtools.ksp.processing.KSPLogger].
 */
public class GrammarResolverProvider : SymbolProcessorProvider {

	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
		val codeGen = environment.codeGenerator
		val logger = environment.logger

		val emitters: List<GrammarEmitter> = listOf(
			LexerRegistryEmitter(codeGen),
			ParserRegistryEmitter(codeGen),
			// Future possibilites:
			// DfaLexerEmitter(codeGen),
			// MarkdownDocsEmitter(codeGen),
			// TextMateGrammarEmitter(codeGen),
			// LspSemanticTokensEmitter(codeGen),
		)

		return GrammarResolver(logger, emitters)
	}
}