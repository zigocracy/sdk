package net.landless_city.zigocracy.zon

import net.landless_city.zigocracy.zon.ZonAstNode.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Parse a ZON string and return the AST node on success.
 */
private fun parseZon(source: String): ZonAstNode {
	val lexed = ZonLexer(source).tokenize()
	val tokens = when (lexed) {
		is LexResult.Success -> lexed.tokens
		is LexResult.Error -> throw AssertionError("Lex failed: ${lexed.diagnostic}")
	}
	val parsed = ZonParser(tokens, source).parse()
	return when (parsed) {
		is ParseResult.Success -> parsed.node
		is ParseResult.Error -> throw AssertionError("Parse failed: ${parsed.diagnostic}")
	}
}

/**
 * Assert that parsing [source] produces a [LexResult.Error] or [ParseResult.Error].
 */
private fun assertParseError(source: String) {
	val lexed = ZonLexer(source).tokenize()
	when (lexed) {
		is LexResult.Error -> return
		is LexResult.Success -> {
			val parsed = ZonParser(lexed.tokens, source).parse()
			assertInstanceOf(ParseResult.Error::class.java, parsed)
		}
	}
}

class ZonLexerParserTest {

	// region Primitives

	@Test
	fun `booleans true and false`() {
		assertAll(
			{ assertEquals(TrueVal, parseZon("true")) },
			{ assertEquals(FalseVal, parseZon("false")) },
		)
	}

	@Test
	fun `null keyword`() {
		assertEquals(NullVal, parseZon("null"))
	}

	@Test
	fun `character literals`() {
		assertAll(
			{ assertEquals(CharVal(122), parseZon("'z'")) },
			{ assertEquals(CharVal(9), parseZon("'\\t'")) },
			{ assertEquals(CharVal(127), parseZon("'\\x7F'")) },
			{ assertEquals(CharVal(9733), parseZon("'\\u{2605}'")) },
		)
	}

	@Test
	fun `integer literals`() {
		assertAll(
			{ assertEquals(IntVal(BigInteger.ZERO), parseZon("0")) },
			{ assertEquals(IntVal(BigInteger("8675309")), parseZon("8675309")) },
			{ assertEquals(IntVal(BigInteger("-404")), parseZon("-404")) },
			{ assertEquals(IntVal(BigInteger("99999999")), parseZon("99_999_999")) },

			{ assertEquals(IntVal(BigInteger("CAFEBABE", 16)), parseZon("0xCAFE_BABE")) },
			{ assertEquals(IntVal(BigInteger("-26")), parseZon("-0X1A")) },

			{ assertEquals(IntVal(BigInteger("644", 8)), parseZon("0o644")) },
			{ assertEquals(IntVal(BigInteger("-7")), parseZon("-0O7")) },

			{ assertEquals(IntVal(BigInteger("11001100", 2)), parseZon("0b1100_1100")) },
			{ assertEquals(IntVal(BigInteger("-1")), parseZon("-0B1")) },
		)
	}

	@Test
	fun `float literals`() {
		assertAll(
			{ assertEquals(FloatVal(BigDecimal("0.0")), parseZon("0.0")) },
			{ assertEquals(FloatVal(BigDecimal("98.6")), parseZon("98.6")) },
			{ assertEquals(FloatVal(BigDecimal("-0.001")), parseZon("-0.001")) },
			{ assertEquals(FloatVal(BigDecimal("1e-5")), parseZon("1e-5")) },
			{ assertEquals(FloatVal(BigDecimal("-3.14E+2")), parseZon("-3.14E+2")) },
			{ assertEquals(FloatVal(BigDecimal.valueOf("0x1.fP3".toDouble())), parseZon("0x1.fP3")) },
			{ assertEquals(FloatVal(BigDecimal.valueOf("-0X0.1p-2".toDouble())), parseZon("-0X0.1p-2")) },
		)
	}

	@Test
	fun `special float constants`() {
		assertAll(
			{ assertEquals(InfVal(false), parseZon("inf")) },
			{ assertEquals(InfVal(true), parseZon("-inf")) },
			{ assertInstanceOf(NanVal::class.java, parseZon("nan")) },
		)
	}

	@Test
	fun `single-line strings`() {
		val emptyZon = "\"\""
		assertAll(
			{ assertEquals(SingleString(""), parseZon(emptyZon)) },
			{ assertEquals(SingleString("abc"), parseZon("\"abc\"")) },
			{ assertEquals(SingleString("a\nb"), parseZon("\"a\\nb\"")) },
			{ assertEquals(SingleString("\t"), parseZon("\"\\t\"")) },
		)
	}

	@Test
	fun `multi-line strings`() {
		val result = parseZon(
			"""\\line one
\\line two
\\"""
		)
		assertEquals(MultilineString(listOf("line one", "line two", "")), result)
	}

	// endregion
	// region Structs

	@Test
	fun `empty struct`() {
		assertAll(
			{ assertEquals(EmptyStruct, parseZon(".{}")) },
			{ assertEquals(EmptyStruct, parseZon(".{ }")) },
		)
	}

	@Test
	fun `array struct with positional elements`() {
		assertAll(
			{
				assertEquals(
					ArrayStruct(listOf(IntVal(BigInteger("10")), IntVal(BigInteger("20")))),
					parseZon(".{ 10, 20 }"),
				)
			},
			{
				val result = parseZon(".{ true, null, .missing }")
				assertEquals(
					ArrayStruct(listOf(TrueVal, NullVal, EnumLiteral(Identifier.Plain("missing")))),
					result,
				)
			},
			{
				assertEquals(
					ArrayStruct(listOf(IntVal(BigInteger("99")))),
					parseZon(".{ 99, }"),
				)
			},
		)
	}

	@Test
	fun `keyed struct`() {
		val result = parseZon(""".{ .enable_logging = true, .retries = 3 }""")
		assertEquals(
			KeyedStruct(
				listOf(
					FieldInit(Identifier.Plain("enable_logging"), TrueVal),
					FieldInit(Identifier.Plain("retries"), IntVal(BigInteger("3"))),
				),
			),
			result,
		)
	}

	@Test
	fun `nested struct composition`() {
		val zon = """
        .{
            .package_name = "network_tools",
            .version = "2.1.0",
            .supported_platforms = .{ .linux, .macos, .windows },
            .dependencies = .{
                .lib_a = .{ .url = "https://server.com/a.tar" },
                .lib_b = .{ .path = "../local_b" }
            }
        }
        """.trimIndent()

		val result = parseZon(zon)
		assertInstanceOf(KeyedStruct::class.java, result)
		val ks = result as KeyedStruct
		assertEquals(4, ks.fields.size)
		assertEquals("package_name", (ks.fields[0].name as Identifier.Plain).name)
	}

	// endregion
	// region Edge cases

	@Test
	fun `comments and whitespace are ignored`() {
		val zon = """
        // Header comment
        .{
            // Comment before key
            .version = "1.0.0", // inline comment
                .deps = .{
                    1, // trailing comma
                }
        }
        """.trimIndent()
		val result = parseZon(zon)
		val expected = KeyedStruct(
			listOf(
				FieldInit(Identifier.Plain("version"), SingleString("1.0.0")),
				FieldInit(Identifier.Plain("deps"), ArrayStruct(listOf(IntVal(BigInteger.ONE)))),
			),
		)
		assertEquals(expected, result)
	}

	@Test
	fun `deep nesting`() {
		val depth = 30
		val zon = (".{").repeat(depth) + "\"deep\"" + ("}").repeat(depth)
		val result = parseZon(zon)
		var node = result
		for (i in 0 until depth) {
			assertInstanceOf(ArrayStruct::class.java, node, "at depth $i")
			assertEquals(1, (node as ArrayStruct).values.size)
			node = (node as ArrayStruct).values[0]
		}
		assertEquals(SingleString("deep"), node)
	}

	// endregion
	// region Error cases

	@Test
	fun `rejects -nan`() {
		assertParseError("-nan")
	}

	@Test
	fun `rejects missing value after equals`() {
		assertParseError(".{ .key = }")
	}

	@Test
	fun `rejects mixing array and keyed struct`() {
		assertParseError(".{ 1, .key = 3 }")
		assertParseError(".{ .key = 3, 1 }")
	}

	@Test
	fun `rejects unclosed string`() {
		assertParseError("\"unclosed string")
	}

	@Test
	fun `rejects invalid hex prefix`() {
		assertParseError("0xg")
	}

	@Test
	fun `rejects incomplete hex escape in char`() {
		assertParseError("'\\x7'")
	}

	@Test
	fun `rejects invalid unicode hex in char`() {
		assertParseError("'\\u{G}'")
	}

	@Test
	fun `rejects bare word not in grammar`() {
		assertParseError("undefined")
	}

	@Test
	fun `rejects struct without leading dot`() {
		assertParseError("{}")
	}

	// endregion
}
