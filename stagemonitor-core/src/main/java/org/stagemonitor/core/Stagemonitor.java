package org.stagemonitor.core;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

public final class Stagemonitor {

	public static final String STAGEMONITOR_PASSWORD = "stagemonitor.password";
	private static Logger logger = LoggerFactory.getLogger(Stagemonitor.class);
	private static Configuration configuration;
	private static volatile boolean started;
	private static volatile boolean disabled;
	private static volatile MeasurementSession measurementSession;

	static {
		reset();
	}

	private Stagemonitor() {
	}

	public synchronized static void startMonitoring(MeasurementSession measurementSession) {
		if (!getConfiguration(CorePlugin.class).isStagemonitorActive()) {
			logger.info("stagemonitor is deactivated");
			disabled = true;
		}
		if (started || disabled) {
			return;
		}
		Stagemonitor.measurementSession = measurementSession;
		if (measurementSession.isInitialized() && !started) {
			try {
				start(measurementSession);
			} catch (RuntimeException e) {
				logger.warn("Error while trying to start monitoring. (this exception is ignored)", e);
			}
		} else {
			logger.warn("Measurement Session is not initialized: {}", measurementSession);
			logger.warn("make sure the properties 'stagemonitor.instanceName' and 'stagemonitor.applicationName' " +
					"are set and stagemonitor.properties is available in the classpath");
		}
	}

	private static void start(MeasurementSession measurementSession) {
		initializePlugins();
		logger.info("Measurement Session is initialized: " + measurementSession);
		started = true;
	}

	private static void initializePlugins() {
		final CorePlugin corePlugin = getConfiguration(CorePlugin.class);
		final Collection<String> disabledPlugins = corePlugin.getDisabledPlugins();
		for (StagemonitorPlugin stagemonitorPlugin : ServiceLoader.load(StagemonitorPlugin.class)) {
			final String pluginName = stagemonitorPlugin.getClass().getSimpleName();

			if (disabledPlugins.contains(pluginName)) {
				logger.info("Not initializing disabled plugin {}", pluginName);
			} else {
				logger.info("Initializing plugin {}", pluginName);
				try {
					stagemonitorPlugin.initializePlugin(getMetricRegistry(), getConfiguration());
				} catch (Exception e) {
					logger.warn("Error while initializing plugin " + pluginName +
							" (this exception is ignored)", e);
				}
			}
		}
	}

	/**
	 * Should be called when the server is shutting down.
	 * Calls the {@link StagemonitorPlugin#onShutDown()} method of all plugins
	 */
	public static void shutDown() {
		measurementSession.setEndTimestamp(System.currentTimeMillis());
		for (StagemonitorPlugin stagemonitorPlugin : ServiceLoader.load(StagemonitorPlugin.class)) {
			stagemonitorPlugin.onShutDown();
		}
	}

	public static MetricRegistry getMetricRegistry() {
		return SharedMetricRegistries.getOrCreate("stagemonitor");
	}

	public static Configuration getConfiguration() {
		return configuration;
	}

	public static <T extends StagemonitorPlugin> T getConfiguration(Class<T> plugin) {
		return configuration.getConfig(plugin);
	}

	static void setConfiguration(Configuration configuration) {
		Stagemonitor.configuration = configuration;
	}

	public static MeasurementSession getMeasurementSession() {
		return measurementSession;
	}

	public static boolean isStarted() {
		return started;
	}

	static boolean isDisabled() {
		return disabled;
	}

	static void setLogger(Logger logger) {
		Stagemonitor.logger = logger;
	}

	/**
	 * Should only be used by the internal unit tests
	 */
	public static void reset() {
		started = false;
		disabled = false;
		measurementSession = new MeasurementSession(null, null, null);
		reloadConfiguration();
	}

	private static void reloadConfiguration() {
		List<ConfigurationSource> configurationSources = new ArrayList<ConfigurationSource>();
		for (StagemonitorConfigurationSourceInitializer initializer : ServiceLoader.load(StagemonitorConfigurationSourceInitializer.class)) {
			initializer.modifyConfigurationSources(configurationSources);
		}
		configurationSources.remove(null);

		configuration = new Configuration(StagemonitorPlugin.class, configurationSources, STAGEMONITOR_PASSWORD);

		try {
			for (StagemonitorConfigurationSourceInitializer initializer : ServiceLoader.load(StagemonitorConfigurationSourceInitializer.class)) {
				initializer.onConfigurationInitialized(configuration);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.error("Stagemonitor will be deactivated!");
			disabled = true;
		}
	}
}
