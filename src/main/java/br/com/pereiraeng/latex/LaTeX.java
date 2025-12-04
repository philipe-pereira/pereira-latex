package br.com.pereiraeng.latex;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import br.com.pereiraeng.io.IOutils;
import br.com.pereiraeng.io.flow.Flow;
import br.com.pereiraeng.unicode.Greek;
import br.com.pereiraeng.unicode.PTC;
import br.com.pereiraeng.core.ColorUtils;
import br.com.pereiraeng.core.Direction;

public class LaTeX {

	public static final String LATEX_COMM = "\\\\\\p{Alnum}+";

	/**
	 * Função que retorna o nome do comando de seccionamento em função da
	 * profundidade do texto
	 * 
	 * @param level profundidade da seção
	 * @param c
	 *              <ol start="0">
	 *              <li>2 para começar o seccionamento em "Parte";</i>
	 *              <li>1 para começar o seccionamento em "Capítulo";</i>
	 *              <li>0 senão.</i>
	 *              </ol>
	 * @return comando de seccionamento
	 */
	public static String getHeaderName(int level, int c) {
		level = (level == 0 ? level : level + 2 - c);
		switch (level) {
		case 0:
			return "title";
		case 1:
			return "part";
		case 2:
			return "chapter";
		case 3:
			return "section";
		case 4:
			return "subsection";
		case 5:
			return "subsubsection";
		case 6:
			return "paragraph";
		case 7:
			return "subparagraph";
		default:
			return null;
		}
	}

	private static final String BLOCKS = "\\\\begin\\{%1$s\\}.+?\\\\end\\{%1$s\\}";

	public static final Pattern P_TABLE = Pattern.compile(String.format(BLOCKS, "tabular"), Pattern.DOTALL);

	// ------------------------ LATEX GROUPS ------------------------

	private static final String INNER_CONTENT = "\\{(?!.*\\{).+?(?<!.*\\})\\}";

	public static final Pattern P_GROUP = Pattern.compile("(" + LATEX_COMM
			+ "(\\{\\p{Alnum}+\\}|\\[\\p{Alnum}+\\]\\{[\\p{Alnum},\\.\\s]+\\}|)|[\\_\\^])" + INNER_CONTENT,
			Pattern.DOTALL);

	public static final Pattern P_CONTENT = Pattern.compile("\\{.+?\\}", Pattern.DOTALL);

	// -------------------- LATEX CARACTERES ESPECIAIS --------------------

	public static final Pattern P_SPE_CHAR = Pattern.compile(LATEX_COMM + " ");

	/**
	 * http://milde.users.sourceforge.net/LUCR/Math/
	 */
	private static final String UNICODE_TABLE = "/unimathsymbols.txt";

	private static final int UNICODE_TABLE_OFFSET = 4101;

	/**
	 * Função que converte um número de 0 a 65535 em seu código em LaTeX equivalente
	 * da tabela do Unicode
	 * 
	 * @param c número de 0 a 65535
	 * @return código em LaTeX
	 */
	public static String getLaTeX(int c) {
		try {
			String out = null;

			InputStream stream = PTC.class.getResourceAsStream(UNICODE_TABLE);

			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			reader.skip(UNICODE_TABLE_OFFSET);
			String str = null;
			while ((str = reader.readLine()) != null) {
				String[] row = str.split("\\^");
				if (Integer.parseInt(row[0], 16) == c) {
					if (!row[2].isEmpty())
						out = row[2];
					break;
				}
			}
			reader.close();

			if (out != null)
				return out;

		} catch (IOException e) {
			e.printStackTrace();
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
		}

		// caracteres HTML-UNICODE que não possuem equivalente em LaTeX
		switch (c) {
		case 402: // fnof
			return "f";
		case 185:
			return "^{1}";
		case 178:
			return "^{2}";
		case 8364:
			return "\\euro";
		case 917:
			return "\\Epsilon";
		default:
			return null;
		}
	}

	public static char getChar(String name) {
		try {
			BufferedReader br = IOutils.getBr(new File(LaTeX.class.getResource(UNICODE_TABLE).toURI()));
			br.skip(4101);
			String s = null;
			while ((s = br.readLine()) != null) {
				String[] row = s.split("\\^");
				if (name.equals(row[2])) {
					br.close();
					return (char) Integer.parseInt(row[0], 16);
				}
			}
			br.close();
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}

		switch (name) {
		case "\\euro":
			return '\u20AC';
		case "\\Epsilon":
			return '\u0395';
		default:
			return ' ';
		}
	}

	// -------------------- LATEX ACENTOS --------------------

	private static final Pattern P_DIAC = Pattern.compile("\\\\(['`\\^~\"][AEIOUaeiou]|c\\{c\\})");

	/**
	 * Função que converte códigos LaTeX em seus respectivos caracteres com acentos
	 * e outros sinais distintivos
	 * 
	 * @param text texto em LaTeX
	 * @return texto normal
	 */
	public static String fromLatek(String text) {
		Matcher m = P_DIAC.matcher(text);
		StringBuffer sb = new StringBuffer(text.length());
		while (m.find()) {
			String g = m.group();
			char c2 = g.charAt(1), c1 = ' ';
			if (c2 == 'c') {
				c1 = 'c';
				c2 = '\u0327';
			} else {
				c1 = g.charAt(2);
				c2 = getAccent(c2);
			}
			m.appendReplacement(sb,
					Matcher.quoteReplacement(Normalizer.normalize(String.format("%c%c", c1, c2), Form.NFC)));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static char getAccent(char accent) {
		switch (accent) {
		case '\'':
			return '\u0301';
		case '`':
			return '\u0300';
		case '^':
			return '\u0302';
		case '~':
			return '\u0303';
		case '\"':
			return '\u0308';
		default:
			return ' ';
		}
	}

	/**
	 * Função que substitui as letras acentuadas de um texto pelos correspondentes
	 * códigos LaTeX
	 * 
	 * @param text texto normal
	 * @return texto em LaTeX
	 */
	public static String toLatex(String text) {
		String out = "";
		String[] ss = text.split("");
		for (String s : ss) {
			s = Normalizer.normalize(s, Normalizer.Form.NFD);
			if (s.length() == 2) {
				char c = s.charAt(0);
				out += "\\" + getAccentLatek(s.charAt(1)) + (c == 'c' ? "{c}" : c);
			} else
				out += s;
		}
		return out;
	}

	public static String greek2latex(String in) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			if (Greek.isGreek(c))
				out.append("\\" + Greek.greek(c) + " ");
			else
				out.append(c);
		}
		return out.toString();
	}

	/**
	 * Função que retorna o nome do acento para a correção do código HTML
	 * 
	 * @param accent caracter relativo ao acento
	 * @return <code>String</code> com o nome do acento
	 */
	private static String getAccentLatek(char accent) {
		if (accent == '\u0301') {
			return "'";
		} else if (accent == '\u0303') {
			return "~";
		} else if (accent == '\u0302') {
			return "^";
		} else if (accent == '\u0327') {
			return "c";
		} else if (accent == '\u0300') {
			return "`";
		} else if (accent == '\u0308') {
			return "\"";
		} else {
			System.err.println("Acento não reconhecido: " + accent + " " + String.format(" \\u%04x", accent + 0));
			return "";
		}
	}

	// ------------- LATEX TABLE -------------

	public static Object[][] tableFromLatek(String text) {
		text = LaTeX.fromLatek(text);
		text = text.replace("\\hline", "").replace('\n', ' ');
		String[] rows = text.split("\\\\\\\\");
		Object[][] out = new Object[rows.length - 1][];

		for (int i = 0; i < out.length; i++) {
			String[] cols = rows[i].split("&");
			out[i] = new Object[cols.length];
			for (int j = 0; j < cols.length; j++)
				out[i][j] = cols[j].trim();
		}

		return out;
	}

	public static String toLatex(Vector<?> table) {
		String out = "";
		for (Object o : table) {
			Vector<?> row = (Vector<?>) o;
			for (Object col : row) {
				String s = col.toString();
				s = LaTeX.toLatex(s);
				out += s + "\t & ";
			}
			out = out.substring(0, out.length() - 3);
			out += "\\\\ \\hline \n";
		}
		return out;
	}

	// ==================================================

	private static final String EXE = "pdflatex";

	public static void runTex(String texExeDir, String texFile, Flow<String> flow) {
		runTex(texExeDir, texFile, texFile.substring(0, texFile.lastIndexOf(File.separatorChar)), flow);
	}

	public static void runTex(String texExeDir, String texFile, String destFolder, Flow<String> flow) {
		String exe = "\"" + texExeDir + "\\" + EXE + "\" "
				+ (destFolder != null ? "\"-output-directory=" + destFolder + "\"" : "") + " \"" + texFile + "\"";
		System.out.println("Executando: " + exe);

		ProcessBuilder builder = new ProcessBuilder(exe);
		Process p = null;

		try {
			p = builder.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

		String s = null;
		try {
			while ((s = br.readLine()) != null) {
				// redireciona o fluxo de informação
				if (flow != null)
					flow.incomingData(s);
				else
					System.out.println(s);

				if (s.startsWith("! LaTeX Error:")) {
					// responde às notificações de erros
					for (int i = 0; i < 7; i++)
						System.out.println(br.readLine());
					bw.write("\n");
					bw.flush();
				} else if (s.startsWith("Transcript written on ")) {
					// mensagem final
					br.readLine();
				}
			}
			br.close();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// encerrar execução
		p.destroy();
		System.out.println("Execução encerrada.");
	}

	/**
	 * Função que analisa o caminho de um diretório e determina se é nele onde está
	 * instalado o compilador LaTeX
	 * 
	 * @param dir caminho do diretório
	 * @return 1 se for de fato o diretório, 0 se for um diretório válido do disco,
	 *         porém o ATP não está nele, -1 se nem diretório for o caminho dado
	 */
	public static int hasTex(String dir) {
		if (dir == null)
			return -1;
		if (dir.equals(""))
			return -1;

		File f = new File(dir);
		if (f.isDirectory()) {
			int exe = Arrays.binarySearch(f.list(), EXE + ".exe");
			if (exe >= 0)
				return 1;
			else
				return 0;
		} else
			return -1;
	}

	// ============================== COLOR ==============================

	/**
	 * Função que converte uma cor numa sequência de caracteres que a identifica na
	 * linguagem de marcação TikZ. É a função inversa de {@link #tex2color(String)}.
	 * 
	 * @param c cor
	 * @return sequência de caracteres que designa a cor (seja o nome dela, seja na
	 *         forma rgb)
	 */
	public static String color2tex(Color c) {
		return rgb2tex(ColorUtils.color2rgb(c));
	}

	/**
	 * Função que converte um número inteiro que representa uma cor numa sequência
	 * de caracteres que a identifica na linguagem de marcação TikZ. É a função
	 * inversa de {@link #tex2rgb(String)}.
	 * 
	 * @param rgb inteiro cujo valor varia entre 0 (preto) e 16777215 (branco,
	 *            0xFFFFFF)
	 * @return nome da cor, tal como aceita na linguagem de marcação TikZ
	 */
	private static String rgb2tex(int rgb) {
		switch (rgb) {
		case 0xFF0000:
			return "red";
		case 0x00FF00:
			return "green";
		case 0x008000:
			return "greenHTML";
		case 0xBE80C0:
			return "lime";
		case 0x808000:
			return "olive";
		case 0x0000FF:
			return "blue";
		case 0x00AE41:
			return "cyan";
		case 0x00FFFF:
			return "cyanJava";
		case 0xEB2977:
			return "magenta";
		case 0xFF00FF:
			return "magentaJava";
		case 0xFFFF00:
			return "yellow";
		case 0x000000:
			return "black";
		case 0x808080:
			return "gray";
		case 0xA9A9A9:
			return "darkgray";
		case 0x404040:
			return "darkgrayJava";
		case 0xD3D3D3:
			return "lightgray";
		case 0xC0C0C0:
			return "lightgrayJava";
		case 0xA52A2A:
			return "brown";
		case 0xFFA500:
			return "orange";
		case 0xFFC800:
			return "orangeJava";
		case 0xFDC1FF:
			return "pink";
		case 0xFFC0CB:
			return "pinkHTML";
		case 0xFFAFAF:
			return "pinkJava";
		case 0xBD82FF:
			return "purple";
		case 0x800080:
			return "purpleHTML";
		case 0x008080:
			return "teal";
		case 0xEE82EE:
			return "violet";
		case 0xFFFFFF:
			return "white";
		default:
			return null;
		}
	}

	/**
	 * Função que converte uma sequência de caracteres que a identifica na linguagem
	 * de marcação TikZ na cor correspondente. É a função inversa de
	 * {@link #color2tex(String)}.
	 * 
	 * @param tikz sequência de caracteres que designa a cor
	 * @return cor correspondente
	 */
	public static Color tex2color(String tikz) {
		return ColorUtils.rgb2color(tex2rgb(tikz));
	}

	/**
	 * Função que converte uma sequência de caracteres que a identifica na linguagem
	 * de marcação TikZ num número inteiro que representa a cor. É a função inversa
	 * de {@link #rgb2tex(int)}.
	 * 
	 * @param tikz nome da cor, tal como aceita na linguagem de marcação TikZ
	 * @return inteiro correspondente entre 0 (preto) e 16777215 (branco, 0xFFFFFF)
	 */
	private static int tex2rgb(String tikz) {
		switch (tikz) {
		case "red":
			return 0xFF0000;
		case "green":
			return 0x00FF00;
		case "greenHTML":
			return 0x008000;
		case "lime":
			return 0xBE80C0;
		case "olive":
			return 0x808000;
		case "blue":
			return 0x0000FF;
		case "cyan":
			return 0x00AE41;
		case "cyanJava":
			return 0x00FFFF;
		case "magenta":
			return 0xEB2977;
		case "magentaJava":
			return 0xFF00FF;
		case "yellow":
			return 0xFFFF00;
		case "black":
			return 0x000000;
		case "gray":
			return 0x808080;
		case "darkgray":
			return 0xA9A9A9;
		case "darkgrayJava":
			return 0x404040;
		case "lightgray":
			return 0xD3D3D3;
		case "lightgrayJava":
			return 0xC0C0C0;
		case "brown":
			return 0xA52A2A;
		case "orange":
			return 0xFFA500;
		case "orangeJava":
			return 0xFFC800;
		case "pink":
			return 0xFDC1FF;
		case "pinkHTML":
			return 0xFFC0CB;
		case "pinkJava":
			return 0xFFAFAF;
		case "purple":
			return 0xBD82FF;
		case "purpleHTML":
			return 0x800080;
		case "teal":
			return 0x008080;
		case "violet":
			return 0xEE82EE;
		case "white":
			return 0xFFFFFF;
		default:
			return -1;
		}
	}

	// ============================== MATHBB ==============================

	public static int blackBoardBold2unicode(char r) {
		switch (r) {
		case 'C':
			return 0x2102;
		case 'N':
			return 0x2115;
		case 'Z':
			return 0x2124;
		case 'Q':
			return 0x211A;
		case 'P':
			return 0x2119;
		case 'H':
			return 0x210D;
		case 'R':
			return 0x211D;
		default:
			boolean lower = r > 96;
			int n = r - (lower ? 97 : 65);
			return 0x1D538 + n + (lower ? 26 : 0);
		}
	}

	public static int script2unicode(char r) {
		switch (r) {
		case 'g':
			return 0x210A;
		case 'H':
			return 0x210B;
		case 'I':
			return 0x2110;
		case 'L':
			return 0x2112;
		case 'R':
			return 0x211B;
		case 'B':
			return 0x212C;
		case 'e':
			return 0x212F;
		case 'E':
			return 0x2130;
		case 'F':
			return 0x2131;
		case 'M':
			return 0x2133;
		case 'o':
			return 0x2134;
		default:
			boolean lower = r > 96;
			int n = r - (lower ? 97 : 65);
			return 0x1D49C + n + (lower ? 26 : 0);
		}
	}

	// --------------- DRAWING ---------------

	public static final float FONT_SIZE = 30f;

	public static void drawLatek(Graphics g, int x, int y, String math) {
		LaTeX.drawLatek(g, x, y, math, FONT_SIZE, Color.BLACK, Direction.UP_LEFT);
	}

	public static void drawLatek(Graphics g, int x, int y, String math, Color color) {
		LaTeX.drawLatek(g, x, y, math, FONT_SIZE, color, Direction.UP_LEFT);
	}

	public static void drawLatek(Graphics g, int x, int y, String math, Direction anchor) {
		LaTeX.drawLatek(g, x, y, math, FONT_SIZE, Color.BLACK, anchor);
	}

	private static void drawLatek(Graphics g, int x, int y, String math, float size, Color color, Direction anchor) {
		BufferedImage bi = LaTeX.getLatekImage(math, size, color);
		if (bi == null)
			return;
		switch (anchor) {
		case UP_LEFT:
			break;
		case UP:
			x -= bi.getWidth() / 2;
			break;
		case UP_RIGHT:
			x -= bi.getWidth();
			break;
		case DOWN_LEFT:
			y -= bi.getHeight();
			break;
		case DOWN_RIGHT:
			x -= bi.getWidth();
			y -= bi.getHeight();
			break;
		case RIGHT:
			x -= bi.getWidth();
			y -= bi.getHeight() / 2;
			break;
		case DOWN:
			x -= bi.getWidth() / 2;
			y -= bi.getHeight();
			break;
		case LEFT:
			y -= bi.getHeight() / 2;
			break;
		case CENTER:
			x -= bi.getWidth() / 2;
			y -= bi.getHeight() / 2;
			break;
		}
		g.drawImage(bi, x, y, null);
	}

	private static BufferedImage getLatekImage(String math, float size, Color color) {
		BufferedImage b = null;
		try {
			TeXIcon ti = (new TeXFormula(math)).createTeXIcon(TeXConstants.STYLE_DISPLAY, size, 0, color);
			b = new BufferedImage(ti.getIconWidth(), ti.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
			ti.paintIcon(new JLabel(), b.getGraphics(), 0, 0);
		} catch (org.scilab.forge.jlatexmath.ParseException | IllegalArgumentException e) {
		}
		return b;
	}

}
