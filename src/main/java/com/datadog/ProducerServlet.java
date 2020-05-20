package com.datadog;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger("com.datadog.demo");
	private KafkaProducer<String, String> producer;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		producer = new KafkaProducer<String, String>(Main.producerProperties);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Main.setStartTime();
		String topic = req.getPathInfo().split("/")[1];
		logger.info("Sending message to topic [" + topic + "]");
		String payload = req.getParameter("payload");
		ProducerRecord<String, String> outRecord = new ProducerRecord<String, String>(topic, payload);
		logger.info("Sending message with content [" + payload + "]");
		Main.setPreviousTime();
		Future<RecordMetadata> future = producer.send(outRecord);

		resp.setContentType("text/plain;charset=UTF-8");
		RecordMetadata record;
		try {
			record = future.get();
			resp.getWriter().println("Record published to topic " + record.topic());
			if (record.hasOffset()) {
				resp.getWriter().println("Record offset is " + record.offset());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		producer.close();
	}

}
