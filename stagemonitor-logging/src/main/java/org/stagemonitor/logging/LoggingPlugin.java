package org.stagemonitor.logging;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class LoggingPlugin extends StagemonitorPlugin {

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		ElasticsearchClient.sendGrafanaDashboardAsync("Logging.json");
	}
}
