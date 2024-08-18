package net.clgd.ccemux.rendering.ansi;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.ansi.UnixTerminal;
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
import java.util.Collections;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

public class AnsiRenderer implements Renderer, EmulatedTerminal.Listener, EmulatedPalette.ColorChangeListener {
	private final EmulatedComputer computer;

	private final List<Listener> listeners = new ArrayList<>();

	private final Terminal terminal;

	private final Writer output;

	public AnsiRenderer(EmulatedComputer computer) {
		this.computer = computer;

		AnsiConsole.systemInstall();
		output = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);

		try {
			terminal = new UnixTerminal(System.in, System.out, StandardCharsets.UTF_8);
			terminal.enterPrivateMode();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

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
		try {
			terminal.exitPrivateMode();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onAdvance(double dt) {
		try {
			final var input = terminal.pollInput();
			if (input != null) {

				if (input.getKeyType() == KeyType.EOF) {
					computer.queueEvent("terminate", new Object[0]);
				} else {
					queueKeyEvents(input);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void queueKeyEvents(KeyStroke input) {
		final var keyCodes = new ArrayList<Integer>();

		if (input.isShiftDown()) {
			keyCodes.add(340); // left shift
		}
		if (input.isCtrlDown()) {
			keyCodes.add(341); // left ctrl
		}
		if (input.isAltDown()) {
			keyCodes.add(342); // left alt
		}

		if (CCKeys.controlKeys.containsKey(input.getKeyType())) {
			keyCodes.add(CCKeys.controlKeys.get(input.getKeyType()));
		} else if (CCKeys.characterKeys.containsKey(input.getCharacter())) {
			keyCodes.add(CCKeys.characterKeys.get(input.getCharacter()));
		}

		for (final var code : keyCodes) {
			computer.queueEvent("key", new Object[]{code});
		}

		if (input.getKeyType() == KeyType.Character) {
			computer.queueEvent("char", new Object[]{input.getCharacter().toString()});
		}

		Collections.reverse(keyCodes);
		for (final var code : keyCodes) {
			computer.queueEvent("key_up", new Object[]{code});
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
		final var fg = palette.getColour(15 - term.getTextColour());
		final var bg = palette.getColour(15 - term.getBackgroundColour());

		try {
			terminal.setBackgroundColor(new TextColor.RGB((int) (bg[0] * 255), (int) (bg[1] * 255), (int) (bg[2] * 255)));
			terminal.setForegroundColor(new TextColor.RGB((int) (fg[0] * 255), (int) (fg[1] * 255), (int) (fg[2] * 255)));
			terminal.putString(text);
			terminal.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setCursorPos(int x, int y) {
		try {
			terminal.setCursorPosition(x, y);
			terminal.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void clear() {
		try {
			terminal.clearScreen();
			terminal.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void clearLine() {
		// FIXME
		write(ansi().eraseLine());
	}

	@Override
	public void scroll(int yDiff) {
		// FIXME
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

		try {
			for (var i = 0; i < fixedText.length(); i++) {
				final var bg = palette.getColour(15 - Integer.parseInt(String.valueOf(backgroundColour.charAt(i)), 16));
				final var fg = palette.getColour(15 - Integer.parseInt(String.valueOf(textColour.charAt(i)), 16));
				final var ch = fixedText.charAt(i);

				terminal.setBackgroundColor(new TextColor.RGB((int) (bg[0] * 255), (int) (bg[1] * 255), (int) (bg[2] * 255)));
				terminal.setForegroundColor(new TextColor.RGB((int) (fg[0] * 255), (int) (fg[1] * 255), (int) (fg[2] * 255)));
				terminal.putCharacter(ch);
			}

			terminal.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
