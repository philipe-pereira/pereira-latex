package br.com.pereiraeng.latex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LaTeXtest {

	@Test
	void testGetLaTeXcode() {
		String code = LaTeX.getLaTeX(0x3C3);
		assertEquals("\\sigma", code);

		code = LaTeX.getLaTeX(0x2207);
		assertEquals("\\nabla", code);
	}

}
