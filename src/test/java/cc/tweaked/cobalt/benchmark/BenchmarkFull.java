package cc.tweaked.cobalt.benchmark;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Runs all benchmarks within the {@code cc.tweaked.cobalt.benchmark} package.
 */
public final class BenchmarkFull {
	private BenchmarkFull() {
	}

	public static void main(String... args) throws RunnerException {
		Options opts = new OptionsBuilder()
			.include("cc.tweaked.cobalt.benchmark.*")
			.warmupIterations(3)
			.measurementIterations(5)
			.measurementTime(TimeValue.milliseconds(3000))
			.jvmArgsPrepend("-server")
			.resultFormat(ResultFormatType.JSON)
			.forks(3)
			.build();
		new Runner(opts).run();
	}
}
