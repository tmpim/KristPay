package pw.lemmmy.kristpay.prometheus;

import com.google.common.base.Preconditions;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.prometheus.reporters.Reporter;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PrometheusController extends AbstractHandler {
	private final CollectorRegistry registry = new CollectorRegistry();
	private final List<Reporter> reporters = new ArrayList<>();
	
	public void addReporter(@Nonnull Reporter reporter) {
		Preconditions.checkNotNull(reporter, "reporter cannot be null");
		reporters.add(reporter);
		reporter.register(registry);
	}
	
	@Override
	public void handle(String target, Request baseRequest, 
					   HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!target.equals("/metrics")) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		for (Reporter reporter : reporters) reporter.fetch();
		
		try {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(io.prometheus.client.exporter.common.TextFormat.CONTENT_TYPE_004);
			
			TextFormat.write004(response.getWriter(), registry.metricFamilySamples());
			
			baseRequest.setHandled(true);
		} catch (IOException e) {
			KristPay.INSTANCE.getLogger().error("Failed to read PrometheusController statistics", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
