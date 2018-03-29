package net.clgd.ccemux.emulation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.filesystem.ComboMount;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.core.filesystem.JarMount;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import net.clgd.ccemux.api.emulation.EmuConfig;
import net.clgd.ccemux.api.emulation.EmulatedComputer;
import net.clgd.ccemux.api.emulation.EmulatedTerminal;
import net.clgd.ccemux.api.emulation.Emulator;
import net.clgd.ccemux.api.emulation.filesystem.VirtualDirectory;
import net.clgd.ccemux.api.emulation.filesystem.VirtualMount;
import net.clgd.ccemux.api.rendering.Renderer;
import net.clgd.ccemux.api.rendering.RendererFactory;
import net.clgd.ccemux.plugins.PluginManager;

@Slf4j
@RequiredArgsConstructor
public class CCEmuX implements Runnable, Emulator, IComputerEnvironment {
	public static String getVersion() {
		try (val s = CCEmuX.class.getResourceAsStream("/ccemux.version")) {
			val props = new Properties();
			props.load(s);
			return props.getProperty("version");
		} catch (IOException e) {
			return null;
		}
	}

	private final EmuConfig cfg;

	@Getter
	private final RendererFactory<?> rendererFactory;

	@Getter
	private final PluginManager pluginMgr;

	private final File ccSource;

	private final Map<EmulatedComputer, Renderer> computers = new ConcurrentHashMap<>();

	private int nextID = 0;

	private long started = -1;
	private boolean running;

	@Override
	public EmuConfig getConfig() {
		return cfg;
	}

	@Override
	public File getCCJar() {
		return ccSource;
	}

	@Override
	public String getEmulatorVersion() {
		return getVersion();
	}

	/**
	 * Creates a new computer and renderer, applying config settings and plugin
	 * hooks appropriately.
	 *
	 * @see #createComputer(Consumer)
	 *
	 * @return The new computer
	 */
	public EmulatedComputer createComputer() {
		return createComputer(b -> {});
	}

	/**
	 * Creates a new computer and renderer, applying config settings and plugin
	 * hooks appropriately. Additionally takes a {@link Consumer} which will be
	 * called on the {@link EmulatedComputer.BuilderImpl} after plugin hooks, which
	 * can be used to change the computers ID or other properties.
	 *
	 * @param builderMutator
	 *            Will be called after plugin hooks with the builder
	 * @return The new computer
	 */
	public EmulatedComputer createComputer(Consumer<EmulatedComputer.Builder> builderMutator) {
		val term = new EmulatedTerminal(cfg.termWidth.get(), cfg.termHeight.get());
		val builder = EmulatedComputerImpl.builder(this, term).id(-1);

		pluginMgr.onCreatingComputer(this, builder);
		builderMutator.accept(builder);

		val computer = builder.build();

		pluginMgr.onComputerCreated(this, computer);

		addComputer(computer);

		return computer;
	}

	private void addComputer(EmulatedComputer ec) {
		Renderer r = rendererFactory.create(ec, cfg);

		ec.addListener(r);
		r.addListener(() -> this.removeComputer(ec)); // onClose

		pluginMgr.onRendererCreated(this, r);

		computers.put(ec, r);

		r.setVisible(true);

		log.info("Created new computer ID {}", ec.getID());
		ec.turnOn();
	}

	public boolean removeComputer(EmulatedComputer computer) {
		synchronized (computers) {
			try {
				log.info("Removing computer ID {}", computer.getID());

				Renderer renderer = computers.remove(computer);
				if (renderer != null) {
					renderer.dispose();
					pluginMgr.onComputerRemoved(this, computer);
					return true;
				} else {
					return false;
				}
			} finally {
				if (computers.size() < 1 && running) {
					log.info("All computers removed, stopping emulation");
					running = false;
				}
			}
		}
	}

	private void advance(double dt) {
		synchronized (computers) {
			computers.keySet().forEach(c -> {
				synchronized (c) {
					c.advance(dt);
				}
			});
		}

		pluginMgr.onTick(this, dt);
	}

	@Override
	public void run() {
		running = true;
		started = System.currentTimeMillis();

		long lastTime = started;
		double computerTickTimer = 0d;

		while (running) {
			long now = System.currentTimeMillis();
			double dt = (now - lastTime) / 1000d;

			computerTickTimer += dt;

			if (computerTickTimer >= 0.05d) {
				advance(dt);
				computerTickTimer = 0d;
			}

			lastTime = now;

			try {
				Thread.sleep(Math.max(0, 50 - (System.currentTimeMillis() - now)));
			} catch (InterruptedException e) {}
		}

		log.info("Emulation stopped");
		started = -1;
	}

	public boolean isRunning() {
		return running;
	}

	public void stop() {
		running = false;
	}

	public long getTicksSinceStart() {
		return (System.currentTimeMillis() - started) / 50;
	}

	@Override
	public int assignNewID() {
		return nextID++;
	}

	@Override
	public IMount createResourceMount(String domain, String subPath) {
		String path = Paths.get("assets", domain, subPath).toString().replace('\\', '/');
		if (path.startsWith("\\")) {
			path = path.substring(1);
		}

		try {
			VirtualDirectory.Builder romBuilder = new VirtualDirectory.Builder();
			pluginMgr.onCreatingROM(this, romBuilder);

			return new ComboMount(new IMount[] { new JarMount(ccSource, path), new VirtualMount(romBuilder.build()) });
		} catch (IOException e) {
			log.error("Failed to create resource mount", e);
			return null;
		}
	}

	@Override
	public InputStream createResourceFile(String domain, String path) {
		return CCEmuX.class.getResourceAsStream("/assets/" + domain + "/" + path);
	}

	@Override
	public IWritableMount createSaveDirMount(String path, long capacity) {
		return new FileMount(cfg.getDataDir().resolve("computer").resolve(path).toFile(), getComputerSpaceLimit());
	}

	@Override
	public long getComputerSpaceLimit() {
		return cfg.maxComputerCapacity.get();
	}

	@Override
	public int getDay() {
		return (int) (((getTicksSinceStart() + 6000) / 24000) + 1);
	}

	@Override
	public String getHostString() {
		if (getVersion() != null) {
			return String.format("ComputerCraft %s (CCEmuX %s)", ComputerCraft.getVersion(), getVersion());
		} else {
			return String.format("ComputerCraft %s (CCEmuX)", ComputerCraft.getVersion());
		}
	}

	@Override
	public double getTimeOfDay() {
		return ((getTicksSinceStart() + 6000) % 24000) / 1000d;
	}

	@Override
	public boolean isColour() {
		return true;
	}
}
