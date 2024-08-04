package net.clgd.ccemux.rendering.ansi;

import net.clgd.ccemux.api.Utils;
import net.clgd.ccemux.api.emulation.EmulatedComputer;
import net.clgd.ccemux.api.emulation.EmulatedPalette;
import net.clgd.ccemux.api.emulation.EmulatedTerminal;
import net.clgd.ccemux.api.rendering.Renderer;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.fusesource.jansi.Ansi.ansi;

public class AnsiRenderer implements Renderer, EmulatedTerminal.Listener, EmulatedPalette.ColorChangeListener {
	private final EmulatedComputer computer;

	private final List<Listener> listeners = new ArrayList<>();

	private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

	private final Writer output;

	public AnsiRenderer(EmulatedComputer computer) {
		this.computer = computer;

		AnsiConsole.systemInstall();
		output = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);

		computer.terminal.addListener(this);
		computer.terminal.getPalette().addListener(this);

		resize(computer.terminal.getWidth(), computer.terminal.getHeight());
	}

	@Override
	public void addListener(@Nonnull Listener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(@Nonnull Listener listener) {
		listeners.remove(listener);
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	@Override
	public void setVisible(boolean visible) {
	}

	@Override
	public void dispose() {
		AnsiConsole.systemUninstall();
	}

	@Override
	public void onAdvance(double dt) {
		while (scanner.hasNext()) {
			for (char ch : scanner.next().toCharArray()) {
				computer.queueEvent("char", new Object[]{String.valueOf(ch)});
			}
		}
	}

	private void write(@Nonnull Ansi ansi) {
		try {
			output.write(ansi.toString());
			output.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void write(@Nonnull String text) {
		final var term = computer.terminal;
		final var palette = term.getPalette();
		final var fg = palette.getColour(term.getTextColour());
		final var bg = palette.getColour(term.getBackgroundColour());

		write(ansi()
			.fgRgb((int) (fg[0] * 255), (int) (fg[1] * 255), (int) (fg[2] * 255))
			.bgRgb((int) (bg[0] * 255), (int) (bg[1] * 255), (int) (bg[2] * 255))
			.a(text.replace('\n', ' ').replace('\r', ' '))
		);
	}

	@Override
	public void setCursorPos(int x, int y) {
		write(ansi().cursor(x, y));
	}

	@Override
	public void clear() {
		write(ansi().eraseScreen());
	}

	@Override
	public void clearLine() {
		write(ansi().eraseLine());
	}

	@Override
	public void scroll(int yDiff) {
		write(yDiff > 0 ? ansi().scrollUp(yDiff) : ansi().scrollDown(yDiff));
	}

	@Override
	public void setColour(int index, double r, double g, double b) {
	}

	@Override
	public void blit(@Nonnull String text, @Nonnull String textColour, @Nonnull String backgroundColour) {
		String fixedText = text.replace('\r', ' ').replace('\n', ' ');
		final var term = computer.terminal;
		final var palette = term.getPalette();

		var ansi = ansi();
		for (var i = 0; i < fixedText.length(); i++) {
			final var bg = palette.getColour(Integer.parseInt(String.valueOf(backgroundColour.charAt(i)), 16));
			final var fg = palette.getColour(Integer.parseInt(String.valueOf(textColour.charAt(i)), 16));
			final var ch = fixedText.charAt(i);

			ansi = ansi
				.fgRgb((int) (fg[0] * 255), (int) (fg[1] * 255), (int) (fg[2] * 255))
				.bgRgb((int) (bg[0] * 255), (int) (bg[1] * 255), (int) (bg[2] * 255))
				.a(ch);
		}

		write(ansi);
	}
}
