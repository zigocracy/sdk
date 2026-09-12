package com.zigocracy.sdk.engine.diagnostics

import com.zigocracy.sdk.engine.ParsedFile
import com.zigocracy.sdk.engine.WorkspaceContext
import com.zigocracy.sdk.engine.module.ImportKind
import com.zigocracy.sdk.engine.vfs.VfsResult
import com.zigocracy.sdk.zig.lexer.TokenDiagnostic
import com.zigocracy.sdk.zig.syntax.*
import java.nio.file.Files

/**
 * Traverses a syntax stream via [SyntaxStreamVisitor] to detect and validate
 * `@import(...)` expressions against workspace files and modules.
 */
class ImportAnalysisVisitor(
	private val parsedFile: ParsedFile,
	private val workspaceContext: WorkspaceContext,
) : SyntaxStreamVisitor {
	private val diagnostics = mutableListOf<EngineDiagnostic>()
	private var currentAbsoluteOffset = 0

	private enum class State {
		Normal,
		SawImportBuiltin,
		SawLeftParen,
	}

	private var state = State.Normal

	override fun enterNode(index: Int, event: NodeEvent): Boolean = true

	override fun leaveNode(index: Int, event: NodeEvent) {}

	override fun visitToken(index: Int, event: TokenEvent, diagnostics: List<TokenDiagnostic>) {
		val tokenOffset = currentAbsoluteOffset
		currentAbsoluteOffset += event.width

		if (isTrivia(event.kind)) {
			return
		}

		when (state) {
			State.Normal -> {
				if (event.kind == TokenKind.BuiltinIdentifier) {
					val tokenText = parsedFile.source.getTextSlice(tokenOffset, event.width)
					if (tokenText == "@import") {
						state = State.SawImportBuiltin
					}
				}
			}

			State.SawImportBuiltin -> {
				if (event.kind == TokenKind.LeftParen) {
					state = State.SawLeftParen
				} else {
					state = State.Normal
				}
			}

			State.SawLeftParen -> {
				state = State.Normal
				if (event.kind == TokenKind.StringLiteral) {
					val rawText = parsedFile.source.getTextSlice(tokenOffset, event.width)
					if (rawText.length >= 2 && rawText.startsWith("\"") && rawText.endsWith("\"")) {
						val target = rawText.substring(1, rawText.length - 1)
						analyzeImportTarget(target, tokenOffset, event.width)
					}
				}
			}
		}
	}

	private fun isTrivia(kind: TokenKind): Boolean {
		return when (kind.classifyToVisualGroup()) {
			VisualGroup.Whitespace, VisualGroup.Newline, VisualGroup.Comment, VisualGroup.DocComment -> true
			else -> false
		}
	}

	private fun analyzeImportTarget(target: String, tokenOffset: Int, width: Int) {
		val kind = ImportKind.classify(target)
		when (kind) {
			is ImportKind.InvalidFilePath -> {
				diagnostics.add(
					EngineDiagnostic(
						code = DiagnosticCode.Import.InvalidPath,
						startOffset = tokenOffset,
						width = width,
						message = "Invalid import path '$target': ${kind.reason}",
					)
				)
			}

			is ImportKind.File -> {
				val parentDir = parsedFile.source.path.parent
				if (parentDir != null) {
					val resolvedPath = parentDir.resolve(kind.path).normalize()
					val existsInVfs = workspaceContext.vfs.acquire(resolvedPath) is VfsResult.Success
					val existsOnDisk = try {
						Files.isRegularFile(resolvedPath)
					} catch (_: Exception) {
						false
					}

					if (!existsInVfs && !existsOnDisk) {
						diagnostics.add(
							EngineDiagnostic(
								code = DiagnosticCode.Import.FileNotFound,
								startOffset = tokenOffset,
								width = width,
								message = "Imported file not found: '$target'",
								note = "Resolved path: $resolvedPath",
							)
						)
					}
				}
			}

			is ImportKind.Module -> {
				if (kind.name != "std" && kind.name != "builtin") {
					val modules = workspaceContext.moduleGraph.getAllModules()
					if (modules.isNotEmpty()) {
						val mod = workspaceContext.moduleGraph.findModuleByName(kind.name)
						if (mod == null) {
							diagnostics.add(
								EngineDiagnostic(
									code = DiagnosticCode.Import.ModuleNotFound,
									startOffset = tokenOffset,
									width = width,
									message = "Module '${kind.name}' not found in workspace dependencies",
								)
							)
						}
					}
				}
			}
		}
	}

	fun getDiagnostics(): List<EngineDiagnostic> = diagnostics
}
