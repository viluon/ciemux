package net.clgd.ccemux.plugins.builtin;

import com.google.auto.service.AutoService;
import net.clgd.ccemux.api.plugins.Plugin;
import net.clgd.ccemux.api.plugins.PluginManager;
import net.clgd.ccemux.rendering.ansi.AnsiRenderer;
import net.clgd.ccemux.rendering.tror.TRoRRenderer;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@AutoService(Plugin.class)
public class AnsiPlugin extends Plugin {
	@Nonnull
	@Override
	public String getName() {
		return "ANSI Renderer";
	}

	@Nonnull
	@Override
	public String getDescription() {
		return "A CPU-based renderer which reads from stdin and writes to stdout using ANSI escape sequences.";
	}

	@Nonnull
	@Override
	public Optional<String> getVersion() {
		return Optional.empty();
	}

	@Nonnull
	@Override
	public Collection<String> getAuthors() {
		return Collections.singleton("CLGD");
	}

	@Nonnull
	@Override
	public Optional<String> getWebsite() {
		return Optional.empty();
	}

	@Override
	public void setup(@Nonnull PluginManager manager) {
		manager.addRenderer("ANSI", (comp, cfg) -> {
			final var renderer = new AnsiRenderer(comp);
			renderer.clear();
			return renderer;
		});
	}
}
