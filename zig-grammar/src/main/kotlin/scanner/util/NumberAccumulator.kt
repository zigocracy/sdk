package net.landless_city.zigocracy.zig.scanner.util

import net.landless_city.zigocracy.zig.scanner.ScanDiagnostics
import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.text.CodeUnits
import net.landless_city.zigocracy.zig.text.TextReader

/**
 * Greedily accumulates alphanumeric character streams for numeric literals.
 *
 * Examples of captured malformed inputs:
 * - "0b1029" -> Entire block is consumed as one token, preventing "29" from splitting into a separate Identifier.
 * - "0xG12"  -> Trailing garbage characters are bound locally to isolate lexical failure scope.
 *
 * Validation of legal digits and underscore positioning is deferred to a downstream pass.
 */
internal class NumberAccumulator(
	private val reader: TextReader,
	private val base: NumberBase,
	startWidth: CodeUnits
) {
	var currentWidth: CodeUnits = startWidth
		private set

	var isFloat: Boolean = false
		private set

	private var hasDecimalPoint: Boolean = false

	fun accumulate(diagnostics: MutableList<ScanDiagnostics>) {
		while (true) {
			val c = reader.peekChar(currentWidth) ?: break

			when {
				// Matches standard alphanumeric chunks and underscores.
				c.isZigNumberBodyPart() -> {

					// Examples of exponent markers:
					// - "1e-5"   -> 'e' flags a Decimal float exponent.
					// - "0x1p+2" -> 'p' flags a Hexadecimal float exponent.
					// - "0xf_e"  -> 'e' inside Hex is treated as a plain digit (14), not an exponent marker.
					if (base.isExponentMarker(c)) {
						isFloat = true

						// Examples of signed exponent scales:
						// - "1e+10" -> Atomic consumption of '+' via base prevents its separation as a binary operator.
						// - "1e-5"  -> Atomic consumption of '-' via base prevents its separation as a binary operator.
						// - "1e4"   -> No sign detected; signStep is 0, advancing pointer by 1 character.
						val signStep = base.consumeExponentSign(reader, currentWidth)
						currentWidth += 1 + signStep
						continue
					}
					currentWidth++
				}

				// Separates literal fractional dots from grammatical dot-operators.
				c == '.' -> {
					// Examples of radix float compatibility:
					// - "1.5"   -> Decimal base allows dot execution path.
					// - "0b1.1" -> Binary base rejects float; immediately breaks out to let operator lexers step in.
					if (!base.supportsFloat()) {
						break
					}

					val next = reader.peekChar(currentWidth + 1)

					// Examples of dot routing situations:
					// - "1.0"   -> Followed by regular fraction start; belongs to the number.
					// - "1.\n"  -> Followed by newline/EOF boundary; marks expression-ending float.
					// - "1.."   -> Followed by second dot; terminates number token immediately before the first dot to preserve the range operator.
					// - "1.*"   -> Followed by asterisks; terminates number token immediately before the dot to preserve pointer dereference operator.
					val belongsToNumber = when {
						next == null || next.isZigHorizontalWhitespace() || next.isZigVerticalWhitespace() -> true
						next.isZigValidFloatFractionStart() -> true
						else -> false
					}

					if (!belongsToNumber) {
						break
					}

					// Examples of multiple fractional points:
					// - "1.2.3" -> Second dot triggers a diagnostic error but remains consumed to preserve stable token width.
					if (hasDecimalPoint) {
						diagnostics.add(
							ScanDiagnostics(
								code = DiagnosticCode.NumberError.MultipleDecimalPoints,
								offset = currentWidth,
								width = 1
							)
						)
					}

					isFloat = true
					hasDecimalPoint = true
					currentWidth++
				}

				else -> break
			}
		}
	}
}