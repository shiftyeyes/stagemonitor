package org.stagemonitor.web.metrics;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.web.WebPlugin;

@WebServlet("/stagemonitor/metrics")
public class StagemonitorMetricsServlet extends HttpServlet {

	private final MetricRegistry registry;
	private final WebPlugin webPlugin;
	private final ObjectMapper mapper;

	public StagemonitorMetricsServlet() {
		this(Stagemonitor.getMetricRegistry(), Stagemonitor.getConfiguration(WebPlugin.class), JsonUtils.getMapper());
		JsonUtils.getMapper().registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false, MetricFilter.ALL));
	}

	public StagemonitorMetricsServlet(MetricRegistry registry, WebPlugin webPlugin, ObjectMapper mapper) {
		this.registry = registry;
		this.webPlugin = webPlugin;
		this.mapper = mapper;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (webPlugin.isWidgetEnabled()) {
			resp.setContentType("application/json");
//			if (allowedOrigin != null) {
			resp.setHeader("Access-Control-Allow-Origin", "*");
//			}
			resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
			resp.setStatus(HttpServletResponse.SC_OK);

			final OutputStream output = resp.getOutputStream();
			try {
//				if (jsonpParamName != null && req.getParameter(jsonpParamName) != null) {
//					getWriter(req).writeValue(output, new JSONPObject(req.getParameter(jsonpParamName), registry));
//				} else {
				getWriter(req).writeValue(output, registry);
//				}
			} finally {
				output.close();
			}
		} else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private ObjectWriter getWriter(HttpServletRequest request) {
		if (Boolean.parseBoolean(request.getParameter("pretty"))) {
			return mapper.writerWithDefaultPrettyPrinter();
		}
		return mapper.writer();
	}
}
