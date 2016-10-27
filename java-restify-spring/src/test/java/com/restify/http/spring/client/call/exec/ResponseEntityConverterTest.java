package com.restify.http.spring.client.call.exec;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.restify.http.client.Headers;
import com.restify.http.client.response.EndpointResponse;
import com.restify.http.client.response.EndpointResponseCode;

public class ResponseEntityConverterTest {

	private ResponseEntityConverter<String> converter = new ResponseEntityConverter<String>();

	private EndpointResponse<String> endpointResponse;

	@Before
	public void setup() {
		Headers headers = new Headers();
		headers.put("Content-Type", MediaType.TEXT_PLAIN_VALUE);
		headers.put("X-Header-Whatever", "whatever");

		endpointResponse = new EndpointResponse<>(EndpointResponseCode.ok(), headers, "expected result");
	}

	@Test
	public void shouldConvertRestifyEndpointResponseToResponseEntity() {
		ResponseEntity<String> responseEntity = converter.convert(endpointResponse);

		assertEquals(MediaType.TEXT_PLAIN, responseEntity.getHeaders().getContentType());
		assertEquals(Arrays.asList("whatever"), responseEntity.getHeaders().get("X-Header-Whatever"));

		assertEquals("expected result", responseEntity.getBody());

		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}
}
